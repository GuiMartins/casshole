package com.casshole.ui.comparison

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.casshole.R
import com.casshole.data.local.CategoryComparison
import com.casshole.data.local.MonthlyComparison
import com.casshole.data.local.MonthlySummary
import com.casshole.data.local.ProjectedMonth
import com.casshole.ui.components.AdaptiveScreen
import com.casshole.ui.components.InstallmentBadge
import com.casshole.ui.components.formatCurrency
import com.casshole.ui.theme.CategoryIcon

/** Projected months added to the chart - enough to show where the installments are heading, not a 12-month wall of bars. */
private const val PROJECTED_MONTHS_IN_CHART = 3

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(viewModel: ComparisonViewModel = viewModel()) {
    val periodsState by viewModel.periodsState.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val comparisonState by viewModel.comparisonState.collectAsState()
    val amountsHidden by viewModel.amountsHidden.collectAsState()
    val projection by viewModel.projection.collectAsState()

    AdaptiveScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.comparison_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            when (val state = periodsState) {
                is PeriodsState.Loading -> item {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                is PeriodsState.Error -> item {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                is PeriodsState.Available -> {
                    if (state.periods.isEmpty()) {
                        item { Text(stringResource(R.string.empty_no_invoices)) }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(R.string.comparison_select_periods),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row {
                                    TextButton(onClick = viewModel::selectAll) {
                                        Text(stringResource(R.string.comparison_select_all))
                                    }
                                    TextButton(onClick = viewModel::clearSelection) {
                                        Text(stringResource(R.string.comparison_clear_selection))
                                    }
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.periods.forEach { period ->
                                    FilterChip(
                                        selected = period in selected,
                                        onClick = { viewModel.toggleSelection(period) },
                                        label = { Text(period.toString()) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when (val state = comparisonState) {
                is ComparisonUiState.Comparing -> item {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                is ComparisonUiState.Error -> item {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                is ComparisonUiState.Idle -> item {
                    Text(
                        text = stringResource(R.string.comparison_select_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                is ComparisonUiState.Result -> {
                    val lastPeriod = (periodsState as? PeriodsState.Available)?.periods?.lastOrNull()
                    val lastCompared = state.comparison.months.lastOrNull()
                    // Projection only joins the chart when the newest imported
                    // month is on screen - glued after an older selection it
                    // would suggest a gap in the timeline that isn't there.
                    val chartProjection = if (
                        lastPeriod != null && lastCompared != null &&
                        lastPeriod.month == lastCompared.referenceMonth && lastPeriod.year == lastCompared.referenceYear
                    ) {
                        projection.take(PROJECTED_MONTHS_IN_CHART)
                    } else {
                        emptyList()
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        ChartCard(
                            comparison = state.comparison,
                            chartProjection = chartProjection,
                            amountsHidden = amountsHidden,
                        )
                    }
                    item { StatsCard(comparison = state.comparison, amountsHidden = amountsHidden) }
                    if (state.comparison.months.size >= 2) {
                        item { CategoryComparisonCard(comparison = state.comparison, amountsHidden = amountsHidden) }
                    }
                    items(state.comparison.months) { month ->
                        MonthlySummaryCard(month, amountsHidden = amountsHidden)
                    }
                }
            }

            if (projection.isNotEmpty()) {
                item { ProjectionCard(projection = projection, amountsHidden = amountsHidden) }
            }
        }
    }
}

@Composable
private fun ChartCard(
    comparison: MonthlyComparison,
    chartProjection: List<ProjectedMonth>,
    amountsHidden: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BarChart(
                data = comparison.months.map {
                    BarData("%02d/%d".format(it.referenceMonth, it.referenceYear), it.totalSpent)
                } + chartProjection.map {
                    BarData(
                        stringResource(R.string.comparison_chart_projected_label, it.referenceMonth, it.referenceYear),
                        it.total,
                        projected = true,
                    )
                },
                amountsHidden = amountsHidden,
                modifier = Modifier.fillMaxWidth(),
            )

            if (chartProjection.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendSwatch(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.comparison_chart_legend_actual),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, end = 12.dp),
                    )
                    LegendSwatch(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    Text(
                        text = stringResource(R.string.comparison_chart_legend_projected),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }

            comparison.totalPercentageChange?.let { change ->
                val sign = if (change >= 0) "+" else ""
                Text(
                    text = stringResource(R.string.comparison_variation, sign, change),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = changeColor(change),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 14.dp, height = 8.dp)
            .background(color, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun StatsCard(comparison: MonthlyComparison, amountsHidden: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(R.string.comparison_stat_total),
                    value = formatCurrency(comparison.total, amountsHidden),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.comparison_stat_average),
                    value = formatCurrency(comparison.average, amountsHidden),
                    modifier = Modifier.weight(1f),
                )
            }
            if (comparison.months.size >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    comparison.highest?.let {
                        StatTile(
                            label = stringResource(R.string.comparison_stat_highest),
                            value = formatCurrency(it.totalSpent, amountsHidden),
                            caption = "%02d/%d".format(it.referenceMonth, it.referenceYear),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    comparison.lowest?.let {
                        StatTile(
                            label = stringResource(R.string.comparison_stat_lowest),
                            value = formatCurrency(it.totalSpent, amountsHidden),
                            caption = "%02d/%d".format(it.referenceMonth, it.referenceYear),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryComparisonCard(comparison: MonthlyComparison, amountsHidden: Boolean) {
    val firstLabel = comparison.months.first().let { "%02d/%d".format(it.referenceMonth, it.referenceYear) }
    val lastLabel = comparison.months.last().let { "%02d/%d".format(it.referenceMonth, it.referenceYear) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.comparison_by_category_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.comparison_by_category_description, firstLabel, lastLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            comparison.byCategory.forEach { category ->
                CategoryComparisonRow(category = category, amountsHidden = amountsHidden)
            }
        }
    }
}

@Composable
private fun CategoryComparisonRow(category: CategoryComparison, amountsHidden: Boolean) {
    val first = category.totalsByPeriod.first()
    val last = category.totalsByPeriod.last()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(category = category.category, size = 30.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 8.dp),
        ) {
            Text(
                text = category.category,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatCurrency(first, amountsHidden) + " → " + formatCurrency(last, amountsHidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ChangeBadge(category = category)
    }
}

/**
 * Percentage when there's a first-period baseline to divide by; otherwise the
 * plain "new"/"stopped" label, which is what a category appearing from zero
 * (or disappearing) actually means.
 */
@Composable
private fun ChangeBadge(category: CategoryComparison) {
    val first = category.totalsByPeriod.first()
    val last = category.totalsByPeriod.last()
    val percentage = category.percentageChange

    val (text, color) = when {
        percentage != null -> {
            val sign = if (percentage >= 0) "+" else ""
            stringResource(R.string.comparison_category_percentage, sign, percentage) to changeColor(percentage)
        }
        first == 0.0 && last > 0.0 -> stringResource(R.string.comparison_category_new) to MaterialTheme.colorScheme.error
        first > 0.0 && last == 0.0 -> stringResource(R.string.comparison_category_stopped) to MaterialTheme.colorScheme.tertiary
        else -> stringResource(R.string.comparison_category_stable) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun changeColor(change: Double): Color = if (change > 0) {
    MaterialTheme.colorScheme.error
} else {
    MaterialTheme.colorScheme.tertiary
}

@Composable
private fun ProjectionCard(projection: List<ProjectedMonth>, amountsHidden: Boolean) {
    val expanded = remember { mutableStateMapOf<Int, Boolean>() }
    val total = projection.sumOf { it.total }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text(
                    text = stringResource(R.string.comparison_projection_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                text = stringResource(R.string.comparison_projection_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.comparison_projection_total_prefix) + formatCurrency(total, amountsHidden),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 12.dp),
            )

            projection.forEach { month ->
                val key = month.referenceYear * 12 + month.referenceMonth
                val isExpanded = expanded[key] ?: false

                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded[key] = !isExpanded }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.comparison_projection_month,
                                month.referenceMonth, month.referenceYear, month.installments.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = formatCurrency(month.total, amountsHidden),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        month.installments.forEach { installment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 28.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                InstallmentBadge(
                                    currentInstallment = installment.installmentNumber,
                                    totalInstallments = installment.totalInstallments,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                                Text(
                                    text = installment.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                )
                                Text(
                                    text = formatCurrency(installment.amount, amountsHidden),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(summary: MonthlySummary, amountsHidden: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.comparison_month_total_prefix,
                    summary.referenceMonth, summary.referenceYear,
                ) + formatCurrency(summary.totalSpent, amountsHidden),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            summary.byCategory.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(category = category.category, size = 26.dp)
                        Text(
                            text = category.category,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(formatCurrency(category.total, amountsHidden), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
