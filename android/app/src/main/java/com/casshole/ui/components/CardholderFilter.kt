package com.casshole.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.casshole.R
import com.casshole.data.local.CardholderSummary

/** Display name of a cardholder group; the blank group is the one whose invoice didn't name anyone. */
@Composable
fun cardholderLabel(summary: CardholderSummary): String =
    summary.name.ifBlank { stringResource(R.string.cardholder_unidentified) }

/**
 * "Everyone" + one chip per cardholder (account holder and additional cards).
 * [selectedKey] null means no filter. Callers only show it when there's more
 * than one cardholder - with a single one the filter would be a no-op.
 */
@Composable
fun CardholderFilterChips(
    cardholders: List<CardholderSummary>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedKey == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.cardholder_all)) },
        )
        cardholders.forEach { cardholder ->
            FilterChip(
                selected = selectedKey == cardholder.key,
                onClick = { onSelect(if (selectedKey == cardholder.key) null else cardholder.key) },
                label = { Text(cardholderLabel(cardholder)) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            )
        }
    }
}
