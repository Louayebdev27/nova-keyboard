@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.novakeys.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var toDelete by remember { mutableStateOf<KeyboardTheme?>(null) }

    Page("Themes", onBack) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onEdit("new") }) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Text("  New theme")
            }
            OutlinedButton(onClick = { importText = ""; importError = false; showImport = true }) { Text("Import") }
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            themes.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { t ->
                        ThemeCard(
                            t, t.id == activeId, Modifier.weight(1f),
                            onUse = { store.setActive(t.id); version++ },
                            onEdit = { onEdit(t.id) },
                            onShare = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, store.export(t))
                                }
                                ctx.startActivity(Intent.createChooser(send, "Share theme"))
                            },
                            onDelete = { toDelete = t },
                        )
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }

    toDelete?.let { t ->
        ConfirmDialog(
            "Delete \"${t.name}\"?", "This theme will be removed from this phone.", "Delete",
            onConfirm = { store.delete(t.id); toDelete = null; version++ },
            onDismiss = { toDelete = null },
        )
    }

    if (showImport) AlertDialog(
        onDismissRequest = { showImport = false },
        title = { Text("Import theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun ThemeCard(
    t: KeyboardTheme, active: Boolean, modifier: Modifier,
    onUse: () -> Unit, onEdit: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        modifier.clickable(onClick = onUse),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (active) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column {
            MiniKeyboard(t, Modifier.fillMaxWidth())
            Row(Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (active) Text("In use", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (active) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if (t.builtIn) "Customise" else "Edit") }, onClick = { menu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; onShare() })
                        if (!t.builtIn) DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                    }
                }
            }
        }
    }
}

// ---- Designer -----------------------------------------------------------------------------

private val fontChoices = listOf(
    "sans-serif" to "Sans", "sans-serif-medium" to "Medium", "sans-serif-condensed" to "Condensed",
    "serif" to "Serif", "monospace" to "Mono", "casual" to "Casual", "cursive" to "Cursive",
)

private val colourFields = listOf(
    "background" to "Keyboard background", "key" to "Keys", "special" to "Function keys",
    "accent" to "Accent (Enter, Shift)", "text" to "Letters", "specialText" to "Function key icons",
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
    var tab by remember { mutableIntStateOf(0) }
    var pickColor by remember { mutableStateOf<String?>(null) }
    var askDiscard by remember { mutableStateOf(false) }
    val dirty = t != initial

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) store.storeBackground(t.id, uri)?.let { name -> t = t.copy(backgroundImage = name) }
    }

    fun requestBack() { if (dirty) askDiscard = true else onBack() }
    BackHandler(enabled = dirty) { askDiscard = true }

    Page(
        "Design theme", { requestBack() },
        actions = {
            TextButton(onClick = { val saved = store.save(t); store.setActive(saved.id); onBack() }) { Text("Save and use") }
        },
        pinned = {
            Column {
                KeyboardPreview(t, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), numberRow = false)
                PrimaryTabRow(selectedTabIndex = tab) {
                    listOf("Colours", "Shape", "Text", "Photo").forEachIndexed { i, label ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                    }
                }
            }
        },
    ) {
        OutlinedTextField(
            t.name, { t = t.copy(name = it) }, label = { Text("Name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        when (tab) {
            0 -> SettingsGroup {
                colourFields.forEachIndexed { i, (k, label) ->
                    if (i > 0) GroupDivider()
                    Row(
                        Modifier.fillMaxWidth().clickable { pickColor = k }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        ColorDot(colorOf(t, k), 30)
                    }
                }
            }
            1 -> SettingsGroup {
                SettingSlider("Corner roundness", t.keyRadiusDp, 0f..24f, { "${it.roundToInt()} dp" }, { t = t.copy(keyRadiusDp = it) })
                GroupDivider()
                SettingSlider("Gap between keys", t.keyGapDp, 1f..10f, { "${it.roundToInt()} dp" }, { t = t.copy(keyGapDp = it) })
                GroupDivider()
                SettingSlider("Key opacity", t.keyAlpha, 0.2f..1f, { "${(it * 100).roundToInt()}%" }, { t = t.copy(keyAlpha = it) })
                GroupDivider()
                SettingSlider("Outline", t.borderWidthDp, 0f..3f, { "%.1f dp".format(it) }, {
                    t = t.copy(borderWidthDp = it, borderColor = if (t.borderColor == 0) t.accentColor else t.borderColor)
                })
                GroupDivider()
                SettingToggle("Key shadow", null, t.keyShadow) { t = t.copy(keyShadow = it) }
            }
            2 -> SettingsGroup {
                SettingChoice("Font", fontChoices, t.fontFamily) { t = t.copy(fontFamily = it) }
                GroupDivider()
                SettingToggle("Bold letters", null, t.boldText) { t = t.copy(boldText = it) }
                GroupDivider()
                SettingSlider("Letter size", t.textSizeSp, 14f..28f, { "${it.roundToInt()} sp" }, { t = t.copy(textSizeSp = it) })
            }
            else -> SettingsGroup(note = "Tip: lower the key opacity on the Shape tab so the photo shows through the keys.") {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { imagePicker.launch("image/*") }) {
                        Text(if (t.backgroundImage == null) "Choose photo" else "Change photo")
                    }
                    if (t.backgroundImage != null) OutlinedButton(onClick = { t = t.copy(backgroundImage = null) }) { Text("Remove") }
                }
                if (t.backgroundImage != null) {
                    GroupDivider()
                    SettingSlider("Darken photo", t.backgroundDim, 0f..0.8f, { "${(it * 100).roundToInt()}%" }, { t = t.copy(backgroundDim = it) })
                }
            }
        }
    }

    if (askDiscard) ConfirmDialog(
        "Discard changes?", "Your edits to this theme have not been saved.", "Discard",
        onConfirm = { askDiscard = false; onBack() }, onDismiss = { askDiscard = false },
    )

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

private val swatches: List<Int> = listOf(
    0xFF000000, 0xFF111318, 0xFF2A2F38, 0xFF5B5BF0, 0xFF7C83FF, 0xFF3DD6F5, 0xFF14A37F, 0xFF00E0A4,
    0xFFFFC857, 0xFFFF9A5C, 0xFFE0486F, 0xFFFF5FC8, 0xFFFFFFFF, 0xFFE7EAF1, 0xFFF6E4E8, 0xFFDDEFE8,
).map { it.toInt() }

@Composable
private fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var r by remember { mutableStateOf(((initial shr 16) and 0xFF).toFloat()) }
    var g by remember { mutableStateOf(((initial shr 8) and 0xFF).toFloat()) }
    var b by remember { mutableStateOf((initial and 0xFF).toFloat()) }
    val color = (0xFF shl 24) or (r.roundToInt() shl 16) or (g.roundToInt() shl 8) or b.roundToInt()
    var hex by remember { mutableStateOf("%06X".format(color and 0xFFFFFF)) }

    fun setAll(c: Int) {
        r = ((c shr 16) and 0xFF).toFloat(); g = ((c shr 8) and 0xFF).toFloat(); b = (c and 0xFF).toFloat()
        hex = "%06X".format(c and 0xFFFFFF)
    }
    fun syncHex() { hex = "%06X".format(color and 0xFFFFFF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a colour") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(color)))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    swatches.forEach { c ->
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                                .clickable { setAll(c) },
                        )
                    }
                }
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
