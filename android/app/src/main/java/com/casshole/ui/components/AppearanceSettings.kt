package com.casshole.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import com.casshole.R
import com.casshole.data.PreferencesRepository

/**
 * Theme + dashboard-summary-display pickers. Shared between the Settings
 * screen and the onboarding "Appearance" step - same controls, same
 * PreferencesRepository-backed state either way.
 */
@Composable
fun AppearanceSettings(
    preferredTheme: String,
    onSelectTheme: (String) -> Unit,
    summaryDisplayMode: String,
    onSelectSummaryDisplayMode: (String) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.settings_appearance),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        RadioOption(
            label = stringResource(R.string.theme_system),
            icon = Icons.Filled.BrightnessAuto,
            selected = preferredTheme == PreferencesRepository.THEME_SYSTEM,
            onClick = { onSelectTheme(PreferencesRepository.THEME_SYSTEM) },
        )
        RadioOption(
            label = stringResource(R.string.theme_light),
            icon = Icons.Filled.LightMode,
            selected = preferredTheme == PreferencesRepository.THEME_LIGHT,
            onClick = { onSelectTheme(PreferencesRepository.THEME_LIGHT) },
        )
        RadioOption(
            label = stringResource(R.string.theme_dark),
            icon = Icons.Filled.DarkMode,
            selected = preferredTheme == PreferencesRepository.THEME_DARK,
            onClick = { onSelectTheme(PreferencesRepository.THEME_DARK) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text(
            text = stringResource(R.string.settings_summary_display),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        RadioOption(
            label = stringResource(R.string.summary_display_numbers),
            icon = Icons.Filled.BarChart,
            selected = summaryDisplayMode == PreferencesRepository.SUMMARY_DISPLAY_NUMBERS,
            onClick = { onSelectSummaryDisplayMode(PreferencesRepository.SUMMARY_DISPLAY_NUMBERS) },
        )
        RadioOption(
            label = stringResource(R.string.summary_display_pie_chart),
            icon = Icons.Filled.PieChart,
            selected = summaryDisplayMode == PreferencesRepository.SUMMARY_DISPLAY_PIE_CHART,
            onClick = { onSelectSummaryDisplayMode(PreferencesRepository.SUMMARY_DISPLAY_PIE_CHART) },
        )
    }
}

@Composable
fun RadioOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
