package com.youseif.playerpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.youseif.playerpro.YouseifPlayerApp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.youseif.playerpro.R
import com.youseif.playerpro.utils.UrlAnalyzer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPlayScreen(
    onPlayUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as YouseifPlayerApp
    val scope = rememberCoroutineScope()
    val savedUrl by app.settingsRepository.lastQuickUrl.collectAsState(initial = "")
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedUrl) {
        if (url.isBlank() && savedUrl.isNotBlank()) {
            url = savedUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.player))
                        Text(
                            text = stringResource(R.string.slogan),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_url),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    error = null
                },
                label = { Text(stringResource(R.string.url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val normalized = UrlAnalyzer.normalizeUrl(url)
                    if (UrlAnalyzer.isValidUrl(normalized)) {
                        scope.launch {
                            app.settingsRepository.setLastQuickUrl(normalized)
                        }
                        onPlayUrl(normalized)
                    } else {
                        error = "Invalid URL"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.open_url))
            }
        }
    }
}
