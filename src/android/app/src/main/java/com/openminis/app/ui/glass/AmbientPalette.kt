package com.openminis.app.ui.glass

import androidx.compose.ui.graphics.Color

/** Coarse weather buckets; only used to tint the ambient backdrop, never shown. */
enum class WeatherKind { UNKNOWN, CLEAR, CLOUDY, FOG, RAIN, SNOW, STORM }

enum class DayPhase { DAWN, DAY, DUSK, NIGHT }

/**
 * [fork/glass-ui] Pure palette logic for the ambient backdrop: a time-of-day
 * base palette, nudged toward the current weather. Kept free of Android types
 * (except Color) so it is trivially unit-testable.
 */
object AmbientPalette {

    fun phaseFor(hourOfDay: Int): DayPhase = when (hourOfDay) {
        in 5..7 -> DayPhase.DAWN
        in 8..16 -> DayPhase.DAY
        in 17..19 -> DayPhase.DUSK
        else -> DayPhase.NIGHT
    }

    /** WMO weather interpretation code (Open-Meteo `weather_code`) -> bucket. */
    fun kindForWmo(code: Int): WeatherKind = when (code) {
        0, 1 -> WeatherKind.CLEAR
        2, 3 -> WeatherKind.CLOUDY
        45, 48 -> WeatherKind.FOG
        in 51..67, in 80..82 -> WeatherKind.RAIN
        in 71..77, 85, 86 -> WeatherKind.SNOW
        95, 96, 99 -> WeatherKind.STORM
        else -> WeatherKind.UNKNOWN
    }

    private fun base(phase: DayPhase): List<Color> = when (phase) {
        DayPhase.DAWN -> listOf(Color(0xFFFFB199), Color(0xFFF7C8E0), Color(0xFF9FB8F5), Color(0xFFFFE3B3))
        DayPhase.DAY -> listOf(Color(0xFF7FC8F8), Color(0xFFB4E1FF), Color(0xFFA0E7E5), Color(0xFFE6F0FF))
        DayPhase.DUSK -> listOf(Color(0xFFFF8C69), Color(0xFFB06AB3), Color(0xFF4568DC), Color(0xFFFFC371))
        DayPhase.NIGHT -> listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFF3A2D6B), Color(0xFF0F2027))
    }

    /** Colour the weather pulls the palette toward, and how hard. */
    private fun weatherPull(kind: WeatherKind): Pair<Color, Float>? = when (kind) {
        WeatherKind.CLOUDY -> Color(0xFF9AA5B1) to 0.35f
        WeatherKind.FOG -> Color(0xFFC9CED6) to 0.5f
        WeatherKind.RAIN -> Color(0xFF4B6584) to 0.45f
        WeatherKind.SNOW -> Color(0xFFE8F1FA) to 0.5f
        WeatherKind.STORM -> Color(0xFF2C3A47) to 0.6f
        WeatherKind.CLEAR, WeatherKind.UNKNOWN -> null
    }

    fun colors(phase: DayPhase, kind: WeatherKind): List<Color> {
        val pull = weatherPull(kind) ?: return base(phase)
        return base(phase).map { lerp(it, pull.first, pull.second) }
    }

    internal fun lerp(a: Color, b: Color, t: Float): Color = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f,
    )

    /** Whether text over this palette should be light. */
    fun isDark(phase: DayPhase, kind: WeatherKind): Boolean =
        phase == DayPhase.NIGHT || kind == WeatherKind.STORM ||
            (phase == DayPhase.DUSK && kind == WeatherKind.RAIN)
}
