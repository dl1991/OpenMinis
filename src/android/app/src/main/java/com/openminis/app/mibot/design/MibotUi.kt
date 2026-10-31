package com.openminis.app.mibot.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.ui.theme.LocalMibotDark

/**
 * [fork/mibot-v2] The Mibot design system: one set of tokens (colours, radius,
 * hairlines) and the handful of building blocks every Mibot page is made of —
 * top bar, frosted card, list row with an icon chip, apps grid, status chip,
 * buttons. Colours come from MaterialTheme (MinisTheme(mibot = true)), so the
 * same components read correctly in the light cloud-sky and the night variant.
 */
object MibotTokens {
    val CardRadius: Dp = 22.dp
    val RowRadius: Dp = 16.dp
    val ScreenPadding: Dp = 16.dp
    val CardShape = RoundedCornerShape(22.dp)

    // Icon chip colours (apps grid / rows), tuned to sit on both variants.
    val Slate = Color(0xFF3E6A8C)
    val Sky = Color(0xFF4A87C9)
    val Brick = Color(0xFFC4553B)
    val Green = Color(0xFF3E8E5E)
    val Purple = Color(0xFF7B5BB5)
    val Red = Color(0xFFD0483C)
    val Teal = Color(0xFF2E9B8A)
    val Amber = Color(0xFFC99323)
    val Taupe = Color(0xFF7A6A60)
    val Rose = Color(0xFFC0504D)
    val Navy = Color(0xFF2F5E86)
    val Steel = Color(0xFF5F6E82)

    val Ok = Color(0xFF3E9B63)
    val Warn = Color(0xFFD08B2C)
    val Err = Color(0xFFD0483C)

    @Composable
    @ReadOnlyComposable
    fun hairline(): Color =
        if (LocalMibotDark.current) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)

    @Composable
    @ReadOnlyComposable
    fun card(): Color = MaterialTheme.colorScheme.surfaceContainerLow

    /** Brick-red accent (salmon on the night variant). */
    @Composable
    @ReadOnlyComposable
    fun accent(): Color = MaterialTheme.colorScheme.primary
}

/** Page frame: transparent scaffold over the shared backdrop + Mibot top bar. */
@Composable
fun MibotScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    scroll: Boolean = true,
    bottomPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = { MibotTopBar(title, onBack, actions) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = bottomPadding),
            content = content,
        )
    }
}

/** Centered title with a round frosted back button (reference style). */
@Composable
fun MibotTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(64.dp)
            .padding(horizontal = 12.dp),
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MibotTokens.card())
                    .border(1.dp, MibotTokens.hairline(), CircleShape)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            title,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Large left-aligned page title for top-level tabs. */
@Composable
fun MibotPageTitle(title: String, subtitle: String? = null) {
    Column(
        Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
    ) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Frosted rounded card with a hairline border. */
@Composable
fun MibotCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = MibotTokens.ScreenPadding)
            .clip(MibotTokens.CardShape)
            .background(MibotTokens.card())
            .border(1.dp, MibotTokens.hairline(), MibotTokens.CardShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Section label above a card, with an optional trailing action ("管理 ›"). */
@Composable
fun MibotSectionHeader(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 28.dp, end = 24.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                "$actionLabel ›",
                modifier = Modifier.clickable(onClick = onAction),
                fontSize = 14.sp,
                color = MibotTokens.accent(),
            )
        }
    }
}

/**
 * Icon chip. [filled] = solid coloured circle with a white glyph (apps grid);
 * otherwise a soft tinted round-square with a coloured glyph (list rows).
 */
@Composable
fun IconChip(icon: ImageVector, color: Color, size: Dp = 40.dp, filled: Boolean = false) {
    val shape = if (filled) CircleShape else RoundedCornerShape(size * 0.34f)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(if (filled) color else color.copy(alpha = if (LocalMibotDark.current) 0.26f else 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (filled) Color.White else if (LocalMibotDark.current) lighten(color) else color,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

private fun lighten(c: Color): Color = Color(
    red = c.red + (1f - c.red) * 0.35f,
    green = c.green + (1f - c.green) * 0.35f,
    blue = c.blue + (1f - c.blue) * 0.35f,
    alpha = 1f,
)

/** Standard list row inside a [MibotCard]. */
@Composable
fun MibotRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    color: Color = MibotTokens.Brick,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    chevron: Boolean = onClick != null,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconChip(icon, color)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrEmpty()) {
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
            if (chevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 70.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = MibotTokens.hairline(),
            )
        }
    }
}

data class MibotGridItem(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

/** 4-column apps grid of coloured circular icons (reference "应用" panel). */
@Composable
fun MibotGrid(items: List<MibotGridItem>, columns: Int = 4) {
    MibotCard(padding = PaddingValues(vertical = 14.dp, horizontal = 6.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = item.onClick)
                            .padding(vertical = 10.dp)
                            .semantics { contentDescription = item.label },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        IconChip(item.icon, item.color, size = 52.dp, filled = true)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            item.label,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

enum class ChipKind { OK, WARN, ERR, NEUTRAL, ACCENT }

/** Small rounded status chip (可用 / 冷却中 / 失效 / 等你 / 完成 / 出错). */
@Composable
fun StatusChip(text: String, kind: ChipKind) {
    val accent = MibotTokens.accent()
    val c = when (kind) {
        ChipKind.OK -> MibotTokens.Ok
        ChipKind.WARN -> MibotTokens.Warn
        ChipKind.ERR -> MibotTokens.Err
        ChipKind.ACCENT -> accent
        ChipKind.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dark = LocalMibotDark.current
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = if (dark) 0.24f else 0.14f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = if (dark) lighten(c) else c,
        maxLines = 1,
    )
}

/** Filled brick-red capsule button. */
@Composable
fun MibotButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, tonal: Boolean = false) {
    val accent = MibotTokens.accent()
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        tonal -> accent.copy(alpha = if (LocalMibotDark.current) 0.24f else 0.13f)
        else -> accent
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        tonal -> if (LocalMibotDark.current) lighten(accent) else accent
        else -> MaterialTheme.colorScheme.onPrimary
    }
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Plain caption text under a card. */
@Composable
fun MibotFootnote(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 17.sp,
    )
}

@Composable
fun MibotSpacer(h: Dp = 12.dp) = Spacer(Modifier.height(h))

/** Horizontal filter chips row (categories). */
@Composable
fun MibotFilterChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScrollCompat()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val sel = opt == selected
            Text(
                opt,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (sel) MibotTokens.accent() else MibotTokens.card())
                    .border(1.dp, if (sel) Color.Transparent else MibotTokens.hairline(), RoundedCornerShape(50))
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun Modifier.horizontalScrollCompat(): Modifier =
    this.then(Modifier.horizontalScroll(rememberScrollState()))
