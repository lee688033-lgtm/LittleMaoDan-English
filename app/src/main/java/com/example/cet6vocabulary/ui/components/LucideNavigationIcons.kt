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
import com.example.cet6vocabulary.ui.theme.MaoDanTextTertiary

// Bottom navigation icons taken verbatim from the Lucide icon set (ISC license),
// https://lucide.dev/icons/. Geometry is copied command-for-command from the official
// 24x24 SVG sources, so all five share one stroke system: 24 viewport, stroke 2,
// round caps and joins, no fill.
private const val LucideStrokeWidth = 2f

private fun lucideVector(
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
            strokeLineWidth = LucideStrokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
            pathBuilder = block
        )
    }
}.build()

object LucideNavigationIcons {
    /** Lucide "house". */
    val Home: ImageVector = lucideVector("LucideHouse", listOf(
        {
            moveTo(15f, 21f)
            verticalLineToRelative(-8f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, -1f)
            horizontalLineToRelative(-4f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1f, 1f)
            verticalLineToRelative(8f)
        },
        {
            moveTo(3f, 10f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.709f, -1.528f)
            lineToRelative(7f, -6f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2.582f, 0f)
            lineToRelative(7f, 6f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 10f)
            verticalLineToRelative(9f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
            horizontalLineTo(5f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
            close()
        }
    ))

    /** Lucide "book-open-text". */
    val Word: ImageVector = lucideVector("LucideBookOpenText", listOf(
        {
            moveTo(12f, 5f)
            verticalLineToRelative(16f)
        },
        {
            moveTo(16f, 13f)
            horizontalLineToRelative(2f)
        },
        {
            moveTo(16f, 9f)
            horizontalLineToRelative(2f)
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
        },
        {
            moveTo(6f, 13f)
            horizontalLineToRelative(2f)
        },
        {
            moveTo(6f, 9f)
            horizontalLineToRelative(2f)
        }
    ))

    /** Lucide "graduation-cap". */
    val Study: ImageVector = lucideVector("LucideGraduationCap", listOf(
        {
            moveTo(21.42f, 10.922f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.019f, -1.838f)
            lineTo(12.83f, 5.18f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.66f, 0f)
            lineTo(2.6f, 9.08f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 1.832f)
            lineToRelative(8.57f, 3.908f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1.66f, 0f)
            close()
        },
        {
            moveTo(22f, 10f)
            verticalLineToRelative(6f)
        },
        {
            moveTo(6f, 12.5f)
            verticalLineTo(16f)
            arcToRelative(6f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 0f)
            verticalLineToRelative(-3.5f)
        }
    ))

    /** Lucide "pen-line". */
    val Spell: ImageVector = lucideVector("LucidePenLine", listOf(
        {
            moveTo(13f, 21f)
            horizontalLineToRelative(8f)
        },
        {
            moveTo(21.174f, 6.812f)
            arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3.986f, -3.987f)
            lineTo(3.842f, 16.174f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.5f, 0.83f)
            lineToRelative(-1.321f, 4.352f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.623f, 0.622f)
            lineToRelative(4.353f, -1.32f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.83f, -0.497f)
            close()
        }
    ))

    /** Lucide "user-round". */
    val Profile: ImageVector = lucideVector("LucideUserRound", listOf(
        {
            moveTo(12f, 3f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12f, 13f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12f, 3f)
            close()
        },
        {
            moveTo(20f, 21f)
            arcToRelative(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, -16f, 0f)
        }
    ))

    val All: List<ImageVector> = listOf(Home, Word, Study, Spell, Profile)
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LucideNavigationIconsPreview() {
    CET6VocabularyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LucideIconPreviewRow(selected = true)
                LucideIconPreviewRow(selected = false)
            }
        }
    }
}

@Composable
private fun LucideIconPreviewRow(selected: Boolean) {
    val labels = listOf("首页", "单词", "背诵", "拼写", "我的")
    Row(
        modifier = Modifier.background(
            if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LucideNavigationIcons.All.forEachIndexed { index, icon ->
            Icon(
                imageVector = icon,
                contentDescription = labels[index],
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaoDanTextTertiary
            )
        }
    }
}
