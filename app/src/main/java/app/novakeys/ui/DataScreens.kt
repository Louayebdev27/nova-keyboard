@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.novakeys.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.novakeys.data.Backup
import app.novakeys.data.Predictors
import app.novakeys.data.Prefs
import app.novakeys.data.ShortcutStore
import app.novakeys.data.StickerStore
import app.novakeys.keyboard.Layouts

// ---- Text shortcuts -----------------------------------------------------------------------

@Composable
fun ShortcutsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { ShortcutStore(ctx) }
    var version by remember { mutableIntStateOf(0) }
    val items = remember(version) { store.all().toList() }
    var trigger by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    Page("Text shortcuts", onBack) {
        SettingsGroup("New shortcut", note = "Type the short code and press space. The keyboard swaps it for the full text.") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    trigger, { trigger = it.filter { c -> !c.isWhitespace() } }, label = { Text("Short code (e.g. omw)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(text, { text = it }, label = { Text("Full text (e.g. On my way!)") }, modifier = Modifier.fillMaxWidth())
                FilledTonalButton(enabled = trigger.isNotBlank() && text.isNotBlank(), onClick = {
                    store.put(trigger, text); trigger = ""; text = ""; version++
                }) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Text("  Add shortcut")
                }
            }
        }
        SettingsGroup("Your shortcuts (${items.size})") {
            if (items.isEmpty()) EmptyState(Icons.Rounded.Bolt, "No shortcuts yet. Add your first one above.")
            items.forEachIndexed { i, (k, v) ->
                if (i > 0) GroupDivider()
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(k, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(v, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { store.remove(k); version++ }) { Icon(Icons.Rounded.Delete, "Delete $k") }
                }
            }
        }
    }
}

// ---- Personal dictionary ------------------------------------------------------------------

@Composable
fun DictionaryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    val langs = remember { prefs.languages }
    var lang by remember { mutableStateOf(langs.firstOrNull() ?: "en") }
    var version by remember { mutableIntStateOf(0) }
    val predictor = remember(lang) { Predictors.get(ctx, lang) }
    val words = remember(lang, version) { predictor.learnedWords() }
    var newWord by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    Page("Personal dictionary", onBack) {
        if (langs.size > 1) {
            SettingsGroup {
                SettingChoice(null, langs.map { it to (Layouts.languageNames[it] ?: it) }, lang) { lang = it }
            }
        }
        SettingsGroup("Add a word") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(newWord, { newWord = it }, label = { Text("Word") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FilledTonalButton(enabled = newWord.isNotBlank(), onClick = { predictor.addWord(newWord); newWord = ""; version++ }) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Text("  Add")
                }
            }
        }
        SettingsGroup("Learned words (${words.size})", note = if (words.size > 300) "Showing the first 300." else null) {
            if (words.isEmpty()) EmptyState(Icons.Rounded.Book, "Nothing learned yet. Words you type will show up here.")
            words.take(300).forEachIndexed { i, w ->
                if (i > 0) GroupDivider()
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(w, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { predictor.removeWord(w); version++ }) { Icon(Icons.Rounded.Close, "Remove $w") }
                }
            }
            if (words.isNotEmpty()) {
                GroupDivider()
                TextButton(onClick = { confirmClear = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Forget everything", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmClear) ConfirmDialog(
        "Forget everything?",
        "This removes every word the keyboard learned for ${Layouts.languageNames[lang] ?: lang}.",
        "Forget",
        onConfirm = { predictor.clearLearned(); confirmClear = false; version++ },
        onDismiss = { confirmClear = false },
    )
}

// ---- Stickers -----------------------------------------------------------------------------

@Composable
fun StickersScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { StickerStore(ctx) }
    var version by remember { mutableIntStateOf(0) }
    val files = remember(version) { store.all() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { store.import(it) }
        version++
    }

    Page("Stickers", onBack) {
        SettingsGroup(note = "Open the Tools menu on the keyboard, tap Stickers, and send them to any app that accepts images from keyboards.") {
            FilledTonalButton(onClick = { picker.launch("image/*") }, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Text("  Add images")
            }
        }
        SettingsGroup("Your stickers (${files.size})") {
            if (files.isEmpty()) EmptyState(Icons.Rounded.EmojiEmotions, "No stickers yet.")
            else FlowRow(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                files.forEach { f ->
                    val bmp = remember(f.path) {
                        runCatching { BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 4 }) }.getOrNull()
                    }
                    Box(Modifier.size(92.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)) {
                        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(92.dp).padding(8.dp), contentScale = ContentScale.Fit)
                        IconButton(onClick = { store.delete(f); version++ }, Modifier.align(Alignment.TopEnd).size(32.dp)) {
                            Icon(Icons.Rounded.Close, "Delete sticker", Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---- Backup -------------------------------------------------------------------------------

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) message = if (Backup.export(ctx, uri)) "Backup saved." else "Could not save the backup."
    }
    val load = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) message = Backup.import(ctx, uri) ?: "That file is not a Nova Keyboard backup."
    }

    Page("Backup and restore", onBack) {
        SettingsGroup(
            "What is included",
            note = "Background photos and stickers are not included. You choose where the file is saved. Nothing is uploaded anywhere.",
        ) {
            Text(
                "Settings, custom themes, shortcuts and your personal dictionary, in one file.",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp),
            )
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { save.launch("nova-keyboard-backup.json") }, Modifier.fillMaxWidth()) { Text("Save a backup file") }
            OutlinedButton(onClick = { load.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text("Restore from a backup file") }
        }
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
        )
    }
}
