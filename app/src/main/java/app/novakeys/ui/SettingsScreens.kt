@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.novakeys.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
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

/** Shared page frame: top bar with a back button, scrolling body. */
@Composable
fun Page(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ Back") } },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            content()
        }
    }
}

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

    Page("Keys and layout", onBack) {
        KeyboardPreview(theme, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), numberRow = numberRow)

        SectionHeader("Languages")
        Layouts.languageNames.forEach { (id, name) ->
            SettingToggle(name, null, id in languages) { on ->
                val next = if (on) (languages + id).distinct() else languages - id
                if (next.isNotEmpty()) { languages = next; prefs.languages = next }
            }
        }
        SettingToggle("Number row", "Keep digits above the letters", numberRow) { numberRow = it; prefs.numberRow = it }

        SectionHeader("Size and position")
        SettingSlider("Key height", height, 0.8f..1.4f, { "${(it * 100).roundToInt()}%" }, { height = it; prefs.keyHeightScale = it })
        SettingChoice("One-handed mode", listOf("0" to "Off", "1" to "Left", "2" to "Right"), oneHanded.toString()) {
            oneHanded = it.toInt(); prefs.oneHanded = oneHanded
        }

        SectionHeader("Bottom row")
        Text(
            "Choose which keys sit next to the space bar and in what order. The space bar always stays.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        bottom.forEachIndexed { i, id ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(BottomKeys.label(id), Modifier.weight(1f))
                TextButton(enabled = i > 0, onClick = { bottom.add(i - 1, bottom.removeAt(i)); saveBottom() }) { Text("↑") }
                TextButton(enabled = i < bottom.lastIndex, onClick = { bottom.add(i + 1, bottom.removeAt(i)); saveBottom() }) { Text("↓") }
                TextButton(enabled = id != "space", onClick = { bottom.removeAt(i); saveBottom() }) { Text("Remove") }
            }
        }
        val available = BottomKeys.all.filter { it !in bottom }
        if (available.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                available.forEach { id ->
                    OutlinedButton(onClick = { bottom.add(id); saveBottom() }) { Text("+ ${BottomKeys.label(id)}") }
                }
            }
        }

        SectionHeader("Touch")
        SettingToggle("Key preview", "Show a bubble above the pressed key", preview) { preview = it; prefs.keyPreview = it }
        SettingSlider("Long-press delay", longPress, 200f..700f, { "${it.roundToInt()} ms" }, {
            longPress = it; prefs.longPressMs = it.roundToInt()
        })
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
        SectionHeader("Suggestions")
        SettingToggle("Word suggestions", "Completions and next-word guesses, computed on your phone", suggestions) {
            suggestions = it; prefs.suggestionsEnabled = it
        }
        SettingToggle("Learn from my typing", "Never learns in password fields. Stays on this device.", learn) {
            learn = it; prefs.learnWords = it
        }
        SectionHeader("Writing")
        SettingToggle("Auto-capitalise", "Start sentences with a capital letter", autoCaps) { autoCaps = it; prefs.autoCapitalize = it }
        SettingToggle("Double space for period", "Tap space twice to end a sentence", doubleSpace) {
            doubleSpace = it; prefs.doubleSpacePeriod = it
        }
        SectionHeader("Clipboard")
        SettingToggle("Clipboard history", "Keeps what you copy while this keyboard is active. Open it from the Tools menu.", clip) {
            clip = it; prefs.clipboardHistory = it
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
        SectionHeader("Sound")
        SettingToggle("Key sound", "Follows your phone's silent mode", sound) { sound = it; prefs.soundEnabled = it; if (it) fb.preview() }
        SettingChoice("Sound", KeyFeedbackImpl.packs.map { it to it.replaceFirstChar { c -> c.uppercase() } }, pack) {
            pack = it; prefs.soundPack = it; fb.preview()
        }
        SettingSlider("Volume", volume, 0f..1f, { "${(it * 100).roundToInt()}%" }, { volume = it; prefs.soundVolume = it })
        OutlinedButton(onClick = { fb.preview() }, Modifier.padding(horizontal = 20.dp)) { Text("Play sample") }

        SectionHeader("Vibration")
        SettingToggle("Vibrate on key press", null, vibrate) { vibrate = it; prefs.vibrationEnabled = it }
        SettingSlider("Strength", strength, 4f..40f, { "${it.roundToInt()} ms" }, {
            strength = it; prefs.vibrationMs = it.roundToInt()
        })
        OutlinedButton(onClick = { fb.preview() }, Modifier.padding(horizontal = 20.dp)) { Text("Try vibration") }
    }
}
