package com.caraudiolabpro

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AudioEngine {
    @Volatile var frequency = 60.0
    @Volatile var volume = 0.15
    @Volatile var mode = Mode.SINE
    @Volatile var sweepStart = 20.0
    @Volatile var sweepEnd = 200.0
    @Volatile var sweepSeconds = 10.0
    @Volatile private var running = false
    private var worker: Thread? = null
    private var track: AudioTrack? = null
    enum class Mode { SINE, SWEEP, WHITE_NOISE }

    @Synchronized fun start() {
        if (running) return
        val sampleRate = 44100
        val min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return
        val bufferSize = maxOf(min, 4096)
        track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        running = true
        track?.play()
        worker = Thread {
            val audio = ShortArray(2048)
            var phase = 0.0
            var frame = 0L
            while (running) {
                for (i in audio.indices step 2) {
                    val f = when (mode) {
                        Mode.SINE -> frequency.coerceIn(20.0, 20000.0)
                        Mode.SWEEP -> {
                            val p = ((frame.toDouble() / sampleRate) % sweepSeconds.coerceAtLeast(1.0)) / sweepSeconds.coerceAtLeast(1.0)
                            sweepStart.coerceAtLeast(20.0) * Math.pow((sweepEnd.coerceAtLeast(sweepStart + 1.0) / sweepStart.coerceAtLeast(20.0)), p)
                        }
                        Mode.WHITE_NOISE -> 0.0
                    }
                    val sample = if (mode == Mode.WHITE_NOISE) {
                        (Random.nextDouble(-1.0, 1.0) * volume * 32767).toInt()
                    } else {
                        val value = sin(phase) * volume * 32767
                        phase += 2.0 * PI * f / sampleRate
                        if (phase >= 2.0 * PI) phase %= 2.0 * PI
                        value.toInt()
                    }.coerceIn(-32768, 32767).toShort()
                    audio[i] = sample
                    audio[i + 1] = sample
                    frame++
                }
                track?.write(audio, 0, audio.size)
            }
        }.apply { name = "CarAudioLabAudio"; start() }
    }

    @Synchronized fun stop() {
        running = false
        try { worker?.join(300) } catch (_: InterruptedException) {}
        worker = null
        try { track?.pause(); track?.flush(); track?.release() } catch (_: Exception) {}
        track = null
    }
}
