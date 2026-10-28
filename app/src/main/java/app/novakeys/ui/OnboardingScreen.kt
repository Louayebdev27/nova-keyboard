@file:OptIn(ExperimentalMaterial3Api::class)

package app.novakeys.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.novakeys.data.Prefs
import app.novakeys.theme.ThemeStore
import app.novakeys.theme.Themes

/**
 * Three-step first-run flow. The current step is worked out from the system state, so when the user
 * comes back from Android's keyboard settings the screen moves on by itself.
 */
@Composable
fun OnboardingScreen(resumeTick: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    val store = remember { ThemeStore(ctx) }
    val enabled = remember(resumeTick) { MainActivity.isEnabled(ctx) }
    val isDefault = remember(resumeTick) { MainActivity.isDefault(ctx) }
    var themeVersion by remember { mutableIntStateOf(0) }
    val active = remember(themeVersion) { store.active() }
    var test by remember { mutableStateOf("") }

    val step = when {
        !enabled -> 0
        !isDefault -> 1
        else -> 2
    }
    fun finish() { prefs.onboardingDone = true; onDone() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(
                            if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }
            Text(
                "Step ${step + 1} of 3", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp),
            )

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                Text(
                    when (step) { 0 -> "Turn on Nova Keyboard"; 1 -> "Make it your keyboard"; else -> "Choose a look" },
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (step) {
                        0 -> "Android needs your OK first. Open the keyboard list and switch Nova Keyboard on. " +
                            "Android will show a standard warning about keyboards. Nova has no internet access, so nothing you type can leave your phone."
                        1 -> "Nova is on. Now pick it as the keyboard you type with. A small chooser will appear."
                        else -> "You are all set. Pick a theme and try typing. You can change everything later in settings."
                    },
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                if (step < 2) {
                    MiniKeyboard(active, Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)))
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(Themes.presets, key = { it.id }) { t ->
                            val selected = t.id == active.id
                            Column(Modifier.width(150.dp).clickable { store.setActive(t.id); themeVersion++ }) {
                                MiniKeyboard(
                                    t,
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(
                                        if (selected) 3.dp else 1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(14.dp),
                                    ),
                                )
                                Text(
                                    t.name, style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 6.dp, start = 2.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = test, onValueChange = { test = it }, label = { Text("Try typing here") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Button(
                onClick = {
                    when (step) {
                        0 -> ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        1 -> (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                        else -> finish()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(when (step) { 0 -> "Open keyboard list"; 1 -> "Choose Nova Keyboard"; else -> "Finish" }) }
            TextButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (step == 2) "Skip theme" else "Skip for now")
            }
        }
    }
}
