package com.example.cet6vocabulary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cet6vocabulary.ui.theme.CET6VocabularyTheme

// "My" page icons taken verbatim from the Lucide icon set (ISC license),
// https://lucide.dev/icons/. Geometry is copied command-for-command from the official
// 24x24 SVG sources so they share the exact stroke system with the other Lucide files:
// 24 viewport, stroke 2, round caps and joins, no fill.
private const val LucideMineStrokeWidth = 2f

private fun lucideMineVector(
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
            strokeLineWidth = LucideMineStrokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
            pathBuilder = block
        )
    }
}.build()

object LucideMineIcons {
    /** Lucide "chart-no-axes-column" - 学习概览. */
    val ChartNoAxesColumn: ImageVector = lucideMineVector("LucideChartNoAxesColumn", listOf(
        {
            moveTo(5f, 21f)
            verticalLineToRelative(-6f)
        },
        {
            moveTo(12f, 21f)
            verticalLineTo(3f)
        },
        {
            moveTo(19f, 21f)
            verticalLineTo(9f)
        }
    ))

    /** Lucide "book-open" - 词库为空时的占位. */
    val BookOpen: ImageVector = lucideMineVector("LucideBookOpen", listOf(
        {
            moveTo(12f, 5f)
            verticalLineToRelative(16f)
        },
        {
            moveTo(20.001f, 19f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 22f, 17f)
            verticalLineTo(5f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.999f, -2f)
            lineTo(16f, 3.002f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 5f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, -4f, -2f)
            horizontalLineTo(4f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, 2f)
            verticalLineToRelative(12f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.999f, 2f)
            horizontalLineTo(8f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 2f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, -2f)
            close()
        }
    ))

    val All: List<ImageVector> = listOf(ChartNoAxesColumn, BookOpen)
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LucideMineIconsPreview() {
    CET6VocabularyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LucideMineIcons.All.forEach { icon ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
