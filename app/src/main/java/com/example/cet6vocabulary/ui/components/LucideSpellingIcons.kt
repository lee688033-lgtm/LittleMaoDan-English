package com.example.cet6vocabulary.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val LucideSpellingStrokeWidth = 2f

private fun lucideSpellingVector(
    name: String,
    paths: List<PathBuilder.() -> Unit>
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    paths.forEach { block ->
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = LucideSpellingStrokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
            pathBuilder = block
        )
    }
}.build()

/**
 * Lucide icons used by the spelling page only. Geometry is mapped 1:1 from the
 * official Lucide SVG sources (check, circle-check, circle-x, arrow-right,
 * rotate-ccw) and shares the stroke system of the other Lucide icon sets.
 */
object LucideSpellingIcons {

    val Check: ImageVector = lucideSpellingVector("LucideCheck", listOf(
        {
            moveTo(20f, 6f)
            lineTo(9f, 17f)
            lineTo(4f, 12f)
        }
    ))

    val CircleCheck: ImageVector = lucideSpellingVector("LucideCircleCheck", listOf(
        {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 22f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
            close()
        },
        {
            moveTo(16f, 9f)
            lineTo(10.5f, 14.5f)
            lineTo(8f, 12f)
        }
    ))

    val CircleX: ImageVector = lucideSpellingVector("LucideCircleX", listOf(
        {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 22f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
            close()
        },
        {
            moveTo(15f, 9f)
            lineTo(9f, 15f)
        },
        {
            moveTo(9f, 9f)
            lineTo(15f, 15f)
        }
    ))

    val ArrowRight: ImageVector = lucideSpellingVector("LucideArrowRight", listOf(
        {
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        },
        {
            moveTo(12f, 5f)
            lineTo(19f, 12f)
            lineTo(12f, 19f)
        }
    ))

    val RotateCcw: ImageVector = lucideSpellingVector("LucideRotateCcw", listOf(
        {
            moveTo(3f, 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12f, 3f)
            arcTo(9.75f, 9.75f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5.26f, 5.74f)
            lineTo(3f, 8f)
        },
        {
            moveTo(3f, 3f)
            lineTo(3f, 8f)
            lineTo(8f, 8f)
        }
    ))

    val All: List<ImageVector> = listOf(Check, CircleCheck, CircleX, ArrowRight, RotateCcw)
}
