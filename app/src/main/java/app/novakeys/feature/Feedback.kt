package app.novakeys.feature

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import app.novakeys.data.Prefs
import app.novakeys.keyboard.Key
import app.novakeys.keyboard.KeyAction
import app.novakeys.keyboard.KeyFeedback
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Key sounds are synthesised at start-up (no audio files shipped) and played from a static
 * AudioTrack, which keeps latency low. Haptics use a short one-shot vibration.
 */
class KeyFeedbackImpl(private val context: Context) : KeyFeedback {
    private val prefs = Prefs.get(context)
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private var track: AudioTrack? = null
    private var loadedPack: String? = null

    companion object {
        const val SAMPLE_RATE = 44100
        val packs = listOf("pop", "click", "soft", "chime")

        /** Builds one short mono 16-bit sound. Pure maths, so it can be tested without a device. */
        fun synthesize(pack: String): ShortArray {
            val ms = when (pack) { "click" -> 28; "soft" -> 70; "chime" -> 160; else -> 55 }
            val n = SAMPLE_RATE * ms / 1000
            val out = ShortArray(n)
            val rnd = Random(7)
            var phase = 0.0
            for (i in 0 until n) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / n
                val v = when (pack) {
                    "click" -> {
                        val env = exp(-t * 220.0)
                        (rnd.nextDouble(-1.0, 1.0) * 0.45 + sin(2 * PI * 2400 * t) * 0.55) * env
                    }
                    "soft" -> {
                        val env = sin(PI * progress) * exp(-t * 18.0)
                        sin(2 * PI * 420 * t) * env * 0.8
                    }
                    "chime" -> {
                        val env = exp(-t * 22.0)
                        (sin(2 * PI * 880 * t) * 0.6 + sin(2 * PI * 1320 * t) * 0.3 + sin(2 * PI * 1760 * t) * 0.1) * env
                    }
                    else -> { // pop: quick downward sweep
                        val f = 700.0 - 450.0 * progress
                        phase += 2 * PI * f / SAMPLE_RATE
                        sin(phase) * exp(-t * 55.0)
                    }
                }
                out[i] = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.9).toInt().toShort()
            }
            return out
        }
    }

    private fun ensureTrack() {
        val pack = prefs.soundPack
        if (track != null && loadedPack == pack) return
        track?.release()
        val pcm = synthesize(pack)
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(pcm, 0, pcm.size)
        track = t
        loadedPack = pack
    }

    override fun onPress(key: Key) {
        if (prefs.vibrationEnabled && prefs.vibrationMs > 0) vibrate(prefs.vibrationMs)
        if (prefs.soundEnabled && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) playSound(key)
    }

    private fun vibrate(ms: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(ms.toLong())
    }

    private fun playSound(key: Key) {
        runCatching {
            ensureTrack()
            val t = track ?: return
            t.setVolume(prefs.soundVolume.coerceIn(0f, 1f))
            // Special keys sound a little lower so they are easy to tell apart.
            val special = key.action != KeyAction.CHAR
            t.playbackRate = if (special) (SAMPLE_RATE * 0.8f).toInt() else SAMPLE_RATE
            t.stop()
            t.reloadStaticData()
            t.play()
        }
    }

    /** Plays the sample sound and buzz even when the toggles are off, so settings can be tried. */
    fun preview() {
        if (prefs.vibrationMs > 0) vibrate(prefs.vibrationMs)
        playSound(Key("a"))
    }

    fun release() {
        track?.release()
        track = null
    }
}
