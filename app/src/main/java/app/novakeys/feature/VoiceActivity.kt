package app.novakeys.feature

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import app.novakeys.R
import android.os.Bundle

/**
 * Voice typing through the phone's own speech recogniser, so this app never needs the microphone
 * permission. The result is handed back to the keyboard with a local broadcast-style callback.
 */
class VoiceActivity : Activity() {

    companion object {
        /** Set by the keyboard service while it is alive. Called on the main thread. */
        @Volatile var onResult: ((String) -> Unit)? = null
        const val EXTRA_LANG = "lang"
        private const val REQ = 41
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lang = intent.getStringExtra(EXTRA_LANG) ?: "en-US"
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            startActivityForResult(i, REQ)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.voice_unavailable, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ && resultCode == RESULT_OK) {
            val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) onResult?.invoke(text)
        }
        finish()
    }
}
