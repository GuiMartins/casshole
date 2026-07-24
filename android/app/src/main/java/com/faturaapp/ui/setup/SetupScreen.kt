package com.faturaapp.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SetupScreen(
    onSetupConcluido: () -> Unit,
    viewModel: SetupViewModel = viewModel(),
) {
    val url by viewModel.url.collectAsState()
    val testState by viewModel.testState.collectAsState()
    val setupConcluido by viewModel.setupConcluido.collectAsState()

    LaunchedEffect(setupConcluido) {
        if (setupConcluido) onSetupConcluido()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }

        Text(
            text = "Fatura App",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Configure o endereço do seu servidor pra começar",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
        )

        OutlinedTextField(
            value = url,
            onValueChange = viewModel::onUrlChange,
            label = { Text("URL do backend") },
            placeholder = { Text("ex: http://100.x.x.x:8000") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        when (val estado = testState) {
            is ConnectionTestState.Failure -> Text(
                text = estado.mensagem,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
            is ConnectionTestState.Success -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Conectado com sucesso!",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            else -> {}
        }

        Button(
            onClick = viewModel::testarConexao,
            enabled = testState != ConnectionTestState.Testing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
        ) {
            if (testState == ConnectionTestState.Testing) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text("Testar conexão")
        }

        if (testState == ConnectionTestState.Success) {
            Button(
                onClick = viewModel::continuar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("Continuar")
            }
        }
    }
}
