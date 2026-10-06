package com.example.cet6vocabulary.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.ui.theme.CET6VocabularyTheme
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import com.example.cet6vocabulary.ui.theme.MaoDanShapes
import com.example.cet6vocabulary.ui.theme.MaoDanTextTertiary

// Geometry carried over verbatim from the previous floating bar, so the glass keeps exactly the
// height, side margins and gap above the navigation inset it had before. Named here rather than
// left inline: the whole point of the layer is that its footprint does not change.
private val BarSideMargin = 16.dp
private val BarBottomGap = 8.dp
private val BarContentPaddingHorizontal = 8.dp
private val BarContentPaddingVertical = 6.dp
private val BarGlassEdgeWidth = 1.dp
private val ItemMinSize: Dp = 48.dp
private val ItemVerticalPadding = 2.dp
private val ItemLabelGap = 2.dp
private val IconSlotPaddingHorizontal = 11.dp
private val IconSlotPaddingVertical = 6.dp
private val NavigationIconSize = 24.dp

/**
 * The bottom navigation as a sheet of liquid glass: a translucent surface that lets the aurora
 * underneath read through, one glass droplet marking the selected tab, and the five icons and
 * labels sitting on top of both.
 *
 * Only the droplet moves. The glass is painted once per measure and the droplet repaints only
 * while the selection travels, so an idle bar schedules no frames of its own and adds nothing to
 * what the aurora and particle layers already cost. Selection is driven purely by
 * [selectedIndex]: routing, back handling and the tab contract stay with the caller.
 */
@Composable
fun MaoDanLiquidGlassBottomBar(
    items: List<CET6NavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val reduceMotion = LocalReduceMotion.current
    val colorScheme = MaterialTheme.colorScheme

    // One droplet for the whole bar, tracking the selection as a continuous index. A rapid series
    // of taps retargets the same animation mid-flight instead of stacking or restarting one, so
    // there is never a second droplet and never a jump back to an earlier tab.
    val capsule = remember { Animatable(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex, reduceMotion) {
        if (reduceMotion) capsule.snapTo(selectedIndex.toFloat())
        else capsule.animateTo(selectedIndex.toFloat(), MaoDanMotion.liquidGlassSelection())
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = BarSideMargin, end = BarSideMargin, bottom = BarBottomGap),
        color = Color.Transparent,
        contentColor = colorScheme.onSurface,
        shape = MaoDanShapes.extraLarge,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.fillMaxWidth()) {
            // Layers 1-3: glass body, top highlight, hairline rim. Surface clips its children to
            // the rounded shape, so all three stay inside the bar without a second clip.
            Box(
                Modifier
                    .matchParentSize()
                    .liquidGlassSurface(
                        bodyTop = colorScheme.surface.copy(alpha = MaoDanMotion.LiquidGlassSurfaceTopAlpha),
                        bodyBottom = colorScheme.surface.copy(
                            alpha = if (darkTheme) MaoDanMotion.LiquidGlassDarkSurfaceBottomAlpha
                            else MaoDanMotion.LiquidGlassLightSurfaceBottomAlpha
                        ),
                        highlight = Color.White.copy(
                            alpha = if (darkTheme) MaoDanMotion.LiquidGlassDarkHighlightAlpha
                            else MaoDanMotion.LiquidGlassLightHighlightAlpha
                        ),
                        edge = Color.White.copy(
                            alpha = if (darkTheme) MaoDanMotion.LiquidGlassDarkEdgeAlpha
                            else MaoDanMotion.LiquidGlassLightEdgeAlpha
                        )
                    )
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BarContentPaddingHorizontal,
                        vertical = BarContentPaddingVertical
                    )
            ) {
                // Layer 4: the droplet, painted under the tabs so ripple and content stay on top.
                Box(
                    Modifier
                        .matchParentSize()
                        .liquidGlassCapsule(
                            progress = capsule,
                            itemCount = items.size,
                            tint = colorScheme.primaryContainer,
                            tintAlpha = if (darkTheme) MaoDanMotion.LiquidGlassDarkCapsuleAlpha
                            else MaoDanMotion.LiquidGlassLightCapsuleAlpha,
                            sheen = Color.White.copy(
                                alpha = if (darkTheme) MaoDanMotion.LiquidGlassDarkCapsuleSheenAlpha
                                else MaoDanMotion.LiquidGlassLightCapsuleSheenAlpha
                            ),
                            edge = Color.White.copy(
                                alpha = if (darkTheme) MaoDanMotion.LiquidGlassDarkCapsuleEdgeAlpha
                                else MaoDanMotion.LiquidGlassLightCapsuleEdgeAlpha
                            )
                        )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        // Layers 5-6. Equal weights keep the five slot centres fixed, which is what
                        // the droplet's index arithmetic and the touch targets both rely on.
                        LiquidGlassNavigationItem(
                            item = item,
                            selected = selectedIndex == index,
                            onClick = { onItemSelected(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Paints the static glass: a body gradient thin enough for the background light to show through,
 * a highlight that dies out over the top third, and a rim that fades before it can read as an
 * outline. Everything is built in the cache block, so a repaint of this layer costs one rect.
 */
private fun Modifier.liquidGlassSurface(
    bodyTop: Color,
    bodyBottom: Color,
    highlight: Color,
    edge: Color
): Modifier = drawWithCache {
    val body = Brush.verticalGradient(
        colorStops = arrayOf(0f to bodyTop, 1f to bodyBottom),
        startY = 0f,
        endY = size.height
    )
    val sheen = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to highlight,
            MaoDanMotion.LiquidGlassHighlightHeightRatio to Color.Transparent
        ),
        startY = 0f,
        endY = size.height
    )
    val rim = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to edge,
            MaoDanMotion.LiquidGlassEdgeFadeStop to edge.copy(alpha = edge.alpha * 0.25f),
            1f to Color.Transparent
        ),
        startY = 0f,
        endY = size.height
    )
    val edgeWidth = BarGlassEdgeWidth.toPx()
    // Inset by half the stroke so the hairline sits inside the surface instead of straddling the
    // clip, and taken from the same shape the Surface clips with.
    val rimPath = Path().apply {
        addOutline(
            MaoDanShapes.extraLarge.createOutline(
                size = Size(size.width - edgeWidth, size.height - edgeWidth),
                layoutDirection = layoutDirection,
                density = this@drawWithCache
            )
        )
    }
    onDrawBehind {
        drawRect(body)
        drawRect(sheen)
        withTransform({ translate(edgeWidth / 2f, edgeWidth / 2f) }) {
            drawPath(rimPath, brush = rim, style = Stroke(width = edgeWidth))
        }
    }
}

/**
 * The selected droplet. Its position is a continuous index rather than a slot, so it travels
 * between tabs; [Animatable.velocity] supplies the liquid part, stretching the bead along its
 * travel and thinning it a little so the area stays roughly constant before both relax at rest.
 *
 * Only the draw phase reads the animated values, and the brushes are cached, so a slide repaints
 * one rounded rect per frame without rebuilding a single gradient.
 */
private fun Modifier.liquidGlassCapsule(
    progress: Animatable<Float, *>,
    itemCount: Int,
    tint: Color,
    tintAlpha: Float,
    sheen: Color,
    edge: Color
): Modifier = drawWithCache {
    val slotWidth = if (itemCount > 0) size.width / itemCount else size.width
    val inset = MaoDanMotion.LiquidGlassCapsuleInset.toPx()
    val beadWidth = (slotWidth - inset * 2f).coerceAtLeast(0f)
    val beadHeight = (size.height - inset * 2f).coerceAtLeast(0f)
    val corner = CornerRadius(MaoDanMotion.LiquidGlassCapsuleCorner.toPx())
    val edgeWidth = BarGlassEdgeWidth.toPx()
    val body = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to tint.copy(alpha = tintAlpha),
            1f to tint.copy(alpha = tintAlpha * MaoDanMotion.LiquidGlassCapsuleBottomFactor)
        ),
        startY = 0f,
        endY = beadHeight
    )
    val sheenBrush = Brush.verticalGradient(
        colorStops = arrayOf(0f to sheen, MaoDanMotion.LiquidGlassCapsuleSheenStop to Color.Transparent),
        startY = 0f,
        endY = beadHeight
    )
    val rimBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to edge,
            MaoDanMotion.LiquidGlassEdgeFadeStop to edge.copy(alpha = edge.alpha * 0.3f),
            1f to Color.Transparent
        ),
        startY = 0f,
        endY = beadHeight
    )
    onDrawBehind {
        if (itemCount <= 0 || beadWidth <= 0f || beadHeight <= 0f) return@onDrawBehind
        val index = progress.value
        val stretch = (progress.velocity / MaoDanMotion.LiquidGlassStretchVelocityDivisor)
            .coerceIn(-MaoDanMotion.LiquidGlassMaxStretch, MaoDanMotion.LiquidGlassMaxStretch)
        val width = beadWidth * (1f + stretch)
        val height = beadHeight * (1f - stretch * MaoDanMotion.LiquidGlassStretchHeightFactor)
        val centerX = inset + slotWidth / 2f + index * slotWidth
        withTransform({ translate(centerX - width / 2f, inset + (beadHeight - height) / 2f) }) {
            val bead = Size(width, height)
            drawRoundRect(brush = body, size = bead, cornerRadius = corner)
            drawRoundRect(brush = sheenBrush, size = bead, cornerRadius = corner)
            drawRoundRect(
                brush = rimBrush,
                topLeft = Offset(edgeWidth / 2f, edgeWidth / 2f),
                size = Size(width - edgeWidth, height - edgeWidth),
                cornerRadius = corner,
                style = Stroke(width = edgeWidth)
            )
        }
    }
}

/**
 * One tab. Layout, touch target, ripple and semantics are the previous item's: a fixed-weight
 * slot, an always-present label so the five centres never move, and the icon slot padding kept as
 * empty space now that the droplet is drawn by the layer underneath.
 */
@Composable
private fun LiquidGlassNavigationItem(
    item: CET6NavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selected
    val reduceMotion = LocalReduceMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(
        interactionSource = interactionSource,
        pressedScale = MaoDanMotion.NavigationPressedScale
    )
    val transition = updateTransition(targetState = selected, label = "liquid glass navigation item")

    // Scale, alpha and the small label rise share one spring so they land together; colour keeps a
    // tween that matches the droplet's travel, because a spring on colour reads as a flicker. Under
    // reduced motion only colour and alpha change, which is enough to tell the selected tab apart.
    val motionSpec = remember(reduceMotion) {
        if (reduceMotion) MaoDanMotion.fast<Float>() else MaoDanMotion.stateSpring<Float>()
    }
    val colorSpec = remember(reduceMotion) {
        if (reduceMotion) MaoDanMotion.fast<Color>() else MaoDanMotion.liquidGlassColor<Color>()
    }
    val selectedIconScale = if (reduceMotion) 1f else MaoDanMotion.LiquidGlassSelectedIconScale
    val unselectedIconScale = if (reduceMotion) 1f else MaoDanMotion.NavigationUnselectedIconScale
    val labelRisePx = with(LocalDensity.current) { MaoDanMotion.NavigationSelectedLabelRise.toPx() }

    val iconScale by transition.animateFloat(
        transitionSpec = { motionSpec },
        label = "icon scale"
    ) { target -> if (target) selectedIconScale else unselectedIconScale }
    val iconAlpha by transition.animateFloat(
        transitionSpec = { motionSpec },
        label = "icon alpha"
    ) { target -> if (target) 1f else MaoDanMotion.NavigationUnselectedIconAlpha }
    val labelAlpha by transition.animateFloat(
        transitionSpec = { motionSpec },
        label = "label alpha"
    ) { target -> if (target) 1f else MaoDanMotion.NavigationUnselectedLabelAlpha }
    val labelRise by transition.animateFloat(
        transitionSpec = { motionSpec },
        label = "label rise"
    ) { target -> if (target) 0f else if (reduceMotion) 0f else labelRisePx }
    val contentColor by transition.animateColor(
        transitionSpec = { colorSpec },
        label = "content color"
    ) { target -> if (target) MaterialTheme.colorScheme.primary else MaoDanTextTertiary }
    // Weight, not size: labelMedium pins its own line height, so the bar never grows on selection.
    val labelWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium

    Box(
        modifier = modifier
            .pressScale(pressScale)
            .defaultMinSize(minWidth = ItemMinSize, minHeight = ItemMinSize)
            .clip(MaoDanShapes.medium)
            // selectable, not clickable: it owns the Tab role and the Selected state in one place,
            // merges the icon's content description and the label into this node for TalkBack, and
            // still drives the same interaction source, so ripple and press feedback are unchanged.
            .selectable(
                selected = isSelected,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = ItemVerticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ItemLabelGap)
        ) {
            Box(
                modifier = Modifier.padding(
                    horizontal = IconSlotPaddingHorizontal,
                    vertical = IconSlotPaddingVertical
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier
                        .size(NavigationIconSize)
                        .graphicsLayer {
                            alpha = iconAlpha
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    tint = contentColor
                )
            }
            Text(
                text = item.label,
                modifier = Modifier.graphicsLayer {
                    alpha = labelAlpha
                    translationY = labelRise
                },
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = labelWeight,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FC, widthDp = 360, heightDp = 140)
@Composable
private fun MaoDanLiquidGlassBottomBarPreview() {
    CET6VocabularyTheme {
        MaoDanLiquidGlassBottomBar(
            items = listOf(
                CET6NavigationItem("首页", LucideNavigationIcons.Home),
                CET6NavigationItem("单词", LucideNavigationIcons.Word),
                CET6NavigationItem("背诵", LucideNavigationIcons.Study),
                CET6NavigationItem("拼写", LucideNavigationIcons.Spell),
                CET6NavigationItem("我的", LucideNavigationIcons.Profile)
            ),
            selectedIndex = 2,
            onItemSelected = {}
        )
    }
}
