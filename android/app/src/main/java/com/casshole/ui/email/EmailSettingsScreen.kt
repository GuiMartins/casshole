package com.casshole.ui.email

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.casshole.R
import com.casshole.data.email.EmailFetchState
import com.casshole.ui.components.AdaptiveScreen
import com.casshole.ui.components.PasswordFieldWithReveal

@Composable
fun EmailSettingsScreen(viewModel: EmailSettingsViewModel = viewModel()) {
    val address by viewModel.address.collectAsState()
    val appPassword by viewModel.appPassword.collectAsState()
    val imapHost by viewModel.imapHost.collectAsState()
    val fetchState by viewModel.fetchState.collectAsState()
    val validationError by viewModel.validationError.collectAsState()
    var isEditing by remember { mutableStateOf(address.isBlank() || appPassword.isBlank()) }

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

            val context = LocalContext.current
            var showHelpDialog by remember { mutableStateOf(false) }

            TextButton(
                onClick = { showHelpDialog = true },
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(stringResource(R.string.email_help_link))
            }

            if (showHelpDialog) {
                AlertDialog(
                    onDismissRequest = { showHelpDialog = false },
                    title = { Text(stringResource(R.string.email_help_title)) },
                    text = { Text(stringResource(R.string.email_help_steps)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords"))
                                )
                                showHelpDialog = false
                            },
                        ) {
                            Text(stringResource(R.string.email_help_open_app_passwords))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showHelpDialog = false }) {
                            Text(stringResource(R.string.action_close))
                        }
                    },
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
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
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = address, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = imapHost,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { isEditing = true }) {
                                Text(stringResource(R.string.email_edit_config))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.saveAndFetch()
                            if (address.isNotBlank() && appPassword.isNotBlank()) {
                                isEditing = false
                            }
                        },
                        enabled = fetchState !is EmailFetchState.Fetching,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        if (fetchState is EmailFetchState.Fetching) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(18.dp))
                        }
                        Text(stringResource(R.string.email_fetch_now))
                    }

                    validationError?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    when (val state = fetchState) {
                        is EmailFetchState.Fetching -> if (state.total > 0) {
                            Text(
                                text = stringResource(
                                    R.string.email_fetch_progress,
                                    state.processed, state.total,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        is EmailFetchState.Done -> Column {
                            Text(
                                text = stringResource(
                                    R.string.email_result,
                                    state.result.imported, state.result.duplicates, state.result.failed,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            if (state.result.failedPasswords > 0) {
                                Text(
                                    text = stringResource(
                                        R.string.email_result_password_hint,
                                        state.result.failedPasswords,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            if (state.result.unsupportedBanks.isNotEmpty()) {
                                val bankNames = state.result.unsupportedBanks.joinToString { bank ->
                                    bank.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
                                }
                                Text(
                                    text = stringResource(R.string.email_result_unsupported_bank_hint, bankNames),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
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
