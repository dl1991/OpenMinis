package com.openminis.app.ui.glass

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What is painted behind the glass surfaces. */
enum class BackdropMode { AMBIENT, PHOTO, PLAIN }

data class GlassSettings(
    /** Master switch: off restores the stock layout exactly. */
    val enabled: Boolean = true,
    val mode: BackdropMode = BackdropMode.AMBIENT,
    /** Absolute path of the app-private copy of the chosen photo, if any. */
    val photoPath: String? = null,
    /** 0..1, how strongly the backdrop is dimmed behind ordinary screens. */
    val dim: Float = 0.55f,
)

/**
 * [fork/glass-ui] Persistent settings for the glass UI. Backed by its own
 * SharedPreferences file and mirrored into a StateFlow so every screen
 * re-renders when the user changes the backdrop.
 */
object GlassPrefs {
    private const val FILE = "glass_ui"
    private const val K_ENABLED = "enabled"
    private const val K_MODE = "backdrop_mode"
    private const val K_PHOTO = "photo_path"
    private const val K_DIM = "dim"

    private val state = MutableStateFlow(GlassSettings())
    @Volatile private var loaded = false

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun flow(context: Context): StateFlow<GlassSettings> {
        if (!loaded) synchronized(this) {
            if (!loaded) {
                val p = prefs(context)
                state.value = GlassSettings(
                    enabled = p.getBoolean(K_ENABLED, true),
                    mode = runCatching { BackdropMode.valueOf(p.getString(K_MODE, null) ?: "") }
                        .getOrDefault(BackdropMode.AMBIENT),
                    photoPath = p.getString(K_PHOTO, null)?.takeIf { java.io.File(it).exists() },
                    dim = p.getFloat(K_DIM, 0.55f).coerceIn(0f, 0.9f),
                )
                loaded = true
            }
        }
        return state.asStateFlow()
    }

    fun update(context: Context, transform: (GlassSettings) -> GlassSettings) {
        flow(context)
        val next = transform(state.value)
        state.value = next
        prefs(context).edit()
            .putBoolean(K_ENABLED, next.enabled)
            .putString(K_MODE, next.mode.name)
            .putString(K_PHOTO, next.photoPath)
            .putFloat(K_DIM, next.dim)
            .apply()
    }
}
