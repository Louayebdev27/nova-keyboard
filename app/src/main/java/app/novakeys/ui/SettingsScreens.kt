@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.novakeys.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.novakeys.data.Prefs
import app.novakeys.feature.KeyFeedbackImpl
import app.novakeys.keyboard.BottomKeys
import app.novakeys.keyboard.Layouts
import app.novakeys.theme.ThemeStore
import kotlin.math.roundToInt

private val defaultBottomRow = listOf("symbols", "emoji", "comma", "space", "period", "enter")

/** Keys that must always be on the bottom row, otherwise the keyboard cannot be used. */
private val lockedKeys = setOf("space", "enter")

@Composable
fun KeysScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    val theme = remember { ThemeStore(ctx).active() }
    var languages by remember { mutableStateOf(prefs.languages) }
    var numberRow by remember { mutableStateOf(prefs.numberRow) }
    var height by remember { mutableStateOf(prefs.keyHeightScale) }
    var oneHanded by remember { mutableStateOf(prefs.oneHanded) }
    var preview by remember { mutableStateOf(prefs.keyPreview) }
    var longPress by remember { mutableStateOf(prefs.longPressMs.toFloat()) }
    val bottom = remember { mutableStateListOf<String>().also { it.addAll(prefs.bottomRow) } }

    fun saveBottom() { prefs.bottomRow = bottom.toList() }

    Page(
        "Keys and layout", onBack,
        pinned = {
            KeyboardPreview(
                theme, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                numberRow = numberRow, heightScale = height, bottom = bottom.toList(),
            )
        },
    ) {
        SettingsGroup("Languages", note = "Switch with the globe key, or hold the space bar. At least one must stay on.") {
            val entries = Layouts.languageNames.entries.toList()
            entries.forEachIndexed { i, (id, name) ->
                if (i > 0) GroupDivider()
                SettingToggle(name, null, id in languages) { on ->
                    val next = if (on) (languages + id).distinct() else languages - id
                    if (next.isNotEmpty()) { languages = next; prefs.languages = next }
                }
            }
        }

        SettingsGroup("Layout") {
            SettingToggle("Number row", "Keep digits above the letters", numberRow) { numberRow = it; prefs.numberRow = it }
            GroupDivider()
            SettingSlider("Key height", height, 0.8f..1.4f, { "${(it * 100).roundToInt()}%" }, { height = it; prefs.keyHeightScale = it })
            GroupDivider()
            SettingChoice(
                "One-handed mode", listOf("0" to "Off", "1" to "Left", "2" to "Right"), oneHanded.toString(),
            ) { oneHanded = it.toInt(); prefs.oneHanded = oneHanded }
        }

        SettingsGroup(
            "Bottom row",
            note = "Choose the keys beside the space bar and their order. Space and Enter always stay.",
        ) {
            bottom.forEachIndexed { i, id ->
                if (i > 0) GroupDivider()
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(BottomKeys.label(id), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    IconButton(enabled = i > 0, onClick = { bottom.add(i - 1, bottom.removeAt(i)); saveBottom() }) {
                        Icon(Icons.Rounded.ArrowUpward, "Move up")
                    }
                    IconButton(enabled = i < bottom.lastIndex, onClick = { bottom.add(i + 1, bottom.removeAt(i)); saveBottom() }) {
                        Icon(Icons.Rounded.ArrowDownward, "Move down")
                    }
                    IconButton(enabled = id !in lockedKeys, onClick = { bottom.removeAt(i); saveBottom() }) {
                        Icon(Icons.Rounded.Close, "Remove")
                    }
                }
            }
            val available = BottomKeys.all.filter { it !in bottom }
            if (available.isNotEmpty()) {
                GroupDivider()
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    available.forEach { id ->
                        AssistChip(
                            onClick = { bottom.add(id); saveBottom() },
                            label = { Text(BottomKeys.label(id)) },
                            leadingIcon = { Icon(Icons.Rounded.Add, null) },
                        )
                    }
                }
            }
            GroupDivider()
            TextButton(
                onClick = { bottom.clear(); bottom.addAll(defaultBottomRow); saveBottom() },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) { Text("Reset to default") }
        }

        SettingsGroup("Touch") {
            SettingToggle("Key preview", "Show a bubble above the pressed key", preview) { preview = it; prefs.keyPreview = it }
            GroupDivider()
            SettingSlider("Long-press delay", longPress, 200f..700f, { "${it.roundToInt()} ms" }, {
                longPress = it; prefs.longPressMs = it.roundToInt()
            })
        }
    }
}

@Composable
fun TypingScreen(onBack: () -> Unit) {
    val prefs = Prefs.get(LocalContext.current)
    var suggestions by remember { mutableStateOf(prefs.suggestionsEnabled) }
    var learn by remember { mutableStateOf(prefs.learnWords) }
    var autoCaps by remember { mutableStateOf(prefs.autoCapitalize) }
    var doubleSpace by remember { mutableStateOf(prefs.doubleSpacePeriod) }
    var clip by remember { mutableStateOf(prefs.clipboardHistory) }

    Page("Typing", onBack) {
        SettingsGroup("Suggestions") {
            SettingToggle("Word suggestions", "Completions and next-word guesses, computed on your phone", suggestions) {
                suggestions = it; prefs.suggestionsEnabled = it
            }
            GroupDivider()
            SettingToggle("Learn from my typing", "Never learns in password fields. Stays on this device.", learn) {
                learn = it; prefs.learnWords = it
            }
        }
        SettingsGroup("Writing") {
            SettingToggle("Auto-capitalise", "Start sentences with a capital letter", autoCaps) { autoCaps = it; prefs.autoCapitalize = it }
            GroupDivider()
            SettingToggle("Double space for period", "Tap space twice to end a sentence", doubleSpace) {
                doubleSpace = it; prefs.doubleSpacePeriod = it
            }
        }
        SettingsGroup("Clipboard", note = "Not recorded in password fields. Open it from the keyboard's Tools menu.") {
            SettingToggle("Clipboard history", "Keeps what you copy while this keyboard is active", clip) {
                clip = it; prefs.clipboardHistory = it
            }
        }
    }
}

@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    val fb = remember { KeyFeedbackImpl(ctx) }
    var sound by remember { mutableStateOf(prefs.soundEnabled) }
    var pack by remember { mutableStateOf(prefs.soundPack) }
    var volume by remember { mutableStateOf(prefs.soundVolume) }
    var vibrate by remember { mutableStateOf(prefs.vibrationEnabled) }
    var strength by remember { mutableStateOf(prefs.vibrationMs.toFloat()) }

    Page("Sound and vibration", onBack) {
        SettingsGroup("Sound", note = "Follows your phone's silent mode.") {
            SettingToggle("Key sound", null, sound) { sound = it; prefs.soundEnabled = it; if (it) fb.preview() }
            if (sound) {
                GroupDivider()
                SettingChoice(
                    "Sound", KeyFeedbackImpl.packs.map { it to it.replaceFirstChar { c -> c.uppercase() } }, pack,
                ) { pack = it; prefs.soundPack = it; fb.preview() }
                GroupDivider()
                SettingSlider("Volume", volume, 0f..1f, { "${(it * 100).roundToInt()}%" }, { volume = it; prefs.soundVolume = it })
                FilledTonalButton(onClick = { fb.preview() }, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Text("Play sample")
                }
            }
        }
        SettingsGroup("Vibration") {
            SettingToggle("Vibrate on key press", null, vibrate) { vibrate = it; prefs.vibrationEnabled = it }
            if (vibrate) {
                GroupDivider()
                SettingSlider("Strength", strength, 4f..40f, { "${it.roundToInt()} ms" }, {
                    strength = it; prefs.vibrationMs = it.roundToInt()
                })
                FilledTonalButton(onClick = { fb.preview() }, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Text("Try vibration")
                }
            }
        }
    }
}
