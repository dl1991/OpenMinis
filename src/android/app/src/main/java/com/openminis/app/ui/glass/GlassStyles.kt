package com.openminis.app.ui.glass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * [fork/glass-ui] Haze styles for the glass surfaces. Haze blurs with
 * RenderEffect on API 31+; below that `blurEnabled` defaults to false and it
 * paints [HazeStyle.fallbackTint] instead, so the fallback tints are nearly
 * opaque to keep text legible over an un-blurred backdrop.
 */
object GlassStyles {
    private fun tint(style: GlassStyle) =
        if (style == GlassStyle.DUSK) GlassPalette.DuskTint else GlassPalette.WarmTint

    /** Docked bottom bar: subtle translucent fill. */
    @Composable
    @ReadOnlyComposable
    fun bar(style: GlassStyle): HazeStyle = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tints = listOf(HazeTint(tint(style).copy(alpha = if (style == GlassStyle.DUSK) 0.58f else 0.70f))),
        blurRadius = 24.dp,
        noiseFactor = 0.04f,
        fallbackTint = HazeTint(tint(style).copy(alpha = 0.95f)),
    )

    /** Sidebar sheet: a little denser than the bar so long lists stay readable. */
    @Composable
    @ReadOnlyComposable
    fun sheet(style: GlassStyle): HazeStyle = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tints = listOf(HazeTint(tint(style).copy(alpha = if (style == GlassStyle.DUSK) 0.66f else 0.76f))),
        blurRadius = 32.dp,
        noiseFactor = 0.04f,
        fallbackTint = HazeTint(tint(style).copy(alpha = 0.97f)),
    )

    /** Hairline colour for borders/dividers on glass. */
    fun hairline(style: GlassStyle): Color =
        if (style == GlassStyle.DUSK) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
}
