@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.novakeys.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import app.novakeys.theme.ThemeStore

@Composable
fun HomeScreen(resumeTick: Int, onNavigate: (String) -> Unit) {
    val ctx = LocalContext.current
    val enabled = remember(resumeTick) { MainActivity.isEnabled(ctx) }
    val isDefault = remember(resumeTick) { MainActivity.isDefault(ctx) }
    val theme = remember(resumeTick) { ThemeStore(ctx).active() }
    var test by remember { mutableStateOf("") }

    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text("Nova Keyboard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Private by design. No internet access, no accounts, no tracking.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!enabled || !isDefault) {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Get started", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        StepLine(1, "Turn on Nova Keyboard", enabled)
                        StepLine(2, "Switch to it as your keyboard", isDefault)
                        if (!enabled) {
                            Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }, Modifier.fillMaxWidth()) {
                                Text("Open keyboard settings")
                            }
                        } else {
                            Button(onClick = {
                                (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                            }, Modifier.fillMaxWidth()) { Text("Choose Nova Keyboard") }
                        }
                    }
                }
            }

            SectionHeader("Your keyboard")
            KeyboardPreview(theme, Modifier.fillMaxWidth().padding(horizontal = 16.dp), numberRow = true)
            OutlinedTextField(
                value = test, onValueChange = { test = it },
                label = { Text("Try it here") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                keyboardOptions = KeyboardOptions.Default,
            )

            SectionHeader("Customise")
            NavRow("Themes", "Pick a look, or design your own with a photo background") { onNavigate("themes") }
            NavRow("Keys and layout", "Languages, number row, bottom row, key height, one-handed mode") { onNavigate("keys") }
            NavRow("Typing", "Suggestions, auto-capitalise, double-space period, clipboard history") { onNavigate("typing") }
            NavRow("Sound and vibration", "Four synthesised key sounds, volume, haptic strength") { onNavigate("feedback") }

            SectionHeader("Your content")
            NavRow("Text shortcuts", "Type a short code, get a full phrase") { onNavigate("shortcuts") }
            NavRow("Personal dictionary", "Words the keyboard has learned. Add or remove any.") { onNavigate("dictionary") }
            NavRow("Stickers", "Import your own images to send from the keyboard") { onNavigate("stickers") }
            NavRow("Backup and restore", "Save everything to a file, or load it on a new phone") { onNavigate("backup") }
        }
    }
}

@Composable
private fun StepLine(n: Int, text: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (done) "✓" else "$n", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
