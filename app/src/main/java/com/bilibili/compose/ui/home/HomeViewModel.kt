package com.bilibili.compose.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilibili.compose.data.model.VideoItem
import com.bilibili.compose.data.repository.AuthRepository
import com.bilibili.compose.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

class HomeViewModel(
    private val videoRepository: VideoRepository,
    val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadVideos(refresh = true)
    }

    fun refresh() {
        loadVideos(refresh = true)
    }

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.hasMore) return
        loadVideos(refresh = false)
    }

    private fun loadVideos(refresh: Boolean) {
        viewModelScope.launch {
            val nextPage = if (refresh) 1 else _uiState.value.currentPage + 1
            _uiState.value = _uiState.value.copy(
                isLoading = !refresh,
                isRefreshing = refresh,
                errorMessage = null
            )

            // Try recommend feed first, fall back to popular if needed
            val result = videoRepository.getRecommendVideos(freshIdx = nextPage, pageSize = 10)
            result.onSuccess { recVideos ->
                val list = if (recVideos.isNotEmpty()) {
                    recVideos
                } else {
                    videoRepository.getPopularVideos(page = nextPage, pageSize = 20).getOrDefault(emptyList())
                }
                val updatedList = if (refresh) list else _uiState.value.videos + list
                _uiState.value = _uiState.value.copy(
                    videos = updatedList,
                    isLoading = false,
                    isRefreshing = false,
                    currentPage = nextPage,
                    hasMore = list.isNotEmpty()
                )
            }.onFailure {
                val fallback = videoRepository.getPopularVideos(page = nextPage, pageSize = 20)
                fallback.onSuccess { popularList ->
                    val updatedList = if (refresh) popularList else _uiState.value.videos + popularList
                    _uiState.value = _uiState.value.copy(
                        videos = updatedList,
                        isLoading = false,
                        isRefreshing = false,
                        currentPage = nextPage,
                        hasMore = popularList.isNotEmpty()
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.localizedMessage ?: "加载失败，请检查网络"
                    )
                }
            }
        }
    }
}
