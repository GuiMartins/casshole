package com.casshole.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.casshole.R
import kotlinx.coroutines.delay

/**
 * Masks the value but reveals the last typed character for a moment, like in
 * banking app password fields. Also offers an eye icon to show/hide it all.
 * Used for PDF-open passwords and the email app password.
 */
private class RevealLastCharacterTransformation(
    private val revealLast: Boolean,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val value = text.text
        val masked = when {
            value.isEmpty() -> ""
            revealLast -> "•".repeat(value.length - 1) + value.last()
            else -> "•".repeat(value.length)
        }
        return TransformedText(AnnotatedString(masked), OffsetMapping.Identity)
    }
}

@Composable
fun PasswordFieldWithReveal(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var showAll by remember { mutableStateOf(false) }
    var revealLast by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value.isNotEmpty()) {
            revealLast = true
            delay(1000)
            revealLast = false
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (showAll) {
            VisualTransformation.None
        } else {
            RevealLastCharacterTransformation(revealLast)
        },
        trailingIcon = {
            IconButton(onClick = { showAll = !showAll }) {
                Icon(
                    imageVector = if (showAll) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showAll) stringResource(R.string.password_hide_cd) else stringResource(R.string.password_show_cd),
                )
            }
        },
        modifier = modifier,
    )
}
