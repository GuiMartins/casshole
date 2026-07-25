package com.moneyhole.ui.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyhole.data.local.MonthlySummary
import com.moneyhole.ui.components.AdaptiveScreen
import com.moneyhole.ui.theme.CategoryIcon

@Composable
fun ComparisonScreen(viewModel: ComparisonViewModel = viewModel()) {
    val periodsState by viewModel.periodsState.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val comparisonState by viewModel.comparisonState.collectAsState()

    AdaptiveScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    text = "Comparar meses",
                    style = MaterialTheme.typography.headlineSmall,
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
                        item { Text("Nenhuma fatura enviada ainda") }
                    } else {
                        items(state.periods) { period ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = period in selected,
                                    onCheckedChange = { viewModel.toggleSelection(period) },
                                )
                                Text(period.toString())
                            }
                        }
                        item {
                            Button(
                                onClick = viewModel::compare,
                                enabled = comparisonState != ComparisonUiState.Comparing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                            ) {
                                Text("Comparar")
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
                is ComparisonUiState.Result -> {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        BarChart(
                            data = state.comparison.months.map {
                                BarData("%02d/%d".format(it.referenceMonth, it.referenceYear), it.totalSpent)
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        )
                        state.comparison.totalPercentageChange?.let { change ->
                            val sign = if (change >= 0) "+" else ""
                            Text(
                                text = "Variação do primeiro ao último mês selecionado: $sign%.1f%%".format(change),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (change > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                        }
                    }
                    items(state.comparison.months) { month ->
                        MonthlySummaryCard(month)
                    }
                }
                is ComparisonUiState.Idle -> {}
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(summary: MonthlySummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "%02d/%d — Total: R$ %.2f".format(
                    summary.referenceMonth, summary.referenceYear, summary.totalSpent
                ),
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
                    Text("R$ %.2f".format(category.total), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
