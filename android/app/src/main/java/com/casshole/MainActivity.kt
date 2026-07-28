package com.casshole

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.casshole.data.PreferencesRepository
import com.casshole.data.SharedFileHolder
import com.casshole.navigation.AppNavigation
import com.casshole.ui.components.LocalWindowWidthSizeClass
import com.casshole.ui.onboarding.OnboardingScreen
import com.casshole.ui.theme.CassholeTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
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
            val onboardingCompleted by preferencesRepository.onboardingCompleted.collectAsState(
                initial = null,
            )
            val coroutineScope = rememberCoroutineScope()

            CassholeTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass,
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        when (onboardingCompleted) {
                            null -> {} // DataStore still loading its first value
                            false -> OnboardingScreen(
                                onFinished = {
                                    coroutineScope.launch {
                                        preferencesRepository.setOnboardingCompleted(true)
                                    }
                                },
                            )
                            true -> AppNavigation()
                        }
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
