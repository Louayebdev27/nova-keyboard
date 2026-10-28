@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.novakeys.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.novakeys.keyboard.KeyboardView
import app.novakeys.keyboard.Layouts
import app.novakeys.theme.KeyboardTheme
import app.novakeys.theme.ThemeStore

private val Indigo = Color(0xFF5B5BF0)

@Composable
fun NovaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFF9EA0FF), secondary = Color(0xFF7C83FF))
        else -> lightColorScheme(primary = Indigo, secondary = Color(0xFF4F5BFF))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

// ---- Page frame -------------------------------------------------------------------------------

/**
 * Standard screen: back arrow, title, optional pinned area (e.g. a live keyboard preview that
 * must stay visible while the user changes settings) and a scrolling body of grouped cards.
 */
@Composable
fun Page(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    pinned: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = actions,
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            pinned?.invoke()
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}

// ---- Grouping ---------------------------------------------------------------------------------

/** A titled rounded card that holds related rows. Use [GroupDivider] between rows. */
@Composable
fun SettingsGroup(title: String? = null, note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (title != null) {
            Text(
                title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            )
        }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), content = content)
        }
        if (note != null) {
            Text(
                note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )
        }
    }
}

@Composable
fun GroupDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
fun IconBadge(icon: ImageVector, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size((size * 0.55f).dp))
    }
}

// ---- Rows -------------------------------------------------------------------------------------

@Composable
fun SettingToggle(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun SettingSlider(
    title: String, value: Float, range: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String, onChange: (Float) -> Unit, steps: Int = 0,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(valueLabel(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

/** Segmented control for up to four short options, wrapping chips otherwise. */
@Composable
fun SettingChoice(title: String?, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
        }
        if (options.size <= 4) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, (id, label) ->
                    SegmentedButton(
                        selected = id == selected, onClick = { onSelect(id) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = options.size),
                    ) { Text(label, maxLines = 1) }
                }
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (id, label) ->
                    FilterChip(selected = id == selected, onClick = { onSelect(id) }, label = { Text(label) })
                }
            }
        }
    }
}

@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Spacer(Modifier.height(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---- Keyboard previews ------------------------------------------------------------------------

private val defaultPreviewBottom = listOf("symbols", "emoji", "comma", "space", "period", "enter")

/** A real keyboard view drawn with the given theme. Use where fidelity matters (designer, settings). */
@Composable
fun KeyboardPreview(
    theme: KeyboardTheme,
    modifier: Modifier = Modifier,
    numberRow: Boolean = false,
    heightScale: Float = 1f,
    bottom: List<String> = defaultPreviewBottom,
) {
    val ctx = LocalContext.current
    val store = remember { ThemeStore(ctx) }
    val bitmap: Bitmap? = remember(theme.backgroundImage, theme.id) {
        store.backgroundFile(theme)?.let { runCatching { BitmapFactory.decodeFile(it.path, BitmapFactory.Options().apply { inSampleSize = 2 }) }.getOrNull() }
    }
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        factory = { c ->
            KeyboardView(c).apply {
                showPreview = false
                this.heightScale = heightScale
                setLayout(Layouts.letters("en", numberRow, bottom, null))
                setTheme(theme, bitmap)
            }
        },
        update = {
            it.heightScale = heightScale
            it.setLayout(Layouts.letters("en", numberRow, bottom, null))
            it.setTheme(theme, bitmap)
        },
    )
}

/** Cheap painted thumbnail of a theme, for grids where many previews are on screen at once. */
@Composable
fun MiniKeyboard(theme: KeyboardTheme, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1.7f)) {
        drawRect(Color(theme.background))
        val pad = size.width * 0.045f
        val gap = size.width * 0.02f
        val rows = 4
        val keyH = (size.height - pad * 2 - gap * (rows - 1)) / rows
        val radius = CornerRadius((theme.keyRadiusDp / 24f * keyH * 0.5f + 2f).coerceAtMost(keyH / 2f))
        val key = Color(theme.keyColor).copy(alpha = theme.keyAlpha.coerceIn(0.4f, 1f))
        val special = Color(theme.specialKeyColor).copy(alpha = theme.keyAlpha.coerceIn(0.4f, 1f))
        val accent = Color(theme.accentColor)
        val avail = size.width - pad * 2

        fun rowOf(index: Int, weights: List<Float>, colors: List<Color>, inset: Float = 0f) {
            val total = weights.sum() + inset * 2
            val unit = (avail - gap * (weights.size - 1)) / total
            var x = pad + inset * unit
            val y = pad + index * (keyH + gap)
            weights.forEachIndexed { i, w ->
                drawRoundRect(colors[i], Offset(x, y), Size(w * unit, keyH), radius)
                x += w * unit + gap
            }
        }
        rowOf(0, List(10) { 1f }, List(10) { key })
        rowOf(1, List(9) { 1f }, List(9) { key }, inset = 0.5f)
        rowOf(2, listOf(1.5f) + List(7) { 1f } + listOf(1.5f), listOf(special) + List(7) { key } + listOf(special))
        rowOf(3, listOf(1.4f, 1f, 4f, 1.5f), listOf(special, special, key, accent))
    }
}

@Composable
fun ColorDot(color: Int, size: Int = 28) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color(color))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
}
