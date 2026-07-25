package com.faturaapp.ui.configuracoes

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
import com.faturaapp.ui.components.TelaAdaptavel

@Composable
fun ConfiguracoesScreen(
    onAbrirSenhas: () -> Unit,
    onAbrirCategoriasPersonalizadas: () -> Unit,
    viewModel: ConfiguracoesViewModel = viewModel(),
) {
    val temaPreferido by viewModel.temaPreferido.collectAsState()

    TelaAdaptavel {
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
        OpcaoTema(
            label = "Sistema",
            icone = Icons.Filled.BrightnessAuto,
            selecionado = temaPreferido == PreferencesRepository.TEMA_SISTEMA,
            onClick = { viewModel.selecionarTema(PreferencesRepository.TEMA_SISTEMA) },
        )
        OpcaoTema(
            label = "Claro",
            icone = Icons.Filled.LightMode,
            selecionado = temaPreferido == PreferencesRepository.TEMA_CLARO,
            onClick = { viewModel.selecionarTema(PreferencesRepository.TEMA_CLARO) },
        )
        OpcaoTema(
            label = "Escuro",
            icone = Icons.Filled.DarkMode,
            selecionado = temaPreferido == PreferencesRepository.TEMA_ESCURO,
            onClick = { viewModel.selecionarTema(PreferencesRepository.TEMA_ESCURO) },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text(
            text = "Segurança",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LinhaNavegavel(
            icone = Icons.Filled.Lock,
            titulo = "Senhas padrão de PDF",
            subtitulo = "Tentar abrir faturas protegidas automaticamente",
            onClick = onAbrirSenhas,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

        Text(
            text = "Categorização",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LinhaNavegavel(
            icone = Icons.Filled.EditNote,
            titulo = "Categorização manual",
            subtitulo = "Correções que o app já aplica automaticamente",
            onClick = onAbrirCategoriasPersonalizadas,
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
    }
}

@Composable
private fun OpcaoTema(
    label: String,
    icone: ImageVector,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selecionado, onClick = onClick)
        Icon(
            imageVector = icone,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinhaNavegavel(
    icone: ImageVector,
    titulo: String,
    subtitulo: String,
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
                imageVector = icone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column {
                Text(titulo, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
    }
}
