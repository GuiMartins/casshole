package com.casshole

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
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
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import com.casshole.data.PreferencesRepository
import com.casshole.navigation.AppNavigation
import com.casshole.ui.components.LocalWindowWidthSizeClass
import com.casshole.ui.onboarding.OnboardingScreen
import com.casshole.ui.theme.CassholeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // A PDF shared into THIS activity instance. Deliberately not a process-wide holder:
    // sharing from another app usually creates a new instance in the sender's task while
    // an older one may still be alive in ours, and only the instance that received the
    // Intent holds the read grant for the shared URI.
    private val sharedPdf = MutableStateFlow<Uri?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This app deliberately allows screenshots - it never sets FLAG_SECURE.
        // Clearing it explicitly documents that intent and neutralizes any
        // dependency that might turn it on. A block coming from the device
        // itself (work profile, Samsung Secure Folder, private space) is not
        // affected by this: there the system enforces it above the app.
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        } else {
            // Not consumed yet (e.g. rotated during onboarding). getIntent() would still be
            // the share Intent after recreation, so re-reading it would import twice.
            sharedPdf.value = BundleCompat.getParcelable(savedInstanceState, KEY_SHARED_PDF, Uri::class.java)
        }
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
            val sharedPdfUri by sharedPdf.collectAsState()

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
                            true -> AppNavigation(
                                sharedPdf = sharedPdfUri,
                                onSharedPdfHandled = { sharedPdf.value = null },
                            )
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
        val uri = when (intent?.action) {
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        } ?: return
        sharedPdf.value = uri
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_SHARED_PDF, sharedPdf.value)
    }

    private companion object {
        const val KEY_SHARED_PDF = "shared_pdf"
    }
}
