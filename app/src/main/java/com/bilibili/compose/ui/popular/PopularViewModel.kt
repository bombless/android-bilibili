package com.bilibili.compose.ui.popular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilibili.compose.data.model.PopularTab
import com.bilibili.compose.data.model.VideoItem
import com.bilibili.compose.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PopularUiState(
    val currentTab: PopularTab = PopularTab.OVERALL,
    // 综合热门
    val overallVideos: List<VideoItem> = emptyList(),
    val overallPage: Int = 1,
    val isOverallLoading: Boolean = false,
    val isOverallRefreshing: Boolean = false,
    val hasMoreOverall: Boolean = true,
    val overallError: String? = null,

    // 排行榜
    val rankingVideos: List<VideoItem> = emptyList(),
    val isRankingLoading: Boolean = false,
    val isRankingRefreshing: Boolean = false,
    val rankingError: String? = null,

    // 入站必刷
    val preciousVideos: List<VideoItem> = emptyList(),
    val isPreciousLoading: Boolean = false,
    val isPreciousRefreshing: Boolean = false,
    val preciousError: String? = null
) {
    val currentVideos: List<VideoItem>
        get() = when (currentTab) {
            PopularTab.OVERALL -> overallVideos
            PopularTab.RANKING -> rankingVideos
            PopularTab.PRECIOUS -> preciousVideos
        }

    val isCurrentLoading: Boolean
        get() = when (currentTab) {
            PopularTab.OVERALL -> isOverallLoading
            PopularTab.RANKING -> isRankingLoading
            PopularTab.PRECIOUS -> isPreciousLoading
        }

    val isCurrentRefreshing: Boolean
        get() = when (currentTab) {
            PopularTab.OVERALL -> isOverallRefreshing
            PopularTab.RANKING -> isRankingRefreshing
            PopularTab.PRECIOUS -> isPreciousRefreshing
        }

    val currentError: String?
        get() = when (currentTab) {
            PopularTab.OVERALL -> overallError
            PopularTab.RANKING -> rankingError
            PopularTab.PRECIOUS -> preciousError
        }
}

class PopularViewModel(private val videoRepository: VideoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PopularUiState())
    val uiState: StateFlow<PopularUiState> = _uiState.asStateFlow()

    init {
        loadOverall(refresh = true)
    }

    fun switchTab(tab: PopularTab) {
        if (_uiState.value.currentTab == tab) return
        _uiState.value = _uiState.value.copy(currentTab = tab)

        when (tab) {
            PopularTab.OVERALL -> {
                if (_uiState.value.overallVideos.isEmpty()) {
                    loadOverall(refresh = true)
                }
            }
            PopularTab.RANKING -> {
                if (_uiState.value.rankingVideos.isEmpty()) {
                    loadRanking(refresh = true)
                }
            }
            PopularTab.PRECIOUS -> {
                if (_uiState.value.preciousVideos.isEmpty()) {
                    loadPrecious(refresh = true)
                }
            }
        }
    }

    fun refresh() {
        when (_uiState.value.currentTab) {
            PopularTab.OVERALL -> loadOverall(refresh = true)
            PopularTab.RANKING -> loadRanking(refresh = true)
            PopularTab.PRECIOUS -> loadPrecious(refresh = true)
        }
    }

    fun loadMore() {
        if (_uiState.value.currentTab == PopularTab.OVERALL) {
            if (_uiState.value.isOverallLoading || !_uiState.value.hasMoreOverall) return
            loadOverall(refresh = false)
        }
    }

    private fun loadOverall(refresh: Boolean) {
        viewModelScope.launch {
            val nextPage = if (refresh) 1 else _uiState.value.overallPage + 1
            _uiState.value = _uiState.value.copy(
                isOverallLoading = !refresh,
                isOverallRefreshing = refresh,
                overallError = null
            )

            videoRepository.getPopularVideos(page = nextPage, pageSize = 20)
                .onSuccess { newVideos ->
                    val updated = if (refresh) newVideos else _uiState.value.overallVideos + newVideos
                    _uiState.value = _uiState.value.copy(
                        overallVideos = updated,
                        overallPage = nextPage,
                        isOverallLoading = false,
                        isOverallRefreshing = false,
                        hasMoreOverall = newVideos.isNotEmpty()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isOverallLoading = false,
                        isOverallRefreshing = false,
                        overallError = error.localizedMessage ?: "加载综合热门失败"
                    )
                }
        }
    }

    private fun loadRanking(refresh: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRankingLoading = !refresh,
                isRankingRefreshing = refresh,
                rankingError = null
            )

            videoRepository.getRankingVideos()
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        rankingVideos = videos,
                        isRankingLoading = false,
                        isRankingRefreshing = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isRankingLoading = false,
                        isRankingRefreshing = false,
                        rankingError = error.localizedMessage ?: "加载排行榜失败"
                    )
                }
        }
    }

    private fun loadPrecious(refresh: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPreciousLoading = !refresh,
                isPreciousRefreshing = refresh,
                preciousError = null
            )

            videoRepository.getPreciousVideos(page = 1, pageSize = 50)
                .onSuccess { videos ->
                    _uiState.value = _uiState.value.copy(
                        preciousVideos = videos,
                        isPreciousLoading = false,
                        isPreciousRefreshing = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPreciousLoading = false,
                        isPreciousRefreshing = false,
                        preciousError = error.localizedMessage ?: "加载入站必刷失败"
                    )
                }
        }
    }
}
