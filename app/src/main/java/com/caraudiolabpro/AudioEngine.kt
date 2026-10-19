package com.caraudiolabpro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AudioEngine {
    @Volatile var frequency = 60.0
    @Volatile var volume = 0.12
    @Volatile var mode = Mode.SINE
    @Volatile var sweepStart = 20.0
    @Volatile var sweepEnd = 200.0
    @Volatile var sweepSeconds = 10.0
    @Volatile private var running = false
    private var worker: Thread? = null
    private var track: AudioTrack? = null

    enum class Mode { SINE, SWEEP, WHITE_NOISE }

    @Synchronized
    fun start() {
        if (running) return
        val sampleRate = 44100
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        check(minBuffer > 0) { "Audio output is not available on this device" }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, 8192))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release()
            throw IllegalStateException("Could not initialize audio output")
        }

        track = audioTrack
        running = true
        try {
            audioTrack.play()
        } catch (e: Exception) {
            running = false
            track = null
            audioTrack.release()
            throw e
        }

        worker = Thread {
            val frames = 1024
            val buffer = ShortArray(frames * 2)
            var phase = 0.0
            var frame = 0L
            var fade = 0.0
            try {
                while (running) {
                    for (i in 0 until frames) {
                        val f = when (mode) {
                            Mode.SINE -> frequency.coerceIn(20.0, 20000.0)
                            Mode.SWEEP -> {
                                val duration = sweepSeconds.coerceAtLeast(1.0)
                                val t = ((frame.toDouble() / sampleRate) % duration) / duration
                                val start = sweepStart.coerceIn(20.0, 20000.0)
                                val end = sweepEnd.coerceIn(start, 20000.0)
                                start * Math.pow((end / start).coerceAtLeast(1.0), t)
                            }
                            Mode.WHITE_NOISE -> 0.0
                        }
                        val raw = if (mode == Mode.WHITE_NOISE) {
                            Random.nextDouble(-1.0, 1.0)
                        } else {
                            val value = sin(phase)
                            phase += 2.0 * PI * f / sampleRate
                            if (phase >= 2.0 * PI) phase %= 2.0 * PI
                            value
                        }
                        fade = (fade + 1.0 / (sampleRate * 0.06)).coerceAtMost(1.0)
                        val sample = (raw * volume.coerceIn(0.0, 0.25) * fade * 32767.0)
                            .toInt().coerceIn(-8192, 8192).toShort()
                        buffer[i * 2] = sample
                        buffer[i * 2 + 1] = sample
                        frame++
                    }
                    if (!running) break
                    val written = audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                    if (written < 0) {
                        running = false
                        break
                    }
                }
            } catch (_: Exception) {
                running = false
            }
        }.apply {
            name = "CarAudioLabAudio"
            priority = Thread.NORM_PRIORITY - 1
            start()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        try { worker?.join(500) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        worker = null
        val old = track
        track = null
        try { old?.pause() } catch (_: Exception) {}
        try { old?.flush() } catch (_: Exception) {}
        try { old?.stop() } catch (_: Exception) {}
        try { old?.release() } catch (_: Exception) {}
    }

    fun isRunning() = running
}
