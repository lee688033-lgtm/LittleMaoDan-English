package com.example.cet6vocabulary.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.cet6vocabulary.ui.theme.MaoDanDimens
import com.example.cet6vocabulary.ui.theme.MaoDanShapes

@Composable
fun MaoDanProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    val animatedProgress = rememberAnimatedProgress(target = progress, animate = animate)
    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(MaoDanDimens.progressHeight)
            .clip(MaoDanShapes.pill),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer
    )
}
