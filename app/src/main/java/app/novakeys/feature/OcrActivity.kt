package app.novakeys.feature

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import app.novakeys.data.ClipboardStore

/**
 * Pick a photo or screenshot, read the text on it offline (ML Kit Latin model), then copy it.
 * The copy also lands in clipboard history, so it can be pasted straight from the keyboard.
 */
class OcrActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var result: EditText
    private lateinit var copyBtn: Button
    private val REQ_PICK = 52

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
            setBackgroundColor(Color.WHITE)
        }
        status = TextView(this).apply {
            text = "Choose an image to read text from."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.DKGRAY)
        }
        val pick = Button(this).apply { text = "Choose image"; setOnClickListener { pickImage() } }
        result = EditText(this).apply {
            gravity = Gravity.TOP
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT
            minLines = 8
            setTextColor(Color.BLACK)
        }
        copyBtn = Button(this).apply {
            text = "Copy text"
            isEnabled = false
            setOnClickListener { copyText() }
        }
        root.addView(status)
        root.addView(pick)
        root.addView(result, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(copyBtn)
        setContentView(root)
        if (intent.action == Intent.ACTION_SEND) (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { recognize(it) }
        else pickImage()
    }

    private fun pickImage() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(Intent.createChooser(i, "Choose image"), REQ_PICK)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK && resultCode == RESULT_OK) data?.data?.let { recognize(it) }
    }

    private fun recognize(uri: Uri) {
        status.text = "Reading…"
        runCatching {
            val image = InputImage.fromFilePath(this, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                .addOnSuccessListener { r ->
                    result.setText(r.text)
                    status.text = if (r.text.isBlank()) "No text found." else "Done. Edit if needed, then copy."
                    copyBtn.isEnabled = r.text.isNotBlank()
                }
                .addOnFailureListener { status.text = "Could not read this image." }
        }.onFailure { status.text = "Could not open this image." }
    }

    private fun copyText() {
        val t = result.text.toString()
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("ocr", t))
        ClipboardStore(this).add(t)
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }
}
