package com.example.cet6vocabulary.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import com.example.cet6vocabulary.ui.theme.LocalReduceMotion
import com.example.cet6vocabulary.ui.theme.MaoDanMotion
import kotlin.math.sqrt
import kotlinx.coroutines.delay

// Unit vector of the sweep axis: nearly horizontal, tipped up by ShineTilt so the band leans a few
// degrees off vertical. Derived from a token once instead of being recomputed on every frame.
private val ShineAxisX = 1f / sqrt(1f + MaoDanMotion.ShineTilt * MaoDanMotion.ShineTilt)
private val ShineAxisY = -MaoDanMotion.ShineTilt * ShineAxisX

/**
 * A label that catches the light once, as the page it belongs to arrives: a soft band crosses the
 * glyphs from left to right, and then the label is an ordinary [Text] again. It never repeats, it
 * never moves the text and it never retints it, so layout size, colour, semantics and click
 * behaviour stay exactly as the underlying Text provides them.
 *
 * Reserved for the one or two labels worth pointing at, never body copy: a study app has to stay
 * quiet where reading happens. With reduce motion on, the sweep does not merely become instant, it
 * does not exist at all: no extra modifier, no text layout retained, no extra frame.
 */
@Composable
fun ShinyText(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    delayMillis: Int = MaoDanMotion.ShineDelayMillis,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    val reduceMotion = LocalReduceMotion.current
    val animate = enabled && !reduceMotion
    // Dark labels are already painted in a near-white ink, so the same peak would be the brightest
    // thing on the page. Both peaks are tokens, and the label keeps its own colour either way.
    val peakAlpha =
        if (isSystemInDarkTheme()) MaoDanMotion.ShineDarkPeakAlpha else MaoDanMotion.ShinePeakAlpha
    val band = remember(peakAlpha) {
        listOf(
            Color.White.copy(alpha = 0f),
            Color.White.copy(alpha = peakAlpha),
            Color.White.copy(alpha = 0f)
        )
    }
    val progress = remember(animate) { Animatable(if (animate) 0f else 1f) }
    // The layout comes from the Text's own first measure and is only ever read while drawing, so a
    // travelling band repaints this label instead of recomposing it.
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val reportLayout: (TextLayoutResult) -> Unit = remember { { layout = it } }
    LaunchedEffect(progress, delayMillis) {
        if (!animate) return@LaunchedEffect
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(targetValue = 1f, animationSpec = MaoDanMotion.shine())
    }
    // A local read keeps the null check meaningful; a delegated property cannot be smart cast.
    val textLayout = layout
    Text(
        text = text,
        modifier = if (animate && textLayout != null) modifier.shineOverlay(progress, band, textLayout) else modifier,
        style = style,
        color = color,
        onTextLayout = if (animate) reportLayout else null
    )
}

/**
 * Paints the band by refilling the very same glyphs with it, so the light exists only where the
 * letters are: nothing washes over the surface behind them, and no isolation layer or blend mode is
 * needed, which keeps the result identical on every supported Android version.
 */
private fun Modifier.shineOverlay(
    progress: Animatable<Float, *>,
    band: List<Color>,
    layout: TextLayoutResult
): Modifier =
    drawWithContent {
        val sweep = progress.value
        drawContent()
        if (sweep <= 0f || sweep >= 1f) return@drawWithContent
        val width = size.width * MaoDanMotion.ShineWidthRatio
        // Travels from fully off the left edge to fully off the right one.
        val center = -width / 2f + sweep * (size.width + width)
        val halfX = width / 2f * ShineAxisX
        val halfY = width / 2f * ShineAxisY
        val midY = size.height / 2f
        drawText(
            layout,
            brush = Brush.linearGradient(
                colors = band,
                start = Offset(center - halfX, midY - halfY),
                end = Offset(center + halfX, midY + halfY)
            )
        )
    }
