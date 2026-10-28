package com.bilibili.compose.ui.dynamic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilibili.compose.data.cdp.CdpSyncResult
import com.bilibili.compose.data.model.DynamicCreator
import com.bilibili.compose.data.model.DynamicItem
import com.bilibili.compose.data.repository.AuthRepository
import com.bilibili.compose.data.repository.DynamicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DynamicUiState(
    val creators: List<DynamicCreator> = emptyList(),
    val filteredCreators: List<DynamicCreator> = emptyList(),
    val selectedCreator: DynamicCreator? = null,
    val creatorSearchQuery: String = "",
    val isSearchActive: Boolean = false,
    val dynamics: List<DynamicItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val page: Int = 1,
    val offset: String? = null,
    val errorMessage: String? = null,
    val cdpSyncLoading: Boolean = false,
    val cdpMessage: String? = null
)

class DynamicViewModel(
    private val dynamicRepository: DynamicRepository,
    val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DynamicUiState())
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            // 1. Load portal (up_list)
            loadPortal()
            // 2. Load feed (all or creator)
            loadDynamics(page = 1, offset = null, isRefresh = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            loadPortal()
            loadDynamics(page = 1, offset = null, isRefresh = true)
        }
    }

    private suspend fun loadPortal() {
        val result = dynamicRepository.getPortal()
        result.onSuccess { portalData ->
            val items = portalData.upList?.items ?: emptyList()
            _uiState.value = _uiState.value.copy(
                creators = items,
                filteredCreators = filterCreators(items, _uiState.value.creatorSearchQuery)
            )
        }.onFailure {
            // Portal error is not fatal (might be guest mode)
        }
    }

    private suspend fun loadDynamics(page: Int, offset: String?, isRefresh: Boolean) {
        val selected = _uiState.value.selectedCreator
        val result = if (selected == null) {
            dynamicRepository.getAllDynamics(page = page, offset = offset)
        } else {
            dynamicRepository.getCreatorDynamics(hostMid = selected.mid, page = page, offset = offset)
        }

        result.onSuccess { feedData ->
            val newItems = feedData.items
            val currentList = if (isRefresh || page == 1) emptyList() else _uiState.value.dynamics
            _uiState.value = _uiState.value.copy(
                dynamics = currentList + newItems,
                hasMore = feedData.hasMore && newItems.isNotEmpty(),
                page = page,
                offset = feedData.offset,
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                errorMessage = null
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                errorMessage = error.localizedMessage ?: "加载动态失败"
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || state.isRefreshing || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            loadDynamics(page = state.page + 1, offset = state.offset, isRefresh = false)
        }
    }

    /**
     * Filter dynamics by creator account
     */
    fun selectCreator(creator: DynamicCreator?) {
        if (_uiState.value.selectedCreator == creator) return
        _uiState.value = _uiState.value.copy(
            selectedCreator = creator,
            dynamics = emptyList(),
            isLoading = true,
            hasMore = true,
            page = 1,
            offset = null,
            errorMessage = null
        )
        viewModelScope.launch {
            loadDynamics(page = 1, offset = null, isRefresh = false)
        }
    }

    /**
     * Filter creator list by keyword (e.g. search UP by name)
     */
    fun onCreatorSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            creatorSearchQuery = query,
            filteredCreators = filterCreators(_uiState.value.creators, query)
        )
    }

    fun toggleSearchActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSearchActive = active,
            creatorSearchQuery = if (!active) "" else _uiState.value.creatorSearchQuery,
            filteredCreators = if (!active) _uiState.value.creators else _uiState.value.filteredCreators
        )
    }

    private fun filterCreators(all: List<DynamicCreator>, query: String): List<DynamicCreator> {
        if (query.isBlank()) return all
        return all.filter { it.uname.contains(query, ignoreCase = true) }
    }

    /**
     * One-click session sync from Edge browser running CDP (port 9222)
     */
    fun syncWithEdgeCdp(port: Int = 9222) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cdpSyncLoading = true, cdpMessage = "正在连接 Edge (端口 $port)...")
            val result = authRepository.syncWithEdgeCdp(port = port)
            _uiState.value = _uiState.value.copy(
                cdpSyncLoading = false,
                cdpMessage = result.message
            )
            if (result.success) {
                // Refresh dynamic data with newly imported session
                refresh()
            }
        }
    }

    fun clearCdpMessage() {
        _uiState.value = _uiState.value.copy(cdpMessage = null)
    }
}
