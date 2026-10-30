package com.openminis.app.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Top-level pages reached from the floating tab bar. */
enum class GlassTab(val labelRes: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    CHATS(R.string.glass_tab_chats, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    FEED(R.string.glass_tab_feed, Icons.Outlined.DynamicFeed, Icons.Filled.DynamicFeed),
    IDEAS(R.string.glass_tab_ideas, Icons.Outlined.Lightbulb, Icons.Filled.Lightbulb),
    BOTS(R.string.glass_tab_bots, Icons.Outlined.SmartToy, Icons.Filled.SmartToy),
    SETTINGS(R.string.glass_tab_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
}

/** Height the floating bar occupies above the navigation-bar inset. */
val GlassTabBarHeight: Dp = 64.dp
val GlassTabBarBottomGap: Dp = 12.dp

/**
 * Bottom space a page under the bar should leave (content padding for lists
 * that scroll beneath the glass, or plain padding for foreign screens).
 */
val LocalGlassBottomInset = compositionLocalOf { 0.dp }

/**
 * [fork/glass-ui] Hosts one tab page: backdrop + page as the Haze source, and
 * the floating glass pill above it. [scrollUnder] = true lets the page draw
 * beneath the bar (it must honour [LocalGlassBottomInset] as content
 * padding); false pads the page instead, for stock screens with their own
 * bottom FABs.
 */
@Composable
fun GlassTabShell(
    current: GlassTab,
    onSelect: (GlassTab) -> Unit,
    scrollUnder: Boolean,
    showBackdrop: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze = rememberHazeState()
    val dark = GlassStyles.isDarkTheme()
    val inset = GlassTabBarHeight + GlassTabBarBottomGap + 8.dp
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            if (showBackdrop) GlassScreenBackdrop()
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (scrollUnder) Modifier
                        else Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = inset),
                    ),
            ) {
                CompositionLocalProvider(LocalGlassBottomInset provides if (scrollUnder) inset else 0.dp) {
                    content()
                }
            }
        }
        GlassTabBar(
            current = current,
            onSelect = onSelect,
            dark = dark,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = GlassTabBarBottomGap, start = 20.dp, end = 20.dp),
            haze = haze,
        )
    }
}

/** The user's backdrop, dimmed toward the theme background for legibility. */
@Composable
fun GlassScreenBackdrop() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by remember(context) { GlassPrefs.flow(context) }.collectAsState()
    Box(Modifier.fillMaxSize()) {
        GlassBackdrop(settings = settings)
        if (settings.mode != BackdropMode.PLAIN) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = settings.dim)))
        }
    }
}

@Composable
private fun GlassTabBar(
    current: GlassTab,
    onSelect: (GlassTab) -> Unit,
    dark: Boolean,
    haze: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    val fg = if (dark) Color.White else Color(0xFF15181E)
    val accent = MaterialTheme.colorScheme.primary
    val edge = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.28f else 0.85f), Color.White.copy(alpha = if (dark) 0.06f else 0.25f)),
    )
    Row(
        modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .height(GlassTabBarHeight)
            .shadow(18.dp, shape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(shape)
            .hazeEffect(state = haze, style = GlassStyles.bar(dark))
            .border(0.8.dp, edge, shape)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassTab.entries.forEach { tab ->
            val selected = tab == current
            val label = stringResource(tab.labelRes)
            val pill by animateColorAsState(
                if (selected) (if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.06f)) else Color.Transparent,
                label = "tabPill",
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(pill)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                    ) { onSelect(tab) }
                    .semantics {
                        this.selected = selected
                        contentDescription = label
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    if (selected) tab.selectedIcon else tab.icon,
                    contentDescription = null,
                    tint = if (selected) accent else fg.copy(alpha = 0.72f),
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    label,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = if (selected) accent else fg.copy(alpha = 0.72f),
                )
            }
        }
    }
}
