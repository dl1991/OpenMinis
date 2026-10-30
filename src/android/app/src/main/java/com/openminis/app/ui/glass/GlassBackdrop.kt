package com.openminis.app.ui.glass

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

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
                AmbientGradient(modifier, settings.style)
            }
        }
        BackdropMode.AMBIENT -> AmbientGradient(modifier, settings.style)
        BackdropMode.PLAIN -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    }
}

@Composable
private fun AmbientGradient(modifier: Modifier, style: GlassStyle) {
    val weather = rememberWeather()
    // Re-read the hour once a minute; the palette only changes on the hour.
    var hour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    val colors = remember(hour, weather, style) {
        AmbientPalette.styled(AmbientPalette.colors(AmbientPalette.phaseFor(hour), weather), style)
    }
    AmbientFog(colors, modifier)
}
