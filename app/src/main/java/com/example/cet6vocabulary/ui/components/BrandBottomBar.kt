package com.example.cet6vocabulary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val NavigationAnimationDuration = 210

@Composable
fun BrandBottomBar(
    items: List<CET6NavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                BrandNavigationItem(
                    item = item,
                    selected = selectedIndex == index,
                    onClick = { onItemSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BrandNavigationItem(
    item: CET6NavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = selected, label = "bottom bar item")
    val iconScale by transition.animateFloat(
        transitionSpec = { tween(NavigationAnimationDuration, easing = FastOutSlowInEasing) },
        label = "icon scale"
    ) { isSelected -> if (isSelected) 1f else 0.92f }
    val capsuleAlpha by transition.animateFloat(
        transitionSpec = { tween(NavigationAnimationDuration, easing = FastOutSlowInEasing) },
        label = "capsule alpha"
    ) { isSelected -> if (isSelected) 1f else 0f }
    val labelAlpha by transition.animateFloat(
        transitionSpec = { tween(NavigationAnimationDuration, easing = FastOutSlowInEasing) },
        label = "label alpha"
    ) { isSelected -> if (isSelected) 1f else 0.78f }
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(NavigationAnimationDuration),
        label = "navigation content color"
    )

    Column(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = capsuleAlpha),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp).scale(iconScale),
                tint = contentColor
            )
        }
        Text(
            text = item.label,
            modifier = Modifier.padding(top = 2.dp).alpha(labelAlpha),
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

private fun brandVector(
    name: String,
    viewportWidth: Float = 24f,
    viewportHeight: Float = 24f,
    block: PathBuilder.() -> Unit
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = viewportWidth,
    viewportHeight = viewportHeight
).apply {
    path(stroke = SolidColor(androidx.compose.ui.graphics.Color.Black), strokeLineWidth = 1.8f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round, pathFillType = PathFillType.NonZero, pathBuilder = block)
}.build()

val BrandEggIcon: ImageVector = brandVector("BrandEgg") {
    moveTo(12f, 3.5f)
    lineTo(7f, 5f)
    lineTo(5f, 9f)
    lineTo(5f, 14f)
    lineTo(7f, 18f)
    lineTo(12f, 20.5f)
    lineTo(17f, 18f)
    lineTo(19f, 14f)
    lineTo(19f, 9f)
    lineTo(17f, 5f)
    lineTo(12f, 3.5f)
    moveTo(9.3f, 11f)
    moveTo(14.7f, 11f)
    moveTo(9.5f, 15.3f)
    lineTo(14.5f, 15.3f)
}

val BrandDictionaryIcon: ImageVector = brandVector("BrandDictionary") {
    moveTo(12f, 5f)
    lineTo(4.5f, 5f)
    verticalLineTo(19f)
    lineTo(12f, 19f)
    moveTo(12f, 5f)
    lineTo(19.5f, 5f)
    verticalLineTo(19f)
    lineTo(12f, 19f)
    moveTo(12f, 5f)
    verticalLineTo(19f)
    moveTo(7.1f, 8f)
    horizontalLineTo(9.2f)
}

val BrandMemoryIcon: ImageVector = brandVector("BrandMemory") {
    moveTo(8.1f, 7f)
    lineTo(5f, 10f)
    lineTo(5f, 14f)
    lineTo(8f, 17f)
    lineTo(10f, 17f)
    lineTo(12f, 18f)
    lineTo(15f, 17f)
    lineTo(18f, 14f)
    lineTo(19f, 11f)
    lineTo(16f, 8f)
    lineTo(12f, 7f)
    lineTo(10f, 8f)
    moveTo(8.7f, 12.2f)
    horizontalLineTo(11.2f)
    moveTo(12.8f, 12.2f)
    horizontalLineTo(15.3f)
    moveTo(10.6f, 14.6f)
    horizontalLineTo(13.4f)
}

val BrandSpellingIcon: ImageVector = brandVector("BrandSpelling") {
    moveTo(4.5f, 6f)
    horizontalLineTo(10.2f)
    verticalLineTo(11.2f)
    horizontalLineTo(4.5f)
    close()
    moveTo(13.8f, 6f)
    horizontalLineTo(19.5f)
    verticalLineTo(11.2f)
    horizontalLineTo(13.8f)
    close()
    moveTo(4.5f, 13f)
    horizontalLineTo(10.2f)
    verticalLineTo(18.2f)
    horizontalLineTo(4.5f)
    close()
    moveTo(13.8f, 13f)
    horizontalLineTo(19.5f)
    verticalLineTo(18.2f)
    horizontalLineTo(13.8f)
    close()
    moveTo(6.2f, 9.2f)
    lineTo(7.3f, 7.8f)
    lineTo(8.4f, 9.2f)
}

val BrandAvatarIcon: ImageVector = brandVector("BrandAvatar") {
    moveTo(12f, 4f)
    lineTo(7f, 5.5f)
    lineTo(5.5f, 9f)
    lineTo(5.5f, 15f)
    lineTo(8f, 18.5f)
    lineTo(12f, 20f)
    lineTo(16f, 18.5f)
    lineTo(18.5f, 15f)
    lineTo(18.5f, 9f)
    lineTo(17f, 5.5f)
    lineTo(12f, 4f)
    moveTo(9.2f, 11.2f)
    moveTo(14.8f, 11.2f)
    moveTo(8.8f, 15.3f)
    lineTo(15.2f, 15.3f)
}
