package com.kenza.callsim.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
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
class MicRecorder(private val onChunk: (ByteArray) -> Unit) {

    @Volatile var muted: Boolean = false

    private var record: AudioRecord? = null
    @Volatile private var running = false
    private var worker: Thread? = null

    @SuppressLint("MissingPermission") // caller guarantees RECORD_AUDIO is granted
    fun start() {
        if (running) return
        val minBuf = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE, AudioConfig.CHANNEL_IN, AudioConfig.ENCODING
        )
        // ~250 ms of audio per read keeps latency low without flooding the socket.
        val chunkBytes = AudioConfig.SAMPLE_RATE / 4 * 2
        val bufSize = maxOf(minBuf, chunkBytes)

        record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNEL_IN,
            AudioConfig.ENCODING,
            bufSize
        )
        if (record?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            return
        }

        running = true
        record?.startRecording()
        worker = thread(name = "mic-recorder") {
            val buffer = ByteArray(chunkBytes)
            while (running) {
                val read = record?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0 && !muted) {
                    onChunk(buffer.copyOf(read))
                }
            }
        }
    }

    fun stop() {
        running = false
        worker?.join(500)
        worker = null
        runCatching { record?.stop() }
        record?.release()
        record = null
    }
}

/**
 * Streams raw PCM coming back from the agent to the speaker/earpiece.
 * [flush] is used on interruptions so the agent stops mid-sentence instantly.
 */
class PcmPlayer {

    private var track: AudioTrack? = null

    fun start() {
        if (track != null) return
        val minBuf = AudioTrack.getMinBufferSize(
            AudioConfig.SAMPLE_RATE, AudioConfig.CHANNEL_OUT, AudioConfig.ENCODING
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
                    .setSampleRate(AudioConfig.SAMPLE_RATE)
                    .setChannelMask(AudioConfig.CHANNEL_OUT)
                    .setEncoding(AudioConfig.ENCODING)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, AudioConfig.SAMPLE_RATE))
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
