package com.bilibili.compose.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilibili.compose.data.model.ReplyItem
import com.bilibili.compose.data.model.VideoItem
import com.bilibili.compose.data.model.VideoViewData
import com.bilibili.compose.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val bvid: String = "",
    val videoDetail: VideoViewData? = null,
    val selectedCid: Long = 0,
    val videoStreamUrl: String? = null,
    val acceptDescriptions: List<String> = emptyList(),
    val acceptQualities: List<Int> = emptyList(),
    val selectedQuality: Int = 64,
    val relatedVideos: List<VideoItem> = emptyList(),
    val comments: List<ReplyItem> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val isLoadingStream: Boolean = false,
    val isLoadingComments: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: Int = 0 // 0: Related, 1: Comments
)

class PlayerViewModel(private val videoRepository: VideoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadVideo(bvid: String) {
        if (_uiState.value.bvid == bvid && _uiState.value.videoDetail != null) return

        _uiState.value = PlayerUiState(bvid = bvid, isLoadingDetail = true)

        viewModelScope.launch {
            // 1. Fetch Video Detail
            val detailResult = videoRepository.getVideoDetail(bvid)
            detailResult.onSuccess { detail ->
                val cid = detail.cid.takeIf { it > 0 } ?: (detail.pages.firstOrNull()?.cid ?: 0)
                _uiState.value = _uiState.value.copy(
                    videoDetail = detail,
                    selectedCid = cid,
                    isLoadingDetail = false
                )

                // 2. Fetch Play Stream URL
                if (cid > 0) {
                    loadPlayUrl(bvid, cid, _uiState.value.selectedQuality)
                }

                // 3. Fetch Related Videos
                loadRelatedVideos(bvid)

                // 4. Fetch Comments
                loadComments(detail.aid)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingDetail = false,
                    errorMessage = error.localizedMessage ?: "加载视频详情失败"
                )
            }
        }
    }

    fun selectPart(cid: Long) {
        if (_uiState.value.selectedCid == cid) return
        _uiState.value = _uiState.value.copy(selectedCid = cid)
        loadPlayUrl(_uiState.value.bvid, cid, _uiState.value.selectedQuality)
    }

    fun selectQuality(quality: Int) {
        if (_uiState.value.selectedQuality == quality) return
        _uiState.value = _uiState.value.copy(selectedQuality = quality)
        loadPlayUrl(_uiState.value.bvid, _uiState.value.selectedCid, quality)
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    private fun loadPlayUrl(bvid: String, cid: Long, quality: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStream = true, errorMessage = null)
            val result = videoRepository.getPlayUrl(bvid, cid, quality)
            result.onSuccess { playData ->
                val streamUrl = playData.durl.firstOrNull()?.url
                _uiState.value = _uiState.value.copy(
                    videoStreamUrl = streamUrl,
                    acceptDescriptions = playData.acceptDescription,
                    acceptQualities = playData.acceptQuality,
                    isLoadingStream = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingStream = false,
                    errorMessage = "解析视频流失败: ${error.localizedMessage}"
                )
            }
        }
    }

    private fun loadRelatedVideos(bvid: String) {
        viewModelScope.launch {
            val result = videoRepository.getRelatedVideos(bvid)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(relatedVideos = list)
            }
        }
    }

    private fun loadComments(aid: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingComments = true)
            val result = videoRepository.getVideoReplies(aid, page = 1)
            result.onSuccess { replyData ->
                _uiState.value = _uiState.value.copy(
                    comments = replyData.replies ?: emptyList(),
                    isLoadingComments = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingComments = false)
            }
        }
    }

    fun retry() {
        val bvid = _uiState.value.bvid
        val cid = _uiState.value.selectedCid
        if (cid > 0) {
            loadPlayUrl(bvid, cid, _uiState.value.selectedQuality)
        } else {
            loadVideo(bvid)
        }
    }
}
