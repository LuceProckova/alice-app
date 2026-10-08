package com.lucyprockova.alice.ui

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import com.lucyprockova.alice.R

/** alice colour tokens, same slots as mockup css variables */
@Immutable
data class AliceColors(
    /** theme name shown on colours screen */
    @StringRes val nameRes: Int,
    val dark: Boolean,
    val header: Color,
    val headerText: Color,
    val headerSub: Color,
    val button: Color,
    val buttonText: Color,
    val spot: Color,
    val kept: Color,
    val missed: Color,
    val card: Color,
    val panel: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val empty: Color,
    val navOn: Color,
) {
    val settled: Color get() = if (dark) panel else Color(0xFFE6E2DB)
    val settledText: Color get() = if (dark) muted.copy(alpha = 0.7f) else Color(0xFFB3ACA2)
    val divider: Color get() = if (dark) line else Color(0xFFCFC6B5)
    val keptText: Color get() = Color(0xFF23321F)
    /** red for delete and other can't-undo actions */
    val danger: Color get() = Color(0xFFC0563F)
    val name: String @Composable get() = stringResource(nameRes)
}

private fun hex(v: String) = Color(v.toColorInt())
private fun isLight(c: Color) = c.luminance() > DARK_LUMINANCE
private val DARK_TEXT = hex("#3b2f28")
private val PALE_TEXT = hex("#fffaf0")

private fun light(name: Int, h: String, t: String, b: String, sp: String, k: String, m: String): AliceColors {
    val header = hex(h); val text = hex(t); val button = hex(b)
    return AliceColors(
        nameRes = name, dark = false, header = header, headerText = text, headerSub = lerp(text, header, 0.4f),
        button = button, buttonText = if (isLight(button)) DARK_TEXT else PALE_TEXT,
        spot = hex(sp), kept = hex(k), missed = hex(m),
        card = hex("#fffefb"), panel = hex("#faf6ec"), ink = hex("#1c1a15"), muted = hex("#6b6459"),
        line = hex("#e7e3d9"), empty = hex("#f2ede2"),
        // pale button too faint for active nav icon on light card, darken it
        navOn = if (isLight(button)) lerp(button, Color.Black, 0.4f) else button,
    )
}

/** dark theme, [v] = header, header text, button, spot, kept, missed, card, panel, ink, muted, line */
private fun dark(name: Int, vararg v: String) = AliceColors(
    nameRes = name, dark = true, header = hex(v[0]), headerText = hex(v[1]), headerSub = hex(v[9]),
    button = hex(v[2]), buttonText = hex("#1b1b1b"), spot = hex(v[3]), kept = hex(v[4]), missed = hex(v[5]),
    card = hex(v[6]), panel = hex(v[7]), ink = hex(v[8]), muted = hex(v[9]), line = hex(v[10]), empty = lerp(hex(v[7]), hex(v[10]), 0.75f), navOn = hex(v[2]),
)

/** ten presets, six light four dark, index 0 default */
val THEMES: List<AliceColors> by lazy {
    listOf(
        light(R.string.core_theme_off_white_honey, "#faf6ec", "#4a3a2c", "#f3c94f", "#fdf2cb", "#9fcb9a", "#eeaa9c"),
        light(R.string.core_theme_vanilla_caramel, "#fbf0d4", "#4b3524", "#d99a55", "#fbe8c8", "#a5cc9d", "#eeae9f"),
        light(R.string.core_theme_sunflower, "#f6d66e", "#3d2e22", "#5c4433", "#fdf3d2", "#a9d0a2", "#efb3a5"),
        light(R.string.core_theme_latte, "#c9a883", "#2f241b", "#fff3c4", "#fbf3da", "#b3d1a8", "#eab7a8"),
        light(R.string.core_theme_cocoa_lemon, "#7a5c45", "#fff9ee", "#fbe89a", "#fdf5d6", "#a6cc9f", "#edb0a2"),
        light(R.string.core_theme_espresso_butter, "#3b2f28", "#fbf5e9", "#f7dc8c", "#fcf1cf", "#a8cfa0", "#eeb1a3"),
        dark(R.string.core_theme_midnight_honey, "#1c1f2b", "#f2ead8", "#f3c94f", "#3a3524", "#6fae7a", "#d9806f", "#14161f", "#232736", "#efe9dc", "#a8a49a", "#30344a"),
        dark(R.string.core_theme_forest_night, "#18241e", "#e8efe0", "#c9d97a", "#2f3a24", "#7fbf86", "#d98a74", "#111a15", "#1e2b24", "#e6ede0", "#9fb0a2", "#2b3a31"),
        dark(R.string.core_theme_plum_dusk, "#2a1f2e", "#f3e6ee", "#e9a8c9", "#3d2a37", "#8cc49a", "#e08a8a", "#1d1520", "#2e2333", "#f1e6ee", "#b3a2b0", "#3d3042"),
        dark(R.string.core_theme_charcoal_coral, "#222222", "#f4f1ec", "#f08c6c", "#3a2c27", "#79b483", "#e4766a", "#171717", "#262626", "#f2efea", "#a9a6a0", "#353535"),
    )
}

val LocalAliceColors = staticCompositionLocalOf { THEMES[0] }

/** wraps every screen, material 3 scheme plus alice colour tokens */
@Composable
fun AliceTheme(theme: Int = 0, content: @Composable () -> Unit) {
    val c = THEMES.getOrElse(theme) { THEMES[0] }
    val scheme = if (c.dark) darkColorScheme(
        primary = c.button, onPrimary = c.buttonText, secondary = c.headerText, background = c.card, surface = c.panel,
        onBackground = c.ink, onSurface = c.ink, surfaceVariant = c.panel, onSurfaceVariant = c.muted, outline = c.line,
        surfaceContainerHigh = c.panel, surfaceContainerHighest = c.panel, surfaceContainer = c.panel,
    ) else lightColorScheme(
        primary = c.button, onPrimary = c.buttonText, secondary = c.headerText, background = c.card, surface = c.card,
        onBackground = c.ink, onSurface = c.ink, surfaceVariant = c.empty, onSurfaceVariant = c.muted, outline = c.line,
        surfaceContainerHigh = c.card, surfaceContainerHighest = c.panel, surfaceContainer = c.card,
    )
    CompositionLocalProvider(LocalAliceColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** short way in, Alice.colors like MaterialTheme.colorScheme */
object Alice {
    val colors: AliceColors
        @Composable get() = LocalAliceColors.current
}
