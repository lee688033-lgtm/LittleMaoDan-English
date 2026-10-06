package com.example.cet6vocabulary.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.cet6vocabulary.ui.theme.MaoDanDimens

@Composable
fun MaoDanPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource = interactionSource, enabled = enabled)
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(pressScale).defaultMinSize(minHeight = MaoDanDimens.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        interactionSource = interactionSource
    ) {
        if (content != null) content() else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MaoDanSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource = interactionSource, enabled = enabled)
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(pressScale).defaultMinSize(minHeight = MaoDanDimens.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        interactionSource = interactionSource
    ) {
        if (content != null) content() else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MaoDanTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource = interactionSource, enabled = enabled)
    TextButton(
        onClick = onClick,
        modifier = modifier.pressScale(pressScale).defaultMinSize(minHeight = MaoDanDimens.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        interactionSource = interactionSource
    ) {
        if (content != null) content() else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
