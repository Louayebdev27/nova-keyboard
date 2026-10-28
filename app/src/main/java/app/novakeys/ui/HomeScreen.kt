@file:OptIn(ExperimentalMaterial3Api::class)

package app.novakeys.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.novakeys.data.Prefs
import app.novakeys.theme.ThemeStore

@Composable
fun HomeScreen(resumeTick: Int, onNavigate: (String) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    val enabled = remember(resumeTick) { MainActivity.isEnabled(ctx) }
    val isDefault = remember(resumeTick) { MainActivity.isDefault(ctx) }
    val theme = remember(resumeTick) { ThemeStore(ctx).active() }
    val numberRow = remember(resumeTick) { prefs.numberRow }
    var test by remember { mutableStateOf("") }

    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
                Text("Nova Keyboard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Private by design. No internet, no accounts, no tracking.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!enabled || !isDefault) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                if (!enabled) "Nova is not switched on yet" else "Nova is on, but not selected",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "Finish setup to start typing with it.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Button(onClick = { onNavigate("onboarding") }) { Text("Set up") }
                    }
                }
            }

            SettingsGroup("Preview") {
                KeyboardPreview(theme, Modifier.fillMaxWidth().padding(12.dp), numberRow = numberRow)
                OutlinedTextField(
                    value = test, onValueChange = { test = it }, label = { Text("Try it here") },
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                )
            }

            SettingsGroup("Appearance") {
                NavRow(Icons.Rounded.Palette, "Themes", "Pick a look or design your own") { onNavigate("themes") }
                GroupDivider()
                NavRow(Icons.Rounded.Keyboard, "Keys and layout", "Languages, number row, height, one-handed") { onNavigate("keys") }
            }

            SettingsGroup("Typing") {
                NavRow(Icons.Rounded.Spellcheck, "Typing", "Suggestions, capitals, clipboard") { onNavigate("typing") }
                GroupDivider()
                NavRow(Icons.Rounded.Vibration, "Sound and vibration", "Key sounds and haptic strength") { onNavigate("feedback") }
            }

            SettingsGroup("Your content") {
                NavRow(Icons.Rounded.Bolt, "Text shortcuts", "Type a short code, get a full phrase") { onNavigate("shortcuts") }
                GroupDivider()
                NavRow(Icons.Rounded.Book, "Personal dictionary", "Words the keyboard has learned") { onNavigate("dictionary") }
                GroupDivider()
                NavRow(Icons.Rounded.EmojiEmotions, "Stickers", "Your own images, sent from the keyboard") { onNavigate("stickers") }
                GroupDivider()
                NavRow(Icons.Rounded.Backup, "Backup and restore", "Save everything to a file") { onNavigate("backup") }
            }
        }
    }
}
