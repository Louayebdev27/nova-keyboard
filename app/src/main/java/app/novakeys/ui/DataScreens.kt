@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.novakeys.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
        Text(
            "Type the short code and press space. The keyboard swaps it for the full text.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(trigger, { trigger = it.filter { c -> !c.isWhitespace() } }, label = { Text("Short code (e.g. omw)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(text, { text = it }, label = { Text("Full text (e.g. On my way!)") }, modifier = Modifier.fillMaxWidth())
            Button(enabled = trigger.isNotBlank() && text.isNotBlank(), onClick = {
                store.put(trigger, text); trigger = ""; text = ""; version++
            }) { Text("Add shortcut") }
        }
        SectionHeader("Your shortcuts (${items.size})")
        items.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(k, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(v, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
                TextButton(onClick = { store.remove(k); version++ }) { Text("Delete") }
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
        SettingChoice("Language", langs.map { it to (Layouts.languageNames[it] ?: it) }, lang) { lang = it }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(newWord, { newWord = it }, label = { Text("Add a word") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = newWord.isNotBlank(), onClick = { predictor.addWord(newWord); newWord = ""; version++ }) { Text("Add") }
                OutlinedButton(enabled = words.isNotEmpty(), onClick = { confirmClear = true }) { Text("Forget everything") }
            }
        }
        SectionHeader("Learned words (${words.size})")
        if (words.isEmpty()) {
            Text("Nothing learned yet. Words you type will show up here.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp))
        }
        words.take(300).forEach { w ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(w, Modifier.weight(1f))
                TextButton(onClick = { predictor.removeWord(w); version++ }) { Text("Remove") }
            }
        }
        if (words.size > 300) Text("Showing the first 300.", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall)
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Forget everything?") },
        text = { Text("This removes every word the keyboard learned for ${Layouts.languageNames[lang] ?: lang}.") },
        confirmButton = { TextButton(onClick = { predictor.clearLearned(); confirmClear = false; version++ }) { Text("Forget") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
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
        Text(
            "Add your own pictures. Open the Tools menu on the keyboard, tap Stickers, and send them to any app that accepts images from keyboards.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Button(onClick = { picker.launch("image/*") }, Modifier.padding(horizontal = 20.dp)) { Text("Add images") }
        SectionHeader("Your stickers (${files.size})")
        androidx.compose.foundation.layout.FlowRow(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            files.forEach { f ->
                val bmp = remember(f.path) {
                    runCatching { BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 4 }) }.getOrNull()
                }
                Box(Modifier.size(92.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.size(92.dp).padding(6.dp), contentScale = ContentScale.Fit)
                    TextButton(onClick = { store.delete(f); version++ }, Modifier.align(Alignment.TopEnd)) { Text("✕") }
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
        Text(
            "A backup is one file with your settings, custom themes, shortcuts and personal dictionary. " +
                "You choose where it is saved. Nothing is uploaded anywhere.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { save.launch("nova-keyboard-backup.json") }, Modifier.fillMaxWidth()) { Text("Save a backup file") }
            OutlinedButton(onClick = { load.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth()) { Text("Restore from a backup file") }
            Text("Background photos are not included in backups. Stickers are not included either.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
