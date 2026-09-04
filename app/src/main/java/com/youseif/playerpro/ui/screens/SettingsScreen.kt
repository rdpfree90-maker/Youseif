package com.youseif.playerpro.ui.screens

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youseif.playerpro.BuildConfig
import com.youseif.playerpro.R
import com.youseif.playerpro.YouseifPlayerApp
import com.youseif.playerpro.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLanguageChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as YouseifPlayerApp
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.settingsRepository)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle(stringResource(R.string.language))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        viewModel.setLanguage("en")
                        onLanguageChanged("en")
                    }
                ) {
                    Text(
                        stringResource(R.string.english) +
                            if (state.language == "en") " ✓" else ""
                    )
                }
                TextButton(
                    onClick = {
                        viewModel.setLanguage("ar")
                        onLanguageChanged("ar")
                    }
                ) {
                    Text(
                        stringResource(R.string.arabic) +
                            if (state.language == "ar") " ✓" else ""
                    )
                }
            }
            Text(
                text = "Language changes text only. Player layout and control order stay fixed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SectionTitle(stringResource(R.string.web_player_settings))

            OutlinedTextField(
                value = state.customUserAgent,
                onValueChange = viewModel::setCustomUserAgent,
                label = { Text(stringResource(R.string.custom_user_agent)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = state.defaultReferer,
                onValueChange = viewModel::setDefaultReferer,
                label = { Text(stringResource(R.string.default_referer)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                singleLine = true
            )

            SettingSwitch(
                title = stringResource(R.string.enable_cookies),
                checked = state.enableCookies,
                onCheckedChange = viewModel::setEnableCookies
            )
            SettingSwitch(
                title = stringResource(R.string.enable_cache),
                checked = state.enableCache,
                onCheckedChange = viewModel::setEnableCache
            )
            SettingSwitch(
                title = stringResource(R.string.enable_javascript),
                checked = state.enableJavascript,
                onCheckedChange = viewModel::setEnableJavascript
            )
            SettingSwitch(
                title = stringResource(R.string.autoplay),
                checked = state.autoplay,
                onCheckedChange = viewModel::setAutoplay
            )
            SettingSwitch(
                title = stringResource(R.string.hardware_acceleration),
                checked = state.hardwareAcceleration,
                onCheckedChange = viewModel::setHardwareAcceleration
            )
            SettingSwitch(
                title = stringResource(R.string.data_saver),
                checked = state.dataSaver,
                onCheckedChange = viewModel::setDataSaver
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SectionTitle(stringResource(R.string.privacy))

            TextButton(
                onClick = {
                    try {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        Toast.makeText(context, "Cookies cleared", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                    }
                }
            ) {
                Text(stringResource(R.string.clear_cookies))
            }
            TextButton(
                onClick = {
                    try {
                        WebStorage.getInstance().deleteAllData()
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                    }
                }
            ) {
                Text(stringResource(R.string.clear_cache))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SectionTitle(stringResource(R.string.about))
            Text(
                text = "${stringResource(R.string.app_name)}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.slogan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
