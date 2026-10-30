package com.bilibili.compose.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilibili.compose.BiliApp
import com.bilibili.compose.data.local.UserSessionStore
import com.bilibili.compose.data.model.VideoItem
import com.bilibili.compose.data.repository.AuthRepository
import com.bilibili.compose.data.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class HomeUiState(
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isAiMode: Boolean = false,
    val aiServerUrl: String = "http://10.0.2.2:3000",
    val aiStatusMessage: String? = null
)

class HomeViewModel(
    private val videoRepository: VideoRepository,
    val authRepository: AuthRepository,
    val sessionStore: UserSessionStore = BiliApp.instance.sessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    init {
        viewModelScope.launch {
            sessionStore.aiServerUrlFlow.collect { url ->
                _uiState.value = _uiState.value.copy(aiServerUrl = url)
            }
        }
        loadVideos(refresh = true)
    }

    fun toggleAiMode(enabled: Boolean) {
        if (_uiState.value.isAiMode == enabled) return
        _uiState.value = _uiState.value.copy(
            isAiMode = enabled,
            videos = emptyList(),
            currentPage = 1
        )
        loadVideos(refresh = true)
    }

    fun updateAiServerUrl(url: String) {
        viewModelScope.launch {
            sessionStore.saveAiServerUrl(url)
            _uiState.value = _uiState.value.copy(aiServerUrl = url)
            if (_uiState.value.isAiMode) {
                refresh()
            }
        }
    }

    fun testAiConnection(url: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var cleanUrl = url.trim()
                if (cleanUrl.endsWith("/")) cleanUrl = cleanUrl.dropLast(1)
                val healthUrl = if (cleanUrl.endsWith("/api/health")) cleanUrl else "$cleanUrl/api/health"

                val req = Request.Builder().url(healthUrl).get().build()
                val resp = httpClient.newCall(req).execute()
                val body = resp.body?.string() ?: ""

                if (resp.isSuccessful && body.contains("healthy")) {
                    withContext(Dispatchers.Main) {
                        callback(true, "连接成功！AI 推荐服务正常运行中")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(false, "服务响应异常: HTTP ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, "连接失败: ${e.localizedMessage ?: e.message}")
                }
            }
        }
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

            if (_uiState.value.isAiMode) {
                // Load from custom AI recommendation server
                val result = videoRepository.getAiRecommendVideos(
                    serverUrl = _uiState.value.aiServerUrl,
                    pageSize = 10
                )
                result.onSuccess { aiVideos ->
                    val updatedList = if (refresh) aiVideos else _uiState.value.videos + aiVideos
                    _uiState.value = _uiState.value.copy(
                        videos = updatedList,
                        isLoading = false,
                        isRefreshing = false,
                        currentPage = nextPage,
                        hasMore = aiVideos.isNotEmpty(),
                        aiStatusMessage = "已加载 ${aiVideos.size} 条大模型深度优选推荐"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "无法连接 AI 推荐服务 (${_uiState.value.aiServerUrl})。\n请点击右上角设置，检查手机或模拟器网络连接。\n错误详情: ${error.localizedMessage}"
                    )
                }
            } else {
                // Regular Bilibili recommend feed
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
}
