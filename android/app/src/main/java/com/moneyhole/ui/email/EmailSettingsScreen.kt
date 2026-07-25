package com.moneyhole.ui.email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyhole.R
import com.moneyhole.ui.components.AdaptiveScreen
import com.moneyhole.ui.components.PasswordFieldWithReveal

@Composable
fun EmailSettingsScreen(viewModel: EmailSettingsViewModel = viewModel()) {
    val address by viewModel.address.collectAsState()
    val appPassword by viewModel.appPassword.collectAsState()
    val imapHost by viewModel.imapHost.collectAsState()
    val fetchState by viewModel.fetchState.collectAsState()

    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(R.string.email_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = stringResource(R.string.email_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = viewModel::onAddressChange,
                        label = { Text(stringResource(R.string.email_address_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PasswordFieldWithReveal(
                        value = appPassword,
                        onValueChange = viewModel::onAppPasswordChange,
                        label = stringResource(R.string.email_app_password_label),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    OutlinedTextField(
                        value = imapHost,
                        onValueChange = viewModel::onImapHostChange,
                        label = { Text(stringResource(R.string.email_imap_host_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )

                    Button(
                        onClick = viewModel::save,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text(stringResource(R.string.action_save))
                    }

                    OutlinedButton(
                        onClick = viewModel::fetchNow,
                        enabled = fetchState != EmailFetchState.Fetching,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        if (fetchState == EmailFetchState.Fetching) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(18.dp))
                        }
                        Text(stringResource(R.string.email_fetch_now))
                    }

                    when (val state = fetchState) {
                        is EmailFetchState.Done -> Text(
                            text = stringResource(
                                R.string.email_result,
                                state.result.imported, state.result.duplicates, state.result.failed,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        is EmailFetchState.Error -> Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}
