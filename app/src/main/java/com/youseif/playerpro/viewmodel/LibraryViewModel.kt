package com.youseif.playerpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.youseif.playerpro.data.m3u.M3uParser
import com.youseif.playerpro.data.model.M3uParseResult
import com.youseif.playerpro.data.model.Source
import com.youseif.playerpro.data.repository.SourceRepository
import com.youseif.playerpro.utils.NetworkFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab { ALL, FAVORITES, RECENT, CATEGORIES }

data class LibraryUiState(
    val sources: List<Source> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val importResult: M3uParseResult? = null
)

class LibraryViewModel(
    private val repository: SourceRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.ALL)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _importResult = MutableStateFlow<M3uParseResult?>(null)

    private val allSources = repository.getAll()
    private val favorites = repository.getFavorites()
    private val recent = repository.getRecent()
    private val categories = repository.getCategories()

    val uiState: StateFlow<LibraryUiState> = combine(
        allSources, favorites, recent, categories,
        _selectedTab, _selectedCategory, _searchQuery, _isLoading, _message, _importResult
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val all = values[0] as List<Source>
        val favs = values[1] as List<Source>
        val rec = values[2] as List<Source>
        val cats = values[3] as List<String>
        val tab = values[4] as LibraryTab
        val cat = values[5] as String?
        val query = values[6] as String
        val loading = values[7] as Boolean
        val msg = values[8] as String?
        val importRes = values[9] as M3uParseResult?

        val filtered = when {
            query.isNotBlank() -> all.filter {
                it.name.contains(query, true) ||
                    it.url.contains(query, true) ||
                    it.category.contains(query, true) ||
                    it.description.contains(query, true)
            }
            tab == LibraryTab.FAVORITES -> favs
            tab == LibraryTab.RECENT -> rec
            tab == LibraryTab.CATEGORIES && cat != null -> all.filter { it.category == cat }
            else -> all
        }

        LibraryUiState(
            sources = filtered,
            categories = cats,
            selectedTab = tab,
            selectedCategory = cat,
            searchQuery = query,
            isLoading = loading,
            message = msg,
            importResult = importRes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun setTab(tab: LibraryTab) {
        _selectedTab.value = tab
        if (tab != LibraryTab.CATEGORIES) _selectedCategory.value = null
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
        _selectedTab.value = LibraryTab.CATEGORIES
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun clearMessage() {
        _message.value = null
        _importResult.value = null
    }

    fun addSource(source: Source) {
        viewModelScope.launch {
            repository.add(source)
            _message.value = "saved"
        }
    }

    fun updateSource(source: Source) {
        viewModelScope.launch {
            repository.update(source)
            _message.value = "saved"
        }
    }

    fun deleteSource(source: Source) {
        viewModelScope.launch {
            repository.delete(source)
            _message.value = "deleted"
        }
    }

    fun toggleFavorite(source: Source) {
        viewModelScope.launch {
            repository.setFavorite(source.id, !source.isFavorite)
        }
    }

    fun markPlayed(source: Source) {
        viewModelScope.launch {
            repository.markPlayed(source.id)
        }
    }

    fun importM3uContent(content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = M3uParser.parse(content)
                if (result.sources.isNotEmpty()) {
                    repository.insertAll(result.sources)
                }
                _importResult.value = result
                _message.value = if (result.errors.isEmpty()) "import_ok" else "import_partial"
            } catch (e: Exception) {
                _message.value = e.message ?: "error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importM3uFromUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetch = NetworkFetcher.fetchText(url.trim())
                if (!fetch.success) {
                    _message.value = fetch.error ?: "network_error"
                    return@launch
                }
                val result = M3uParser.parse(fetch.body)
                if (result.sources.isNotEmpty()) {
                    repository.insertAll(result.sources)
                }
                _importResult.value = result
                _message.value = if (result.errors.isEmpty()) "import_ok" else "import_partial"
            } catch (e: Exception) {
                _message.value = e.message ?: "error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportM3u(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val list = repository.getAll().first()
            onResult(M3uParser.export(list))
        }
    }

    class Factory(private val repository: SourceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository) as T
        }
    }
}
