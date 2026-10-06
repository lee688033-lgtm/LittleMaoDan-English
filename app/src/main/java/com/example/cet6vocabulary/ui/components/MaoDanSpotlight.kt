package com.example.cet6vocabulary.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion

/**
 * Where the pointer is and how visible its light currently is. One instance per card, owned by
 * [rememberSpotlightState]; both fields are only read during the draw phase, so a moving finger
 * invalidates drawing without recomposing anything.
 */
@Stable
class SpotlightState internal constructor(internal val enabled: Boolean) {
    internal val position = mutableStateOf(Offset.Unspecified)
    internal val active = mutableStateOf(false)

    // Replaced on every composition by the animated value from rememberSpotlightState.
    internal var fade: State<Float> = mutableFloatStateOf(0f)
}

/**
 * Spotlight state for a card. [enabled] false costs nothing at draw time, so cards that should
 * stay flat can share the same component. With reduce motion on, the light switches on and off
 * instead of fading.
 */
@Composable
fun rememberSpotlightState(enabled: Boolean = true): SpotlightState {
    val reduceMotion = LocalReduceMotion.current
    val state = remember(enabled) { SpotlightState(enabled) }
    val fadeSpec = remember(reduceMotion) {
        if (reduceMotion) snap<Float>() else MaoDanMotion.fast<Float>()
    }
    state.fade = animateFloatAsState(
        targetValue = if (enabled && state.active.value) 1f else 0f,
        animationSpec = fadeSpec,
        label = "spotlightFade"
    )
    return state
}

/**
 * Draws a soft radial light that follows the pointer inside [shape], the way a surface catches
 * light where it is touched.
 *
 * It observes on [PointerEventPass.Initial] and never consumes a change, so an existing
 * clickable, ripple or scrolling parent keeps behaving exactly as before, and it adds no
 * semantics. Nothing is drawn while no pointer is down: there is no idle animation and no
 * repeating frame work.
 */
fun Modifier.spotlight(
    state: SpotlightState,
    shape: Shape,
    color: Color,
    peakAlpha: Float = MaoDanMotion.SpotlightPeakAlpha
): Modifier {
    if (!state.enabled) return this
    return this
        .pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    state.position.value = down.position
                    state.active.value = true
                    var tracked: PointerInputChange? = down
                    while (tracked != null && tracked.pressed) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        tracked = event.changes.firstOrNull { it.id == down.id }
                        if (tracked != null) state.position.value = tracked.position
                    }
                    state.active.value = false
                }
            }
        }
        .drawWithCache {
            // Rebuilt only when the card is measured again, never per pointer move.
            val clip = Path().apply {
                addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
            }
            val radius = (minOf(size.width, size.height) * MaoDanMotion.SpotlightRadiusRatio)
                .coerceIn(MaoDanMotion.SpotlightMinRadius.toPx(), MaoDanMotion.SpotlightMaxRadius.toPx())
            onDrawWithContent {
                drawContent()
                val fade = state.fade.value
                val center = state.position.value
                if (fade <= 0f || !center.isSpecified) return@onDrawWithContent
                val peak = peakAlpha * fade
                clipPath(clip) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = peak),
                                color.copy(alpha = peak * 0.4f),
                                color.copy(alpha = 0f)
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }
        }
}
