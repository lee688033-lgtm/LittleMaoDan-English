package com.example.cet6vocabulary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * How far a button is currently pulled. One instance per button, owned by [rememberMagneticState];
 * both values are read only during the draw phase, so a moving finger invalidates drawing without
 * recomposing anything.
 */
@Stable
class MagneticState internal constructor(
    internal val enabled: Boolean,
    internal val touchSlop: Float
) {
    internal val translationX = Animatable(0f)
    internal val translationY = Animatable(0f)
}

/**
 * Magnetic state for a button. [enabled] false costs nothing at draw time, so buttons that should
 * stay still can share the same component. With reduce motion on, magnetic is removed completely:
 * no gesture listener, no translation and no settling animation.
 */
@Composable
fun rememberMagneticState(enabled: Boolean = true): MagneticState {
    val reduceMotion = LocalReduceMotion.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    return remember(enabled, reduceMotion, touchSlop) {
        MagneticState(enabled = enabled && !reduceMotion, touchSlop = touchSlop)
    }
}

/**
 * Pulls a button a few dp towards the pointer, like the button is magnetically attracted to the
 * touch.
 *
 * It observes on [PointerEventPass.Initial] and never consumes a change, so an existing clickable,
 * ripple, spotlight or scrolling parent keeps behaving exactly as before, and it adds no semantics
 * and no layout of its own. A clear vertical drag belongs to the scrolling parent: the magnetic
 * drops back to zero and the gesture is left alone. On release the button settles through
 * [MaoDanMotion.fast]. Nothing animates while no pointer is down.
 */
fun Modifier.magnetic(
    state: MagneticState,
    maxTranslation: Dp = MaoDanMotion.MagneticMaxTranslation
): Modifier {
    if (!state.enabled) return this
    return this
        .pointerInput(state, maxTranslation) {
            coroutineScope {
                val scope = this
                val maxPx = maxTranslation.toPx()
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val startX = down.position.x
                        val startY = down.position.y
                        var pressed = true
                        var scrolled = false
                        while (pressed) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            val position = change?.takeIf { it.pressed }?.position
                            if (position == null) {
                                pressed = false
                            } else {
                                val dx = (position.x - startX).absoluteValue
                                val dy = (position.y - startY).absoluteValue
                                // Past the slop and clearly vertical: the list is scrolling, so
                                // stop pulling and leave the gesture to the parent.
                                if (dy > state.touchSlop * 2f && dy > dx) scrolled = true
                                val targetX: Float
                                val targetY: Float
                                if (scrolled || size.width == 0 || size.height == 0) {
                                    targetX = 0f
                                    targetY = 0f
                                } else {
                                    // Calculate offset from center, normalize to [-1, 1], scale to max.
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val offsetX = position.x - centerX
                                    val offsetY = position.y - centerY
                                    targetX = (offsetX / centerX).coerceIn(-1f, 1f) * maxPx
                                    targetY = (offsetY / centerY).coerceIn(-1f, 1f) * maxPx
                                }
                                scope.launch {
                                    state.translationX.snapTo(targetX)
                                    state.translationY.snapTo(targetY)
                                }
                            }
                        }
                        // Both axes settle at the same time so the button is centered inside the
                        // short timing tier instead of taking it twice over.
                        scope.launch { state.translationX.animateTo(0f, MaoDanMotion.fast()) }
                        scope.launch { state.translationY.animateTo(0f, MaoDanMotion.fast()) }
                    }
                }
            }
        }
        .graphicsLayer {
            translationX = state.translationX.value
            translationY = state.translationY.value
        }
}