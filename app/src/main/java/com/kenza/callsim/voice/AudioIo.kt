package com.kenza.callsim.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import kotlin.concurrent.thread

/** Shared PCM format used by both supported live providers. */
object AudioConfig {
    const val SAMPLE_RATE = 16_000
    const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    const val BYTES_PER_SAMPLE = 2
    const val INPUT_CHUNK_BYTES =
        SAMPLE_RATE * GeminiLiveTuning.INPUT_CHUNK_MS / 1_000 * BYTES_PER_SAMPLE
}

/**
 * Captures 16 kHz mono PCM and forwards small chunks immediately. Speaker and
 * earpiece routes remain echo-safe half duplex, while headset microphone routes
 * stay full duplex so the user can genuinely interrupt Gemini mid-sentence.
 */
class MicRecorder(
    private val onChunk: (ByteArray) -> Unit,
    private val onError: (String) -> Unit = {},
    private val onStreaming: () -> Unit = {},
) {

    @Volatile var muted: Boolean = false
    @Volatile var agentSpeaking: Boolean = false

    private var record: AudioRecord? = null
    @Volatile private var running = false
    private var worker: Thread? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null
    private var lastRouteType: Int? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        val minBuffer = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNEL_IN,
            AudioConfig.ENCODING,
        )
        if (minBuffer <= 0) {
            onError("This device can't record 16 kHz mono audio.")
            return
        }

        // The AudioRecord allocation may be larger, but each network write stays
        // exactly 40 ms so Gemini receives a smooth realtime stream.
        val chunkBytes = AudioConfig.INPUT_CHUNK_BYTES
        val bufferSize = maxOf(minBuffer, chunkBytes * 2)

        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
        )
        for (source in sources) {
            val candidate = runCatching {
                AudioRecord(
                    source,
                    AudioConfig.SAMPLE_RATE,
                    AudioConfig.CHANNEL_IN,
                    AudioConfig.ENCODING,
                    bufferSize,
                )
            }.getOrNull()
            if (candidate != null && candidate.state == AudioRecord.STATE_INITIALIZED) {
                record = candidate
                Log.i(TAG, "AudioRecord initialized with source=$source chunkMs=${GeminiLiveTuning.INPUT_CHUNK_MS}")
                break
            }
            runCatching { candidate?.release() }
        }
        if (record == null) {
            onError("Microphone is unavailable (couldn't open an audio input).")
            return
        }

        record?.audioSessionId?.let { session ->
            if (AcousticEchoCanceler.isAvailable()) {
                aec = runCatching {
                    AcousticEchoCanceler.create(session)?.apply { enabled = true }
                }.getOrNull()
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = runCatching {
                    NoiseSuppressor.create(session)?.apply { enabled = true }
                }.getOrNull()
            }
            if (AutomaticGainControl.isAvailable()) {
                agc = runCatching {
                    AutomaticGainControl.create(session)?.apply { enabled = true }
                }.getOrNull()
            }
            Log.i(TAG, "effects aec=${aec != null} ns=${ns != null} agc=${agc != null}")
        }

        running = true
        runCatching { record?.startRecording() }
            .onFailure {
                onError("Couldn't start the microphone: ${it.message}")
                running = false
                return
            }

        var announced = false
        worker = thread(name = "mic-recorder") {
            val buffer = ByteArray(chunkBytes)
            while (running) {
                val read = record?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    if (!announced) {
                        announced = true
                        onStreaming()
                    }
                    val headsetFullDuplex = routedInputSupportsBargeIn()
                    if (!muted && (!agentSpeaking || headsetFullDuplex)) {
                        onChunk(buffer.copyOf(read))
                    }
                } else if (read < 0) {
                    onError("Microphone read error ($read).")
                    break
                }
            }
        }
    }

    /**
     * A dedicated headset mic is physically separated from the phone speaker, so
     * streaming it while Gemini speaks is safe and enables true barge-in. Built-in
     * mic routes remain gated to prevent the agent interrupting itself.
     */
    private fun routedInputSupportsBargeIn(): Boolean {
        val type = record?.routedDevice?.type ?: return false
        if (lastRouteType != type) {
            lastRouteType = type
            Log.i(TAG, "input route type=$type fullDuplex=${isHeadsetInput(type)}")
        }
        return isHeadsetInput(type)
    }

    private fun isHeadsetInput(type: Int): Boolean = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET -> true
        else -> Build.VERSION.SDK_INT >= 31 && type == AudioDeviceInfo.TYPE_BLE_HEADSET
    }

    fun stop() {
        running = false
        worker?.join(500)
        worker = null
        runCatching { aec?.release() }
        aec = null
        runCatching { ns?.release() }
        ns = null
        runCatching { agc?.release() }
        agc = null
        runCatching { record?.stop() }
        record?.release()
        record = null
    }
}

/** Streams agent PCM to the phone audio route with a deliberately short queue. */
class PcmPlayer(private val sampleRate: Int = AudioConfig.SAMPLE_RATE) {

    private var track: AudioTrack? = null

    fun start() {
        if (track != null) return
        val minimumBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioConfig.CHANNEL_OUT,
            AudioConfig.ENCODING,
        )
        val targetBuffer =
            sampleRate * GeminiLiveTuning.OUTPUT_BUFFER_MS / 1_000 * AudioConfig.BYTES_PER_SAMPLE
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioConfig.CHANNEL_OUT)
                    .setEncoding(AudioConfig.ENCODING)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minimumBuffer, targetBuffer))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track?.play()
    }

    fun write(pcm: ByteArray) {
        track?.write(pcm, 0, pcm.size)
    }

    /** Immediately discard unsounded speech when Gemini reports interruption. */
    fun flush() {
        runCatching {
            track?.pause()
            track?.flush()
            track?.play()
        }
    }

    fun stop() {
        runCatching {
            track?.pause()
            track?.flush()
            track?.stop()
        }
        track?.release()
        track = null
    }

    companion object
}

private const val TAG = "AudioIo"
