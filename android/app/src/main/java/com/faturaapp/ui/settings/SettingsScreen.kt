package com.faturaapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.PreferencesRepository
import com.faturaapp.ui.components.AdaptiveScreen

@Composable
fun SettingsScreen(
    onOpenPasswords: () -> Unit,
    onOpenCategoryOverrides: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val preferredTheme by viewModel.preferredTheme.collectAsState()

    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "Configurações",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Text(
                text = "Aparência",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 28.dp, bottom = 4.dp),
            )
            ThemeOption(
                label = "Sistema",
                icon = Icons.Filled.BrightnessAuto,
                selected = preferredTheme == PreferencesRepository.THEME_SYSTEM,
                onClick = { viewModel.selectTheme(PreferencesRepository.THEME_SYSTEM) },
            )
            ThemeOption(
                label = "Claro",
                icon = Icons.Filled.LightMode,
                selected = preferredTheme == PreferencesRepository.THEME_LIGHT,
                onClick = { viewModel.selectTheme(PreferencesRepository.THEME_LIGHT) },
            )
            ThemeOption(
                label = "Escuro",
                icon = Icons.Filled.DarkMode,
                selected = preferredTheme == PreferencesRepository.THEME_DARK,
                onClick = { viewModel.selectTheme(PreferencesRepository.THEME_DARK) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text(
                text = "Segurança",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            NavigableRow(
                icon = Icons.Filled.Lock,
                title = "Senhas padrão de PDF",
                subtitle = "Tentar abrir faturas protegidas automaticamente",
                onClick = onOpenPasswords,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text(
                text = "Categorização",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            NavigableRow(
                icon = Icons.Filled.EditNote,
                title = "Categorização manual",
                subtitle = "Correções que o app já aplica automaticamente",
                onClick = onOpenCategoryOverrides,
            )
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ThemeOption(
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

@Composable
private fun NavigableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
    }
}
