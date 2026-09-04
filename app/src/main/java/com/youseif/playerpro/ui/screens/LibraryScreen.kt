package com.youseif.playerpro.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youseif.playerpro.R
import com.youseif.playerpro.YouseifPlayerApp
import com.youseif.playerpro.data.model.Source
import com.youseif.playerpro.viewmodel.LibraryTab
import com.youseif.playerpro.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPlay: (Source) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as YouseifPlayerApp
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(app.sourceRepository)
    )
    val state by viewModel.uiState.collectAsState()

    // Import result feedback
    LaunchedEffect(state.message, state.importResult) {
        val msg = state.message
        val result = state.importResult
        when {
            msg == "import_ok" && result != null -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.import_success) +
                        " (${result.validEntries}/${result.totalEntries})",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.clearMessage()
            }
            msg == "import_partial" && result != null -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.import_partial) +
                        " OK=${result.validEntries} err=${result.errors.size}",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.clearMessage()
            }
            msg == "saved" -> {
                Toast.makeText(context, context.getString(R.string.source_saved), Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
            msg == "deleted" -> {
                Toast.makeText(context, context.getString(R.string.source_deleted), Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
            msg == "network_error" || (msg != null && msg !in listOf("import_ok", "import_partial", "saved", "deleted")) -> {
                if (msg != null) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<Source?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Source?>(null) }
    var searchActive by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val content = input.bufferedReader().readText()
                viewModel.importM3uContent(content)
            }
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_SHORT).show()
        }
    }

    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri: Uri? ->
        val content = pendingExportContent
        pendingExportContent = null
        if (uri == null || content == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = stringResource(R.string.slogan),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.import_m3u))
                    }
                    IconButton(onClick = {
                        viewModel.exportM3u { content ->
                            pendingExportContent = content
                            exportLauncher.launch("youseif_playlist.m3u")
                        }
                    }) {
                        Icon(Icons.Default.UploadFile, contentDescription = stringResource(R.string.export_m3u))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSource = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_source))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (searchActive) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search)) },
                    singleLine = true
                )
            }

            ScrollableTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                edgePadding = 8.dp
            ) {
                Tab(
                    selected = state.selectedTab == LibraryTab.ALL,
                    onClick = { viewModel.setTab(LibraryTab.ALL) },
                    text = { Text(stringResource(R.string.all_sources)) }
                )
                Tab(
                    selected = state.selectedTab == LibraryTab.FAVORITES,
                    onClick = { viewModel.setTab(LibraryTab.FAVORITES) },
                    text = { Text(stringResource(R.string.favorites)) }
                )
                Tab(
                    selected = state.selectedTab == LibraryTab.RECENT,
                    onClick = { viewModel.setTab(LibraryTab.RECENT) },
                    text = { Text(stringResource(R.string.recent)) }
                )
                Tab(
                    selected = state.selectedTab == LibraryTab.CATEGORIES,
                    onClick = { viewModel.setTab(LibraryTab.CATEGORIES) },
                    text = { Text(stringResource(R.string.categories)) }
                )
            }

            if (state.selectedTab == LibraryTab.CATEGORIES && state.selectedCategory == null) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories) { cat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCategory(cat) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    if (state.categories.isEmpty()) {
                        item {
                            EmptyHint(stringResource(R.string.no_results))
                        }
                    }
                }
            } else if (state.sources.isEmpty() && !state.isLoading) {
                EmptyLibrary(
                    isFavorites = state.selectedTab == LibraryTab.FAVORITES,
                    isRecent = state.selectedTab == LibraryTab.RECENT
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.sources, key = { it.id }) { source ->
                        SourceItem(
                            source = source,
                            onPlay = {
                                viewModel.markPlayed(source)
                                onPlay(source)
                            },
                            onEdit = {
                                editingSource = source
                                showAddDialog = true
                            },
                            onDelete = { deleteTarget = source },
                            onToggleFavorite = { viewModel.toggleFavorite(source) },
                            onCopy = {
                                copyText(context, source.url)
                                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                            },
                            onShare = { shareText(context, source.url) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showAddDialog) {
        SourceEditorDialog(
            initial = editingSource,
            onDismiss = { showAddDialog = false },
            onSave = { source ->
                if (source.id == 0L) viewModel.addSource(source)
                else viewModel.updateSource(source)
                showAddDialog = false
            }
        )
    }

    if (showImportDialog) {
        ImportM3uDialog(
            onDismiss = { showImportDialog = false },
            onImportText = { content ->
                viewModel.importM3uContent(content)
                showImportDialog = false
            },
            onImportUrl = { m3uUrl ->
                viewModel.importM3uFromUrl(m3uUrl)
                showImportDialog = false
            },
            onPickFile = {
                showImportDialog = false
                filePicker.launch("*/*")
            }
        )
    }

    deleteTarget?.let { src ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSource(src)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SourceItem(
    source: Source,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlay)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (source.category.isNotBlank()) {
                    Text(
                        text = source.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (source.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (source.isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_source)) },
                        onClick = { menuOpen = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy_url)) },
                        onClick = { menuOpen = false; onCopy() },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_url)) },
                        onClick = { menuOpen = false; onShare() },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = { menuOpen = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(isFavorites: Boolean, isRecent: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when {
                    isFavorites -> stringResource(R.string.no_favorites)
                    isRecent -> stringResource(R.string.no_recent)
                    else -> stringResource(R.string.empty_library)
                },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_library_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SourceEditorDialog(
    initial: Source?,
    onDismiss: () -> Unit,
    onSave: (Source) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var userAgent by remember { mutableStateOf(initial?.userAgent ?: "") }
    var referer by remember { mutableStateOf(initial?.referer ?: "") }
    var headers by remember { mutableStateOf(initial?.headersJson ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) stringResource(R.string.add_source)
                else stringResource(R.string.edit_source)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.url) + " *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.category)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text(stringResource(R.string.user_agent)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = referer,
                    onValueChange = { referer = it },
                    label = { Text(stringResource(R.string.referer)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = headers,
                    onValueChange = { headers = it },
                    label = { Text(stringResource(R.string.headers)) },
                    placeholder = { Text(stringResource(R.string.headers_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onSave(
                            Source(
                                id = initial?.id ?: 0,
                                name = name.ifBlank { url },
                                url = url.trim(),
                                category = category.trim(),
                                description = description.trim(),
                                userAgent = userAgent.trim(),
                                referer = referer.trim(),
                                headersJson = headers.trim(),
                                isFavorite = initial?.isFavorite ?: false,
                                lastPlayedAt = initial?.lastPlayedAt ?: 0L
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ImportM3uDialog(
    onDismiss: () -> Unit,
    onImportText: (String) -> Unit,
    onImportUrl: (String) -> Unit,
    onPickFile: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_m3u)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.m3u_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { if (url.isNotBlank()) onImportUrl(url.trim()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.from_url))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.paste_m3u)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
                TextButton(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.from_file))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        text.isNotBlank() -> onImportText(text)
                        url.isNotBlank() -> onImportUrl(url.trim())
                    }
                }
            ) {
                Text(stringResource(R.string.import_m3u))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("url", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
