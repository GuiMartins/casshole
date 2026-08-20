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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    val periodLabels = comparison.months.map { "%02d/%d".format(it.referenceMonth, it.referenceYear) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val biggestRise = comparison.byCategory.maxByOrNull { it.change }?.takeIf { it.change > 0 }
    val biggestDrop = comparison.byCategory.minByOrNull { it.change }?.takeIf { it.change < 0 }

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
                text = stringResource(R.string.comparison_by_category_description, periodLabels.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (biggestRise != null || biggestDrop != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    biggestRise?.let {
                        HighlightTile(
                            label = stringResource(R.string.comparison_highlight_biggest_rise),
                            category = it,
                            amountsHidden = amountsHidden,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    biggestDrop?.let {
                        HighlightTile(
                            label = stringResource(R.string.comparison_highlight_biggest_drop),
                            category = it,
                            amountsHidden = amountsHidden,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            comparison.byCategory.forEach { category ->
                CategoryComparisonRow(
                    category = category,
                    periodLabels = periodLabels,
                    amountsHidden = amountsHidden,
                    expanded = expanded[category.category] ?: false,
                    onToggle = { expanded[category.category] = !(expanded[category.category] ?: false) },
                )
            }
        }
    }
}

@Composable
private fun HighlightTile(
    label: String,
    category: CategoryComparison,
    amountsHidden: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(end = 8.dp)
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(category = category.category, size = 20.dp)
            Text(
                text = category.category,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        val sign = if (category.change > 0) "+" else "-"
        Text(
            text = sign + formatCurrency(kotlin.math.abs(category.change), amountsHidden),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

/**
 * Collapsed the row already shows every selected month as a mini bar - the
 * first → last pair alone hid whatever happened in between. Tapping opens the
 * month-by-month breakdown.
 */
@Composable
private fun CategoryComparisonRow(
    category: CategoryComparison,
    periodLabels: List<String>,
    amountsHidden: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val first = category.totalsByPeriod.first()
    val last = category.totalsByPeriod.last()
    val peak = category.totalsByPeriod.maxOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(top = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(category = category.category, size = 30.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.category,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = stringResource(R.string.comparison_category_share, category.share),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                if (periodLabels.size > 2) {
                    MiniBars(
                        values = category.totalsByPeriod,
                        peak = peak,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
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

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 40.dp, top = 6.dp, bottom = 4.dp)) {
                category.totalsByPeriod.forEachIndexed { index, value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = periodLabels[index],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(58.dp),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .padding(end = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (peak > 0) (value / peak).toFloat() else 0f)
                                    .height(6.dp)
                                    .background(
                                        if (index == category.peakPeriodIndex) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                        },
                                        RoundedCornerShape(3.dp),
                                    ),
                            )
                        }
                        Text(
                            text = formatCurrency(value, amountsHidden),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (index == category.peakPeriodIndex) FontWeight.Medium else FontWeight.Normal,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.comparison_category_range_total_prefix) +
                        formatCurrency(category.total, amountsHidden) +
                        stringResource(R.string.comparison_category_range_average_infix) +
                        formatCurrency(category.average, amountsHidden),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** One slim bar per selected month, so the whole range reads at a glance without opening the row. */
@Composable
private fun MiniBars(values: List<Double>, peak: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { value ->
            val fraction = if (peak > 0) (value / peak).toFloat() else 0f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f), RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), RoundedCornerShape(2.dp)),
                )
            }
        }
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
