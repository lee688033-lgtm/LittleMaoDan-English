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

private const val LucideWordStrokeWidth = 2f

private fun lucideWordVector(
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
            strokeLineWidth = LucideWordStrokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
            pathBuilder = block
        )
    }
}.build()

object LucideWordIcons {

    val ArrowLeft: ImageVector = lucideWordVector("LucideArrowLeft", listOf(
        {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
        },
        {
            moveTo(12f, 19f)
            lineTo(5f, 12f)
            lineTo(12f, 5f)
        }
    ))

    val Search: ImageVector = lucideWordVector("LucideSearch", listOf(
        {
            moveTo(11f, 3f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11f, 19f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11f, 3f)
            close()
        },
        {
            moveTo(21f, 21f)
            lineTo(16.65f, 16.65f)
        }
    ))
}
