package com.casshole.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.casshole.R

/**
 * "3/10" pill shown next to every installment purchase - the installment
 * count used to be buried in the small grey line under the description, where
 * it was easy to miss that a charge repeats for months.
 *
 * Renders nothing for a single-charge purchase (no installments to show).
 */
@Composable
fun InstallmentBadge(
    currentInstallment: Int?,
    totalInstallments: Int?,
    modifier: Modifier = Modifier,
) {
    if (totalInstallments == null || totalInstallments <= 1) return

    val label = if (currentInstallment != null) {
        stringResource(R.string.installment_badge, currentInstallment, totalInstallments)
    } else {
        stringResource(R.string.installment_badge_total_only, totalInstallments)
    }
    val description = if (currentInstallment != null) {
        stringResource(R.string.installment_badge_cd, currentInstallment, totalInstallments)
    } else {
        stringResource(R.string.installment_badge_total_only_cd, totalInstallments)
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics { contentDescription = description },
    )
}
