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
    /** Floating pill tab bar (iOS 26 "Liquid Glass"-like, light material). */
    @Composable
    @ReadOnlyComposable
    fun bar(dark: Boolean): HazeStyle {
        val base = if (dark) Color(0xFF1C1F26) else Color(0xFFF7F8FA)
        return HazeStyle(
            backgroundColor = MaterialTheme.colorScheme.surface,
            tints = listOf(HazeTint(base.copy(alpha = if (dark) 0.50f else 0.55f))),
            blurRadius = 22.dp,
            noiseFactor = 0.06f,
            fallbackTint = HazeTint(base.copy(alpha = 0.94f)),
        )
    }

    /** Sidebar sheet: a little denser than the bar so long lists stay readable. */
    @Composable
    @ReadOnlyComposable
    fun sheet(dark: Boolean): HazeStyle {
        val base = if (dark) Color(0xFF15181E) else Color(0xFFFBFBFC)
        return HazeStyle(
            backgroundColor = MaterialTheme.colorScheme.surface,
            tints = listOf(HazeTint(base.copy(alpha = if (dark) 0.62f else 0.68f))),
            blurRadius = 32.dp,
            noiseFactor = 0.05f,
            fallbackTint = HazeTint(base.copy(alpha = 0.97f)),
        )
    }

    /** Is the app currently drawn dark (drives glass tint + foreground)? */
    @Composable
    @ReadOnlyComposable
    fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
}
