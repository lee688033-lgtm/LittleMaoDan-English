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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// One wash per screen corner so colour is spread across the whole background instead of pooling
// in a single spot. Origins are fractions of the canvas; drifts are directions scaled by
// MaoDanMotion.AuroraTravelRatio, so one token sets how far everything travels and these only
// decide the shape of each blob's path. Consecutive blobs push in different directions, so the
// washes slide past one another rather than translating as a block.
private val BlobOrigins = listOf(
    Offset(0.05f, 0.10f), // upper left
    Offset(0.90f, 0.15f), // upper right
    Offset(0.15f, 0.85f), // lower left
    Offset(0.85f, 0.75f)  // lower right
)
private val BlobDrifts = listOf(
    Offset(1.0f, 0.9f),   // right and down
    Offset(-1.0f, 0.8f),  // left and down
    Offset(0.9f, -1.0f),  // right and up
    Offset(-0.8f, -0.9f)  // left and up
)

// Multiplied by the scheme's peak alpha to give each blob its own strength, strongest at the top
// left and easing off towards the bottom.
private val BlobWeights = listOf(1f, 0.82f, 0.64f, 0.72f)

private val TWO_PI = 2f * PI.toFloat()

/**
 * Ambient colour for the whole app: four very large radial washes that sit under every page and
 * card, overlapping until they read as one continuous field of light rather than as four circles.
 *
 * The washes orbit continuously on a [MaoDanMotion.AuroraLoopMillis] cycle, each offset by a
 * quarter of the loop so they never move in step, while a slower breath raises and lowers their
 * alpha. The loop is deliberately long: it should read as the room's light shifting, not as an
 * animation the user watches.
 *
 * Colours all come from the active scheme, and the four are chosen for hue separation rather than
 * for strength: primary and secondary are both blues in this theme, so tertiary and
 * tertiaryContainer carry the warm side and keep the field from collapsing into a single flat tint.
 *
 * Mount it as a full size sibling behind the content and it paints under it, adding no layout, no
 * semantics and no pointer handling of its own, so scrolling, clicks, ripple, spotlight, tilt and
 * press scale are all untouched. Every brush is built once inside the draw cache and only moved
 * with translate(), so a frame allocates nothing. The animated values are read in the draw phase
 * rather than during composition, which invalidates drawing only and never recomposes a screen.
 *
 * With reduce motion on the loop does not start: the washes are drawn once in their resting
 * positions, at full strength, and nothing invalidates this drawing again.
 *
 * The root scaffold paints the opaque window background and this layer sits directly on top of it.
 * Each page's own scaffold therefore passes `containerColor = Color.Transparent`: a second opaque
 * fill there would cover the light for that page. Cards and bars keep their own surfaces, so they
 * still read as solid objects resting on the lit background and body text never loses contrast.
 */
@Composable
fun MaoDanAuroraBackground(modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val scheme = MaterialTheme.colorScheme
    // Dark surfaces are near-black, so the same alpha would read as a glowing blob rather than as
    // room light; dark mode gets its own lower peak.
    val peakAlpha =
        if (isSystemInDarkTheme()) MaoDanMotion.AuroraDarkPeakAlpha else MaoDanMotion.AuroraLightPeakAlpha
    val blobColors = listOf<Color>(
        scheme.primary,
        scheme.tertiary,
        scheme.secondary,
        scheme.tertiaryContainer
    )

    val transition = if (reduceMotion) null else rememberInfiniteTransition(label = "aurora")
    // Master clock for the orbit: 0 to 1 across one full loop, then it restarts. Linear, because a
    // slow drift that eased in and out would visibly stall at each turn.
    val motion = transition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MaoDanMotion.AuroraLoopMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraMotion"
    )
    // Breath: 0 to 1 to 0, reversing every AuroraBreathMillis. Its period shares no factor with the
    // orbit, so the two rhythms never lock together and produce a visible beat.
    val breath = transition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = MaoDanMotion.AuroraBreathMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraBreath"
    )

    Spacer(
        modifier.drawWithCache {
            val radius = size.minDimension * MaoDanMotion.AuroraBlobRadiusRatio
            val travel = size.minDimension * MaoDanMotion.AuroraTravelRatio
            // Each gradient is centred on the origin at its real radius and moved with translate(),
            // so a drifting blob reuses one brush for the whole loop instead of rebuilding four
            // gradients per frame. The middle stop keeps the core broad, which is what lets two
            // overlapping washes blend into a gradient instead of showing a seam.
            val brushes: List<Brush> = blobColors.mapIndexed { index, color ->
                val peak = peakAlpha * BlobWeights[index]
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = peak),
                        color.copy(alpha = peak * 0.45f),
                        color.copy(alpha = 0f)
                    ),
                    center = Offset.Zero,
                    radius = radius
                )
            }
            onDrawBehind {
                // With reduce motion these stay null, so t is 0 and the washes sit at their origins.
                val t = motion?.value ?: 0f
                val breathFactor = MaoDanMotion.AuroraBreathFactor
                // Reverse repeat gives a triangle from 0 to 1 to 0; folding it around its midpoint
                // turns that into a swell that peaks once per breath.
                val swell = 1f - abs((breath?.value ?: 0f) * 2f - 1f)
                // Alpha only, never scale: breathing the radius would pull the washes off their
                // corners and expose flat background at the edges.
                val alphaMod = breathFactor + (1f - breathFactor) * swell
                for (index in brushes.indices) {
                    val origin = BlobOrigins[index]
                    val drift = BlobDrifts[index]
                    // Staggering each blob by a quarter loop keeps them out of step, and the
                    // sine/cosine pair makes the path a closed ellipse, so the loop has no seam
                    // where it restarts.
                    val angle = ((t + index / brushes.size.toFloat()) % 1f) * TWO_PI
                    translate(
                        left = origin.x * size.width + cos(angle) * travel * drift.x,
                        top = origin.y * size.height + sin(angle) * travel * drift.y
                    ) {
                        drawCircle(
                            brush = brushes[index],
                            radius = radius,
                            center = Offset.Zero,
                            alpha = alphaMod
                        )
                    }
                }
            }
        }
    )
}