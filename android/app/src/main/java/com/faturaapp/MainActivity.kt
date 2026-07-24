package com.faturaapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.faturaapp.data.PreferencesRepository
import com.faturaapp.data.SharedFileHolder
import com.faturaapp.navigation.AppNavigation
import com.faturaapp.ui.theme.FaturaAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        setContent {
            val preferencesRepository = remember { PreferencesRepository(applicationContext) }
            val temaPreferido by preferencesRepository.temaPreferido.collectAsState(
                initial = PreferencesRepository.TEMA_SISTEMA,
            )
            val temaEscuro = when (temaPreferido) {
                PreferencesRepository.TEMA_CLARO -> false
                PreferencesRepository.TEMA_ESCURO -> true
                else -> null
            }

            FaturaAppTheme(temaEscuro = temaEscuro) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type != "application/pdf") return

        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        SharedFileHolder.pendingUri = uri
    }
}
