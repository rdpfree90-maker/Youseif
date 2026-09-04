package com.youseif.playerpro

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.youseif.playerpro.ui.navigation.AppNavigation
import com.youseif.playerpro.ui.theme.YouseifPlayerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = try {
            runBlocking {
                (newBase.applicationContext as? YouseifPlayerApp)
                    ?.settingsRepository
                    ?.language
                    ?.first()
                    ?: "en"
            }
        } catch (_: Exception) {
            "en"
        }
        super.attachBaseContext(applyLanguage(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Handle VIEW intent
        val intentUrl = intent?.data?.toString()

        setContent {
            val app = application as YouseifPlayerApp
            val themePref by app.settingsRepository.theme.collectAsState(initial = "dark")
            val darkTheme = themePref != "light"

            YouseifPlayerTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var localeKey by remember { mutableStateOf(0) }
                    // Force recomposition on language change by key
                    androidx.compose.runtime.key(localeKey) {
                        AppNavigation(
                            onLanguageChanged = { lang ->
                                // Persist already done in SettingsViewModel.
                                // Recreate activity so attachBaseContext picks new language
                                // without flipping player layout (layoutDirection handled carefully).
                                recreate()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    companion object {
        fun applyLanguage(context: Context, language: String): Context {
            val locale = when (language) {
                "ar" -> Locale("ar")
                else -> Locale.ENGLISH
            }
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            // Locale affects string resources (Arabic/English text).
            // Player controls, gestures, and bottom navigation FORCE LTR in Compose
            // via CompositionLocalProvider(LocalLayoutDirection provides Ltr).
            // Do NOT call setLayoutDirection here in a way that is required for player —
            // player UI ignores RTL for control order on purpose.
            return context.createConfigurationContext(config)
        }
    }
}
