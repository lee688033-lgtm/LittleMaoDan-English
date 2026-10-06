package com.example.cet6vocabulary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MaoDanCard(
    modifier: Modifier = Modifier,
    spotlightEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val spotlight = rememberSpotlightState(enabled = spotlightEnabled)
    Card(
        modifier = modifier.spotlight(spotlight, shape, MaterialTheme.colorScheme.primary),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun MaoDanOutlinedCard(
    modifier: Modifier = Modifier,
    spotlightEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val spotlight = rememberSpotlightState(enabled = spotlightEnabled)
    Card(
        modifier = modifier.spotlight(spotlight, shape, MaterialTheme.colorScheme.primary),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}
