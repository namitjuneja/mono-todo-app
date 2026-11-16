package com.example.monofocus

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class Sound { TICK, ADD, CHECK, UNDO, WIN, TIMER_START, TIMER_DONE, SWEEP }

/**
 * Small, soft tones generated in code (no audio files). Played as UI "sonification", so they follow the
 * phone's silent / vibrate mode and stay quiet.
 */
object Sfx {
    @Volatile var enabled = true
    private const val RATE = 22050
    private val tracks = HashMap<Sound, AudioTrack>()

    private class Part(val freq: Double, val startMs: Int, val durMs: Int, val amp: Double, val decay: Double)

    private fun parts(s: Sound): List<Part> = when (s) {
        Sound.TICK -> listOf(Part(1100.0, 0, 50, 0.16, 70.0))
        Sound.ADD -> listOf(Part(740.0, 0, 70, 0.18, 45.0), Part(988.0, 45, 90, 0.16, 40.0))
        Sound.CHECK -> listOf(Part(659.0, 0, 110, 0.20, 28.0), Part(880.0, 70, 170, 0.20, 22.0))
        Sound.UNDO -> listOf(Part(784.0, 0, 90, 0.17, 32.0), Part(587.0, 70, 160, 0.17, 26.0))
        Sound.WIN -> listOf(Part(523.0, 0, 280, 0.19, 10.0), Part(659.0, 110, 280, 0.19, 10.0), Part(784.0, 220, 420, 0.19, 8.0), Part(1047.0, 330, 520, 0.11, 8.0))
        Sound.TIMER_START -> listOf(Part(520.0, 0, 90, 0.15, 50.0))
        Sound.TIMER_DONE -> listOf(Part(784.0, 0, 720, 0.19, 6.0), Part(1176.0, 0, 520, 0.09, 8.0), Part(988.0, 360, 720, 0.16, 6.0))
        Sound.SWEEP -> listOf(Part(880.0, 0, 80, 0.13, 30.0), Part(659.0, 60, 80, 0.13, 30.0), Part(494.0, 120, 170, 0.13, 22.0))
    }

    private fun build(s: Sound): AudioTrack {
        val ps = parts(s)
        val totalMs = ps.maxOf { it.startMs + it.durMs } + 30
        val n = RATE * totalMs / 1000
        val mix = DoubleArray(n)
        for (p in ps) {
            val from = RATE * p.startMs / 1000
            val len = RATE * p.durMs / 1000
            for (i in 0 until len) {
                val idx = from + i
                if (idx >= n) break
                val t = i.toDouble() / RATE
                val attack = min(1.0, t / 0.004)          // no click at the start
                val tail = min(1.0, (len - i).toDouble() / (RATE * 0.01)) // or at the end
                mix[idx] += p.amp * attack * tail * exp(-p.decay * t) * sin(2.0 * PI * p.freq * t)
            }
        }
        val pcm = ShortArray(n) { (mix[it].coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort() }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.setVolume(0.8f)
        return track
    }

    fun play(sound: Sound) {
        if (!enabled) return
        try {
            val t = tracks.getOrPut(sound) { build(sound) }
            t.stop()
            t.setPlaybackHeadPosition(0)
            t.play()
        } catch (_: Exception) {
            tracks.remove(sound)
        }
    }
}
