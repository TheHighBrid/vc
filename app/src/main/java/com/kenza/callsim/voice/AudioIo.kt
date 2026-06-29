package com.kenza.callsim.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlin.concurrent.thread

/**
 * Audio constants shared by capture + playback.
 * ElevenLabs Conversational AI expects 16 kHz, mono, signed 16-bit PCM in,
 * and (by default) returns 16 kHz mono PCM out.
 */
object AudioConfig {
    const val SAMPLE_RATE = 16_000
    const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
}

/**
 * Captures microphone audio as raw PCM and hands each chunk to [onChunk]
 * on a background thread. Honors a mute flag so the user can silence the mic
 * without tearing down the session.
 */
class MicRecorder(
    private val onChunk: (ByteArray) -> Unit,
    private val onError: (String) -> Unit = {},
    private val onStreaming: () -> Unit = {},
) {

    @Volatile var muted: Boolean = false

    /**
     * True while the agent is actively speaking. We stop forwarding mic audio
     * (half-duplex) so the agent's own voice coming out of the speaker isn't
     * captured and mistaken for the user talking — which otherwise makes the
     * server interrupt and cut off every reply after the greeting.
     */
    @Volatile var agentSpeaking: Boolean = false

    private var record: AudioRecord? = null
    @Volatile private var running = false
    private var worker: Thread? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    @SuppressLint("MissingPermission") // caller guarantees RECORD_AUDIO is granted
    fun start() {
        if (running) return
        val minBuf = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE, AudioConfig.CHANNEL_IN, AudioConfig.ENCODING
        )
        if (minBuf <= 0) {
            onError("This device can't record 16 kHz mono audio.")
            return
        }
        // ~250 ms of audio per read keeps latency low without flooding the socket.
        val chunkBytes = AudioConfig.SAMPLE_RATE / 4 * 2
        val bufSize = maxOf(minBuf, chunkBytes)

        // Some devices/ROMs fail to open VOICE_COMMUNICATION (AEC) capture; fall
        // back to the plain mic so we always have an input source.
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
        )
        for (src in sources) {
            val r = runCatching {
                AudioRecord(src, AudioConfig.SAMPLE_RATE, AudioConfig.CHANNEL_IN, AudioConfig.ENCODING, bufSize)
            }.getOrNull()
            if (r != null && r.state == AudioRecord.STATE_INITIALIZED) {
                record = r
                Log.i(TAG, "AudioRecord initialized with source=$src")
                break
            }
            runCatching { r?.release() }
        }
        if (record == null) {
            onError("Microphone is unavailable (couldn't open an audio input).")
            return
        }

        // Attach platform echo cancellation / noise suppression so the speaker
        // output doesn't leak back into the mic. Best-effort: not all devices
        // support these, and they're harmless when absent.
        record?.audioSessionId?.let { session ->
            if (AcousticEchoCanceler.isAvailable())
                aec = runCatching { AcousticEchoCanceler.create(session)?.apply { enabled = true } }.getOrNull()
            if (NoiseSuppressor.isAvailable())
                ns = runCatching { NoiseSuppressor.create(session)?.apply { enabled = true } }.getOrNull()
            if (AutomaticGainControl.isAvailable())
                agc = runCatching { AutomaticGainControl.create(session)?.apply { enabled = true } }.getOrNull()
            Log.i(TAG, "effects aec=${aec != null} ns=${ns != null} agc=${agc != null}")
        }

        running = true
        runCatching { record?.startRecording() }
            .onFailure { onError("Couldn't start the microphone: ${it.message}"); running = false; return }

        var announced = false
        worker = thread(name = "mic-recorder") {
            val buffer = ByteArray(chunkBytes)
            while (running) {
                val read = record?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    if (!announced) { announced = true; onStreaming() }
                    // Drain the buffer always, but only forward when it's the
                    // user's turn (not muted, agent not speaking).
                    if (!muted && !agentSpeaking) onChunk(buffer.copyOf(read))
                } else if (read < 0) {
                    onError("Microphone read error ($read).")
                    break
                }
            }
        }
    }

    fun stop() {
        running = false
        worker?.join(500)
        worker = null
        runCatching { aec?.release() }; aec = null
        runCatching { ns?.release() }; ns = null
        runCatching { agc?.release() }; agc = null
        runCatching { record?.stop() }
        record?.release()
        record = null
    }
}

/**
 * Streams raw PCM coming back from the agent to the speaker/earpiece.
 * [flush] is used on interruptions so the agent stops mid-sentence instantly.
 */
class PcmPlayer(private val sampleRate: Int = AudioConfig.SAMPLE_RATE) {

    private var track: AudioTrack? = null

    fun start() {
        if (track != null) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioConfig.CHANNEL_OUT, AudioConfig.ENCODING
        )
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
            .setBufferSizeInBytes(maxOf(minBuf, sampleRate))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track?.play()
    }

    fun write(pcm: ByteArray) {
        track?.write(pcm, 0, pcm.size)
    }

    /** Drop everything queued — used when the user interrupts the agent. */
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
