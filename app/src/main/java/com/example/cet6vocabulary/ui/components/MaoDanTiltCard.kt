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
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * How far a card is currently tipped. One instance per card, owned by [rememberTiltState]; both
 * values are read only during the draw phase, so a moving finger invalidates drawing without
 * recomposing anything.
 */
@Stable
class TiltState internal constructor(
    internal val enabled: Boolean,
    internal val touchSlop: Float
) {
    internal val rotationX = Animatable(0f)
    internal val rotationY = Animatable(0f)
}

/**
 * Tilt state for a card. [enabled] false costs nothing at draw time, so cards that should stay
 * flat can share the same component. With reduce motion on, tilt is removed completely: no
 * gesture listener, no rotation and no settling animation.
 */
@Composable
fun rememberTiltState(enabled: Boolean = true): TiltState {
    val reduceMotion = LocalReduceMotion.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    return remember(enabled, reduceMotion, touchSlop) {
        TiltState(enabled = enabled && !reduceMotion, touchSlop = touchSlop)
    }
}

/**
 * Tips a card a few degrees towards the pointer, the way a card tilts under a finger.
 *
 * It observes on [PointerEventPass.Initial] and never consumes a change, so an existing
 * clickable, ripple, spotlight or scrolling parent keeps behaving exactly as before, and it adds
 * no semantics and no layout of its own. A clear vertical drag belongs to the scrolling parent:
 * the tilt drops back to flat and the gesture is left alone. On release the card settles through
 * [MaoDanMotion.fast]. Nothing animates while no pointer is down.
 */
fun Modifier.tilt(
    state: TiltState,
    maxRotation: Float = MaoDanMotion.TiltMaxRotation
): Modifier {
    if (!state.enabled) return this
    return this
        .pointerInput(state, maxRotation) {
            coroutineScope {
                val scope = this
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
                                // stop tilting and leave the gesture to the parent.
                                if (dy > state.touchSlop * 2f && dy > dx) scrolled = true
                                val tiltX: Float
                                val tiltY: Float
                                if (scrolled || size.width == 0 || size.height == 0) {
                                    tiltX = 0f
                                    tiltY = 0f
                                } else {
                                    val normalizedX = (position.x / size.width) * 2f - 1f
                                    val normalizedY = (position.y / size.height) * 2f - 1f
                                    tiltX = (-normalizedY * maxRotation).coerceIn(-maxRotation, maxRotation)
                                    tiltY = (normalizedX * maxRotation).coerceIn(-maxRotation, maxRotation)
                                }
                                scope.launch {
                                    state.rotationX.snapTo(tiltX)
                                    state.rotationY.snapTo(tiltY)
                                }
                            }
                        }
                        // Both axes settle at the same time so the card is flat inside the short
                        // timing tier instead of taking it twice over.
                        scope.launch { state.rotationX.animateTo(0f, MaoDanMotion.fast()) }
                        scope.launch { state.rotationY.animateTo(0f, MaoDanMotion.fast()) }
                    }
                }
            }
        }
        .graphicsLayer {
            rotationX = state.rotationX.value
            rotationY = state.rotationY.value
            cameraDistance = MaoDanMotion.TiltCameraDistance
        }
}
