package com.bilibili.compose.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilibili.compose.data.model.HistoryCursor
import com.bilibili.compose.data.model.HistoryEntry
import com.bilibili.compose.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val cursor: HistoryCursor = HistoryCursor(),
    val selectedType: String = "all",
    val searchQuery: String = "",
    val timeFilter: String = "全部时间",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null
)

class HistoryViewModel(private val repository: VideoRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init { loadHistory(refresh = true) }

    fun setType(type: String) {
        if (_uiState.value.selectedType == type) return
        _uiState.value = _uiState.value.copy(selectedType = type)
        loadHistory(refresh = true)
    }

    fun setSearchQuery(query: String) { _uiState.value = _uiState.value.copy(searchQuery = query) }
    fun setTimeFilter(filter: String) { _uiState.value = _uiState.value.copy(timeFilter = filter) }
    fun refresh() = loadHistory(refresh = true)

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadHistory(refresh = false)
    }

    private fun loadHistory(refresh: Boolean) {
        viewModelScope.launch {
            val before = _uiState.value
            _uiState.value = before.copy(isLoading = refresh, isLoadingMore = !refresh, errorMessage = null)
            val cursor = if (refresh) HistoryCursor() else before.cursor
            repository.getHistory(cursor.max, cursor.viewAt, cursor.business, before.selectedType)
                .onSuccess { page ->
                    val merged = if (refresh) page.list else (before.entries + page.list).distinctBy { it.kid }
                    _uiState.value = _uiState.value.copy(
                        entries = merged, cursor = page.cursor,
                        isLoading = false, isLoadingMore = false,
                        hasMore = page.list.size >= 20
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, isLoadingMore = false,
                        errorMessage = error.localizedMessage ?: "加载历史记录失败"
                    )
                }
        }
    }
}