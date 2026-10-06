package com.example.cet6vocabulary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import kotlinx.coroutines.delay

/**
 * Page enter: fade in while rising a few dp. Keyed by destination, so it replays on navigation
 * and never on in-page state changes such as a list reload.
 */
@Composable
fun AnimatedPageContent(
    pageKey: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember(pageKey, reduceMotion) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(progress) {
        if (!reduceMotion) progress.animateTo(targetValue = 1f, animationSpec = MaoDanMotion.pageEnter())
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * MaoDanMotion.PageEnterOffset.toPx()
        }
    ) { content() }
}

/**
 * One-shot entrance for a single block (a hero card, a result panel). Plays whenever the block
 * enters the composition; [delayMillis] allows a small stagger behind the page itself.
 */
@Composable
fun AnimatedEnterBox(
    modifier: Modifier = Modifier,
    delayMillis: Int = MaoDanMotion.CardEnterDelayMillis,
    offset: Dp = MaoDanMotion.CardEnterOffset,
    content: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember(reduceMotion) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(progress, delayMillis) {
        if (reduceMotion) return@LaunchedEffect
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(targetValue = 1f, animationSpec = MaoDanMotion.cardEnter())
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * offset.toPx()
        }
    ) { content() }
}

/**
 * Progress values share one spec across screens, so a bar fills the same way on home, study,
 * spelling and mine. It snaps instead of tweening when animation is turned off for that bar or
 * the system asks for less motion; the final value is identical either way.
 */
@Composable
fun rememberAnimatedProgress(target: Float, animate: Boolean = true): Float {
    val reduceMotion = LocalReduceMotion.current
    val spec = remember(reduceMotion, animate) {
        if (reduceMotion || !animate) snap<Float>() else MaoDanMotion.progress<Float>()
    }
    val progress by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = spec,
        label = "progress"
    )
    return progress
}

/**
 * Press scale driven by an existing [MutableInteractionSource], so ripple, enabled state and
 * semantics stay exactly as the underlying component provides them. Press down quickly, then
 * release through a slight overshoot back to rest.
 */
@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = MaoDanMotion.ButtonPressedScale,
    enabled: Boolean = true
): Float {
    val reduceMotion = LocalReduceMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val animate = enabled && !reduceMotion
    // Hoisted so a recomposition neither re-allocates the specs nor restarts a running animation.
    val pressSpec = remember { MaoDanMotion.pressDown() }
    val releaseSpec = remember { MaoDanMotion.pressRelease() }
    val idleSpec = remember { snap<Float>() }
    val scale by animateFloatAsState(
        targetValue = if (pressed && animate) pressedScale else 1f,
        animationSpec = when {
            !animate -> idleSpec
            pressed -> pressSpec
            else -> releaseSpec
        },
        label = "pressScale"
    )
    return scale
}

/** Applies an already-animated press scale without touching layout or hit testing. */
fun Modifier.pressScale(scale: Float): Modifier = graphicsLayer {
    scaleX = scale
    scaleY = scale
}
