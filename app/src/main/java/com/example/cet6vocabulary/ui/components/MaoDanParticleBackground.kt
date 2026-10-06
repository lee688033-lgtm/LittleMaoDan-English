package com.example.cet6vocabulary.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Fixed seed: where the dust sits is part of the design, not a roll of the dice per launch. */
private const val ParticleSeed = 20261001

/**
 * Typical phone aspect ratio, used only to choose a grid whose cells come out close to square on
 * screen, so the dust is spread evenly in pixels rather than evenly in fractions.
 */
private const val CanvasAspect = 0.45f

/** How far into its cell a mote may sit, keeping it clear of the cell edges. */
private const val CellInset = 0.10f

private val TWO_PI = 2f * PI.toFloat()

/**
 * One mote of dust. Every field is decided once, when the layer is first composed, and nothing
 * here is derived from the clock or the theme, so no recomposition, tab switch, theme change or
 * loop restart can move a mote somewhere new.
 */
private data class Particle(
    /** Resting position as a fraction of the canvas, resolved against the real size when drawing. */
    val baseX: Float,
    val baseY: Float,
    /** 0..1 between [MaoDanMotion.ParticleMinRadius] and [MaoDanMotion.ParticleMaxRadius]. */
    val radiusFraction: Float,
    /** Offset into the loop, so the motes never drift or breathe in step. */
    val phase: Float,
    /** Signed scale of the drift orbit on each axis; the sign is what makes paths differ. */
    val directionX: Float,
    val directionY: Float,
    /** Index into the scheme palette, assigned round-robin rather than at random. */
    val colorIndex: Int,
    /** Per-mote alpha scale, so the layer has a little variance instead of one flat strength. */
    val weight: Float
)

/**
 * Lays the motes out on a jittered grid, one per cell, so they cover the canvas evenly instead of
 * clumping into a corner or leaving a hole the eye keeps returning to. Phases are stratified the
 * same way: mote `i` starts `i / count` of the way round the loop, plus a little jitter.
 */
private fun buildParticles(count: Int): List<Particle> {
    val random = Random(ParticleSeed)
    val columns = maxOf(1, sqrt(count * CanvasAspect).roundToInt())
    val rows = maxOf(1, ceil(count.toFloat() / columns).toInt())
    val span = 1f - 2f * CellInset
    return List(count) { index ->
        Particle(
            baseX = (index % columns + CellInset + random.nextFloat() * span) / columns,
            baseY = (index / columns + CellInset + random.nextFloat() * span) / rows,
            radiusFraction = random.nextFloat(),
            phase = (index + random.nextFloat()) / count,
            directionX = (0.6f + random.nextFloat() * 0.4f) * if (random.nextBoolean()) 1f else -1f,
            directionY = (0.6f + random.nextFloat() * 0.4f) * if (random.nextBoolean()) 1f else -1f,
            colorIndex = index % 3,
            weight = 0.75f + random.nextFloat() * 0.25f
        )
    }
}

/**
 * Dust floating over the ambient light: a handful of very small, very faint circles drifting on
 * slow closed orbits. It sits above [MaoDanAuroraBackground] and below the app's content, so the
 * aurora supplies the broad colour, this supplies the fine grain, and cards and the bottom bar
 * still cover both with their own opaque surfaces.
 *
 * Mount it as a full size sibling behind the content and it only ever paints: no layout, no
 * semantics, no pointer handling, so clicks, scrolling, ripple, spotlight, tilt, press scale and
 * the magnetic pull are all untouched.
 *
 * The whole layer is one canvas, one clock and plain `drawCircle` calls, deliberately: no
 * gradient, no glow and no blur, because a second soft light source would only muddy the aurora
 * and cost fill rate the background cannot spare. Everything derived from the theme or the canvas
 * size is resolved once inside the draw cache, and the single animated value is read in the draw
 * phase, so a frame allocates nothing and never recomposes a screen.
 *
 * Each mote moves on a closed ellipse, one angle driving both axes, which is what lets the loop
 * restart without a visible jump. The breath runs at twice that angle, so a mote's brightness is
 * not pinned to where it happens to be on its path, and it only ever dims to
 * [MaoDanMotion.ParticleBreathFactor] of its peak: dust should look like it is catching the light
 * a little more or a little less, never blinking out.
 *
 * With [enabled] false nothing is drawn at all. With reduce motion on the loop never starts and
 * the motes are drawn once in their resting positions, still visible, and nothing invalidates
 * that drawing again.
 *
 * @param enabled set false to drop the layer entirely, for a surface that wants no dust on it.
 */
@Composable
fun MaoDanParticleBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled) {
        Spacer(modifier)
        return
    }
    val reduceMotion = LocalReduceMotion.current
    val scheme = MaterialTheme.colorScheme
    // Dark dust sits a little stronger: on a near-black background the same alpha loses contrast,
    // and it has to stay readable with the aurora glowing underneath.
    val baseAlpha =
        if (isSystemInDarkTheme()) MaoDanMotion.ParticleDarkAlpha else MaoDanMotion.ParticleLightAlpha
    val palette = listOf<Color>(scheme.primary, scheme.secondary, scheme.tertiary)
    // Built once and remembered with no key: the layout is identical for the life of this
    // composition, which outlives every tab switch and every in-page reload.
    val particles = remember { buildParticles(MaoDanMotion.ParticleCount) }

    val transition = if (reduceMotion) null else rememberInfiniteTransition(label = "particles")
    // Master clock for the drift: 0 to 1 across one orbit, then it restarts. Linear, because the
    // motion is too slow to read any easing, and easing would make it stall at the turn.
    val progress = transition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MaoDanMotion.ParticleLoopMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleProgress"
    )

    Spacer(
        modifier.drawWithCache {
            val travel = size.minDimension * MaoDanMotion.ParticleTravelRatio
            val minRadius = MaoDanMotion.ParticleMinRadius.toPx()
            val radiusSpan = MaoDanMotion.ParticleMaxRadius.toPx() - minRadius
            val breathDepth = 1f - MaoDanMotion.ParticleBreathFactor
            // Resolved here rather than per frame: colour, radius and resting alpha of a mote do
            // not change between frames, only its position and its breath do.
            val colors = particles.map { palette[it.colorIndex] }
            val radii = FloatArray(particles.size) { minRadius + radiusSpan * particles[it].radiusFraction }
            val alphas = FloatArray(particles.size) { baseAlpha * particles[it].weight }
            onDrawBehind {
                // With reduce motion this stays null, so t is 0 and the dust sits at its origins.
                val t = progress?.value ?: 0f
                val width = size.width
                val height = size.height
                for (index in particles.indices) {
                    val particle = particles[index]
                    val angle = ((t + particle.phase) % 1f) * TWO_PI
                    // Twice the orbit angle: still seamless over the loop, but it decorrelates the
                    // breath from the position, so a mote is not always brightest at one end.
                    val breath = 1f - (sin(angle * 2f) + 1f) * 0.5f * breathDepth
                    drawCircle(
                        color = colors[index],
                        radius = radii[index],
                        center = Offset(
                            x = particle.baseX * width + cos(angle) * travel * particle.directionX,
                            y = particle.baseY * height + sin(angle) * travel * particle.directionY
                        ),
                        alpha = alphas[index] * breath
                    )
                }
            }
        }
    )
}
