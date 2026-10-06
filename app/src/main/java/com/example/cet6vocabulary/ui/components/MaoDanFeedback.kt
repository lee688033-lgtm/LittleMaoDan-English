package com.example.cet6vocabulary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion

/** Result of an answer, used to pick the one-shot feedback animation. */
enum class MaoDanFeedbackKind { SUCCESS, ERROR }

/**
 * One-shot answer feedback around a result surface: a small scale pulse on success, a short
 * horizontal shake on error. It fires once when the content enters the composition, never loops,
 * and only moves the wrapped surface, so the input field and its state stay untouched.
 */
@Composable
fun MaoDanFeedbackBox(
    kind: MaoDanFeedbackKind,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val pulse = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(kind, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        when (kind) {
            MaoDanFeedbackKind.SUCCESS -> pulse.pulseOnce()
            MaoDanFeedbackKind.ERROR -> shake.shakeOnce()
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
            translationX = shake.value * MaoDanMotion.ShakeAmplitude.toPx()
        }
    ) { content() }
}

private suspend fun Animatable<Float, AnimationVector1D>.pulseOnce() {
    snapTo(1f)
    animateTo(targetValue = MaoDanMotion.SuccessPulseScale, animationSpec = MaoDanMotion.stateSpring())
    animateTo(targetValue = 1f, animationSpec = MaoDanMotion.stateSpring())
}

/** Fractions of [MaoDanMotion.ShakeAmplitude]; ~290ms end to end and always back at rest. */
private suspend fun Animatable<Float, AnimationVector1D>.shakeOnce() {
    snapTo(0f)
    animateTo(targetValue = -1f, animationSpec = MaoDanMotion.shakeStep(70))
    animateTo(targetValue = 1f, animationSpec = MaoDanMotion.shakeStep(90))
    animateTo(targetValue = -0.5f, animationSpec = MaoDanMotion.shakeStep(70))
    animateTo(targetValue = 0f, animationSpec = MaoDanMotion.shakeStep(60))
}
