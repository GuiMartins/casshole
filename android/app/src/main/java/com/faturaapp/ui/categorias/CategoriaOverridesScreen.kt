package com.faturaapp.ui.categorias

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.faturaapp.data.local.entity.CategoriaOverrideEntity
import com.faturaapp.ui.theme.CategoriaIcone

@Composable
fun CategoriaOverridesScreen(viewModel: CategoriaOverridesViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "Categorização manual",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = "Toda vez que você corrige a categoria de uma transação, o app guarda " +
                    "essa descrição aqui e já aplica a mesma categoria automaticamente nas " +
                    "próximas faturas com o mesmo estabelecimento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            )
        }

        when (val estado = state) {
            is CategoriaOverridesState.Carregando -> item {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            is CategoriaOverridesState.Erro -> item {
                Text(estado.mensagem, color = MaterialTheme.colorScheme.error)
            }
            is CategoriaOverridesState.Carregado -> {
                if (estado.overrides.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhuma correção manual ainda",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(estado.overrides, key = { it.id }) { override ->
                        LinhaOverride(override, onRemover = viewModel::remover)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaOverride(override: CategoriaOverrideEntity, onRemover: (Long) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoriaIcone(categoria = override.categoria, tamanho = 32.dp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(override.descricao, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = override.categoria,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { onRemover(override.id) }) {
                Icon(Icons.Filled.Close, contentDescription = "Remover categorização manual")
            }
        }
    }
}
