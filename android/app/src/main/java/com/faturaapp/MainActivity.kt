package com.faturaapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.faturaapp.data.PreferencesRepository
import com.faturaapp.data.SharedFileHolder
import com.faturaapp.navigation.AppNavigation
import com.faturaapp.ui.components.LocalWindowWidthSizeClass
import com.faturaapp.ui.theme.FaturaAppTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        setContent {
            val preferencesRepository = remember { PreferencesRepository(applicationContext) }
            val preferredTheme by preferencesRepository.preferredTheme.collectAsState(
                initial = PreferencesRepository.THEME_SYSTEM,
            )
            val darkTheme = when (preferredTheme) {
                PreferencesRepository.THEME_LIGHT -> false
                PreferencesRepository.THEME_DARK -> true
                else -> null
            }
            val windowSizeClass = calculateWindowSizeClass(this)

            FaturaAppTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation()
                    }
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
