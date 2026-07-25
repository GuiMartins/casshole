package com.faturaapp.ui.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.SharedFileHolder
import com.faturaapp.ui.components.TelaAdaptavel

@Composable
fun UploadScreen(
    onFaturaEnviada: () -> Unit,
    viewModel: UploadViewModel = viewModel(),
) {
    val arquivoSelecionado by viewModel.arquivoSelecionado.collectAsState()
    val senha by viewModel.senha.collectAsState()
    val salvarSenha by viewModel.salvarSenha.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    val seletorArquivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::selecionarArquivo) }

    LaunchedEffect(Unit) {
        SharedFileHolder.consume()?.let(viewModel::selecionarArquivo)
    }

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Sucesso) onFaturaEnviada()
    }

    TelaAdaptavel(alignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Enviar fatura",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Selecione o PDF da fatura (Nubank, Itaú, Bradesco ou Mercado Pago)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            Text(
                text = arquivoSelecionado?.nome ?: "Nenhum arquivo selecionado",
                style = MaterialTheme.typography.bodyLarge,
            )

            OutlinedButton(
                onClick = { seletorArquivo.launch(arrayOf("application/pdf")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text("Escolher arquivo")
            }

            OutlinedTextField(
                value = senha,
                onValueChange = viewModel::onSenhaChange,
                label = { Text("Senha do PDF (opcional)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Switch(
                    checked = salvarSenha,
                    onCheckedChange = viewModel::onSalvarSenhaChange,
                    enabled = senha.isNotBlank(),
                )
                Text(
                    text = "Salvar como senha padrão, pra abrir futuras faturas automaticamente",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            when (val estado = uploadState) {
                is UploadState.Erro -> Text(
                    text = estado.mensagem,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
                else -> {}
            }

            Button(
                onClick = viewModel::enviarFatura,
                enabled = arquivoSelecionado != null && uploadState != UploadState.Enviando,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                if (uploadState == UploadState.Enviando) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text("Enviar fatura")
            }
        }
    }
}
