@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.novakeys.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.novakeys.theme.KeyboardTheme
import app.novakeys.theme.ThemeStore
import app.novakeys.theme.Themes
import kotlin.math.roundToInt

// ---- Gallery ------------------------------------------------------------------------------

@Composable
fun ThemeGalleryScreen(onBack: () -> Unit, onEdit: (String) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ThemeStore(ctx) }
    var version by remember { mutableIntStateOf(0) }
    val themes = remember(version) { store.all() }
    val activeId = remember(version) { store.active().id }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf(false) }

    Page("Themes", onBack) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onEdit("new") }) { Text("Design new theme") }
            OutlinedButton(onClick = { importText = ""; importError = false; showImport = true }) { Text("Import") }
        }
        themes.forEach { t ->
            val active = t.id == activeId
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (active) Text("In use", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        listOf(t.background, t.keyColor, t.specialKeyColor, t.accentColor, t.textColor).forEach {
                            ColorDot(it, 26); Spacer(Modifier.width(5.dp))
                        }
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!active) TextButton(onClick = { store.setActive(t.id); version++ }) { Text("Use") }
                        TextButton(onClick = { onEdit(t.id) }) { Text(if (t.builtIn) "Customise" else "Edit") }
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, store.export(t))
                            }
                            ctx.startActivity(Intent.createChooser(send, "Share theme"))
                        }) { Text("Share") }
                        if (!t.builtIn) TextButton(onClick = { store.delete(t.id); version++ }) { Text("Delete") }
                    }
                }
            }
        }
    }

    if (showImport) AlertDialog(
        onDismissRequest = { showImport = false },
        title = { Text("Import theme") },
        text = {
            Column {
                Text("Paste a theme someone shared with you.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(importText, { importText = it; importError = false }, Modifier.fillMaxWidth().height(160.dp), isError = importError)
                if (importError) Text("That is not a valid theme.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = store.import(importText)
                if (t == null) importError = true else { showImport = false; version++ }
            }) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = { showImport = false }) { Text("Cancel") } },
    )
}

// ---- Designer -----------------------------------------------------------------------------

private val fontChoices = listOf(
    "sans-serif" to "Sans", "sans-serif-medium" to "Medium", "sans-serif-condensed" to "Condensed",
    "serif" to "Serif", "monospace" to "Mono", "casual" to "Casual", "cursive" to "Cursive",
)

@Composable
fun ThemeDesignerScreen(id: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ThemeStore(ctx) }
    val initial = remember(id) {
        val base = if (id == "new") Themes.default.copy(name = "My theme") else store.find(id) ?: Themes.default
        if (base.builtIn || id == "new") base.copy(id = store.newId(), builtIn = false, name = if (id == "new") "My theme" else base.name + " copy")
        else base
    }
    var t by remember { mutableStateOf(initial) }
    var pickColor by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) store.storeBackground(t.id, uri)?.let { name -> t = t.copy(backgroundImage = name) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Design theme") },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
                actions = {
                    TextButton(onClick = { val saved = store.save(t); store.setActive(saved.id); onBack() }) { Text("Save and use") }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            KeyboardPreview(t, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), numberRow = false)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                OutlinedTextField(
                    t.name, { t = t.copy(name = it) }, label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                )

                SectionHeader("Colours")
                val fields = listOf(
                    "background" to "Keyboard background", "key" to "Keys", "special" to "Function keys",
                    "accent" to "Accent (Enter, Shift)", "text" to "Letters", "specialText" to "Function key icons",
                )
                fields.forEach { (k, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { pickColor = k }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, Modifier.weight(1f))
                        ColorDot(colorOf(t, k), 30)
                    }
                }

                SectionHeader("Shape")
                SettingSlider("Corner roundness", t.keyRadiusDp, 0f..24f, { "${it.roundToInt()} dp" }, { t = t.copy(keyRadiusDp = it) })
                SettingSlider("Gap between keys", t.keyGapDp, 1f..10f, { "${it.roundToInt()} dp" }, { t = t.copy(keyGapDp = it) })
                SettingSlider("Key opacity", t.keyAlpha, 0.2f..1f, { "${(it * 100).roundToInt()}%" }, { t = t.copy(keyAlpha = it) })
                SettingSlider("Outline", t.borderWidthDp, 0f..3f, { "%.1f dp".format(it) }, {
                    t = t.copy(borderWidthDp = it, borderColor = if (t.borderColor == 0) t.accentColor else t.borderColor)
                })
                SettingToggle("Key shadow", null, t.keyShadow) { t = t.copy(keyShadow = it) }

                SectionHeader("Text")
                SettingChoice("Font", fontChoices, t.fontFamily) { t = t.copy(fontFamily = it) }
                SettingToggle("Bold letters", null, t.boldText) { t = t.copy(boldText = it) }
                SettingSlider("Letter size", t.textSizeSp, 14f..28f, { "${it.roundToInt()} sp" }, { t = t.copy(textSizeSp = it) })

                SectionHeader("Background photo")
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { imagePicker.launch("image/*") }) { Text(if (t.backgroundImage == null) "Choose photo" else "Change photo") }
                    if (t.backgroundImage != null) OutlinedButton(onClick = { t = t.copy(backgroundImage = null) }) { Text("Remove") }
                }
                if (t.backgroundImage != null) {
                    SettingSlider("Darken photo", t.backgroundDim, 0f..0.8f, { "${(it * 100).roundToInt()}%" }, { t = t.copy(backgroundDim = it) })
                    Text(
                        "Tip: lower the key opacity so the photo shows through the keys.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }
    }

    pickColor?.let { key ->
        ColorPickerDialog(
            initial = colorOf(t, key),
            onDismiss = { pickColor = null },
            onPick = { c ->
                t = when (key) {
                    "background" -> t.copy(background = c)
                    "key" -> t.copy(keyColor = c, keyPressedColor = lighten(c))
                    "special" -> t.copy(specialKeyColor = c)
                    "accent" -> t.copy(accentColor = c, accentTextColor = readableOn(c))
                    "text" -> t.copy(textColor = c)
                    else -> t.copy(specialTextColor = c, hintColor = c)
                }
                pickColor = null
            },
        )
    }
}

private fun colorOf(t: KeyboardTheme, key: String): Int = when (key) {
    "background" -> t.background
    "key" -> t.keyColor
    "special" -> t.specialKeyColor
    "accent" -> t.accentColor
    "text" -> t.textColor
    else -> t.specialTextColor
}

/** Pressed colour: lighter on dark keys, darker on light keys. */
private fun lighten(c: Int): Int {
    val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
    val lum = (0.299 * r + 0.587 * g + 0.114 * b)
    val f = if (lum < 128) 1.35f else 0.85f
    fun ch(v: Int) = (v * f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
}

private fun readableOn(c: Int): Int {
    val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
    return if (0.299 * r + 0.587 * g + 0.114 * b > 150) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
}

@Composable
private fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var r by remember { mutableStateOf(((initial shr 16) and 0xFF).toFloat()) }
    var g by remember { mutableStateOf(((initial shr 8) and 0xFF).toFloat()) }
    var b by remember { mutableStateOf((initial and 0xFF).toFloat()) }
    val color = (0xFF shl 24) or (r.roundToInt() shl 16) or (g.roundToInt() shl 8) or b.roundToInt()
    var hex by remember { mutableStateOf("%06X".format(color and 0xFFFFFF)) }

    fun syncHex() { hex = "%06X".format(color and 0xFFFFFF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a colour") },
        text = {
            Column {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth().height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color(color)),
                )
                Slider(r, { r = it; syncHex() }, valueRange = 0f..255f)
                Slider(g, { g = it; syncHex() }, valueRange = 0f..255f)
                Slider(b, { b = it; syncHex() }, valueRange = 0f..255f)
                OutlinedTextField(
                    hex, { v ->
                        hex = v.filter { it.isLetterOrDigit() }.take(6).uppercase()
                        if (hex.length == 6) hex.toIntOrNull(16)?.let { c ->
                            r = ((c shr 16) and 0xFF).toFloat(); g = ((c shr 8) and 0xFF).toFloat(); b = (c and 0xFF).toFloat()
                        }
                    },
                    label = { Text("Hex") }, singleLine = true, prefix = { Text("#") },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onPick(color) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
