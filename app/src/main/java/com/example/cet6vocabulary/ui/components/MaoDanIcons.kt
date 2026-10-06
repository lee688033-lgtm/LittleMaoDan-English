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

private const val IconStrokeWidth = 1.9f

private fun maoDanVector(
    name: String,
    block: PathBuilder.() -> Unit
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(androidx.compose.ui.graphics.Color.Black),
        strokeLineWidth = IconStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.NonZero,
        pathBuilder = block
    )
}.build()

object MaoDanIcons {
    /** House silhouette with an egg-shaped window: a branded home marker. */
    val Home: ImageVector = maoDanVector("MaoDanHome") {
        moveTo(4.5f, 10.5f)
        lineTo(12f, 4.5f)
        lineTo(19.5f, 10.5f)
        moveTo(6.5f, 9.2f)
        lineTo(6.5f, 19f)
        lineTo(17.5f, 19f)
        lineTo(17.5f, 9.2f)
        moveTo(10.2f, 19f)
        lineTo(10.2f, 14.2f)
        lineTo(13.8f, 14.2f)
        lineTo(13.8f, 19f)
        moveTo(12f, 8.8f)
        lineTo(10.8f, 10f)
        lineTo(10.8f, 11.7f)
        lineTo(12f, 12.6f)
        lineTo(13.2f, 11.7f)
        lineTo(13.2f, 10f)
        lineTo(12f, 8.8f)
    }

    /** Rounded word card with a page fold and short vocabulary lines. */
    val Word: ImageVector = maoDanVector("MaoDanWord") {
        moveTo(5.5f, 4.8f)
        lineTo(18.5f, 4.8f)
        lineTo(18.5f, 19.2f)
        lineTo(5.5f, 19.2f)
        lineTo(5.5f, 4.8f)
        moveTo(8.5f, 8.8f)
        lineTo(15.5f, 8.8f)
        moveTo(8.5f, 12f)
        lineTo(14f, 12f)
        moveTo(8.5f, 15.2f)
        lineTo(12.5f, 15.2f)
        moveTo(15.2f, 4.8f)
        lineTo(15.2f, 7.1f)
        lineTo(17.4f, 7.1f)
    }

    /** Academic cap over an open book: study and memorisation. */
    val Study: ImageVector = maoDanVector("MaoDanStudy") {
        moveTo(3.8f, 8.5f)
        lineTo(12f, 4.8f)
        lineTo(20.2f, 8.5f)
        lineTo(12f, 12.2f)
        lineTo(3.8f, 8.5f)
        moveTo(6.2f, 10f)
        lineTo(6.2f, 13.8f)
        lineTo(12f, 16.4f)
        lineTo(17.8f, 13.8f)
        lineTo(17.8f, 10f)
        moveTo(20.2f, 8.5f)
        lineTo(20.2f, 14.5f)
        moveTo(8.2f, 17f)
        lineTo(5.8f, 19.2f)
        lineTo(10.2f, 19.2f)
        moveTo(15.8f, 17f)
        lineTo(18.2f, 19.2f)
        lineTo(13.8f, 19.2f)
    }

    /** Word card with a diagonal pen: spelling practice. */
    val Spell: ImageVector = maoDanVector("MaoDanSpell") {
        moveTo(4.8f, 5.5f)
        lineTo(14.8f, 5.5f)
        lineTo(14.8f, 15.5f)
        lineTo(4.8f, 15.5f)
        lineTo(4.8f, 5.5f)
        moveTo(7.2f, 9f)
        lineTo(12f, 9f)
        moveTo(7.2f, 12f)
        lineTo(10.5f, 12f)
        moveTo(13.2f, 17.8f)
        lineTo(18.7f, 12.3f)
        lineTo(20f, 13.6f)
        lineTo(14.5f, 19.1f)
        lineTo(12.7f, 19.5f)
        lineTo(13.2f, 17.8f)
        moveTo(18.7f, 12.3f)
        lineTo(17.4f, 11f)
    }

    /** Egg-shaped profile with a simple face, reserved for the personal area. */
    val Profile: ImageVector = maoDanVector("MaoDanProfile") {
        moveTo(12f, 4.2f)
        lineTo(8.2f, 5.4f)
        lineTo(6.2f, 8.7f)
        lineTo(6.2f, 14.8f)
        lineTo(8.4f, 18.2f)
        lineTo(12f, 19.8f)
        lineTo(15.6f, 18.2f)
        lineTo(17.8f, 14.8f)
        lineTo(17.8f, 8.7f)
        lineTo(15.8f, 5.4f)
        lineTo(12f, 4.2f)
        moveTo(9.2f, 11f)
        lineTo(9.2f, 11.1f)
        moveTo(14.8f, 11f)
        lineTo(14.8f, 11.1f)
        moveTo(9.2f, 14.4f)
        lineTo(12f, 15.8f)
        lineTo(14.8f, 14.4f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FC)
@Composable
private fun MaoDanIconsPreview() {
    CET6VocabularyTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconPreviewRow(selected = true)
                IconPreviewRow(selected = false)
            }
        }
    }
}

@Composable
private fun IconPreviewRow(selected: Boolean) {
    val icons = listOf(
        MaoDanIcons.Home,
        MaoDanIcons.Word,
        MaoDanIcons.Study,
        MaoDanIcons.Spell,
        MaoDanIcons.Profile
    )
    Row(
        modifier = Modifier.background(
            if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icons.forEachIndexed { index, icon ->
            Icon(
                imageVector = icon,
                contentDescription = listOf("首页", "单词", "背诵", "拼写", "我的")[index],
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
