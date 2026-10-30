package com.openminis.app.ui.glass

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Wall-clock driven animation time in ms. Ticks at ~4 fps and ONLY while the
 * host lifecycle is RESUMED, so the ambient backdrop costs nothing in the
 * background and very little in the foreground (the drift is slow enough that
 * 4 fps reads as continuous). Being wall-clock based, every backdrop instance
 * on screen (main + blurred copy inside the glass panel) stays in sync.
 */
@Composable
fun rememberAmbientClock(animate: Boolean): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, animate) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                now = System.currentTimeMillis()
                if (!animate) break
                delay(250)
            }
        }
    }
    return now
}

@Composable
private fun rememberWeather(): WeatherKind {
    val context = LocalContext.current
    val weather by produceState(WeatherKind.UNKNOWN) {
        while (true) {
            value = WeatherSource.current(context)
            delay(30L * 60 * 1000)
        }
    }
    return weather
}

@Composable
private fun rememberPhotoBitmap(path: String?): ImageBitmap? {
    val bmp by produceState<ImageBitmap?>(null, path) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }
    return bmp
}

/**
 * [fork/glass-ui] Full-bleed backdrop: the user's photo, the ambient gradient,
 * or the plain theme background. Glass surfaces blur it (and whatever is
 * drawn above it) through Haze; see GlassStyles.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    settings: GlassSettings = LocalContext.current.let { ctx ->
        remember(ctx) { GlassPrefs.flow(ctx) }.collectAsState().value
    },
) {
    when (settings.mode) {
        BackdropMode.PHOTO -> {
            val bmp = rememberPhotoBitmap(settings.photoPath)
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.fillMaxSize(),
                )
            } else {
                AmbientGradient(modifier)
            }
        }
        BackdropMode.AMBIENT -> AmbientGradient(modifier)
        BackdropMode.PLAIN -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    }
}

@Composable
private fun AmbientGradient(modifier: Modifier) {
    val now = rememberAmbientClock(animate = true)
    val weather = rememberWeather()
    val hour = remember(now / 60_000) { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val colors = remember(hour, weather) {
        AmbientPalette.colors(AmbientPalette.phaseFor(hour), weather)
    }
    // One full drift cycle every 90 s; blobs move on offset phases.
    val t = (now % 90_000L) / 90_000f * 2f * PI.toFloat()
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(listOf(colors[0], colors[3]), Offset.Zero, Offset(size.width, size.height)))
        val r = size.maxDimension * 0.6f
        fun blob(c: Color, cx: Float, cy: Float) = drawCircle(
            brush = Brush.radialGradient(listOf(c.copy(alpha = 0.85f), c.copy(alpha = 0f)), Offset(cx, cy), r),
            radius = r,
            center = Offset(cx, cy),
        )
        blob(colors[1], size.width * (0.25f + 0.15f * cos(t)), size.height * (0.25f + 0.10f * sin(t)))
        blob(colors[2], size.width * (0.80f + 0.12f * sin(t + 2f)), size.height * (0.55f + 0.12f * cos(t + 1f)))
        blob(colors[0], size.width * (0.35f + 0.18f * sin(t + 4f)), size.height * (0.90f + 0.08f * cos(t + 3f)))
    }
}
