package com.bilibili.compose.ui.player

import android.util.Log
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
    val creatorFollowing: Boolean? = null,
    val isUpdatingFollow: Boolean = false,
    val followError: String? = null,
    val selectedCid: Long = 0,
    val videoStreamUrl: String? = null,
    val acceptDescriptions: List<String> = emptyList(),
    val acceptQualities: List<Int> = emptyList(),
    val selectedQuality: Int = 64,
    val relatedVideos: List<VideoItem> = emptyList(),
    val comments: List<ReplyItem> = emptyList(),
    val commentsTotalCount: Int = 0,
    val commentsNextCursor: Long = 0,
    val commentsHasMore: Boolean = true,
    val commentsMode: Int = 3, // 3: 最热, 2: 最新
    val isLoadingDetail: Boolean = false,
    val isLoadingStream: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val commentsError: String? = null,
    val isLoadingRelated: Boolean = false,
    val relatedError: String? = null,
    val errorMessage: String? = null,
    val selectedTab: Int = 0, // 0: Related, 1: Comments
    val activeRootComment: ReplyItem? = null,
    val subReplies: List<ReplyItem> = emptyList(),
    val subRepliesPage: Int = 1,
    val subRepliesTotalCount: Int = 0,
    val subRepliesHasMore: Boolean = true,
    val isLoadingSubReplies: Boolean = false,
    val isLoadingMoreSubReplies: Boolean = false,
    val subRepliesError: String? = null,
    val showSubRepliesSheet: Boolean = false
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
                val totalComments = detail.stat?.reply?.toInt() ?: 0
                _uiState.value = _uiState.value.copy(
                    videoDetail = detail,
                    selectedCid = cid,
                    commentsTotalCount = totalComments,
                    isLoadingDetail = false
                )

                detail.owner?.mid?.takeIf { it > 0 }?.let { mid ->
                    viewModelScope.launch {
                        videoRepository.getCreatorFollowing(mid)
                            .onSuccess { following ->
                                if (_uiState.value.videoDetail?.owner?.mid == mid) {
                                    _uiState.value = _uiState.value.copy(creatorFollowing = following)
                                }
                            }
                            .onFailure { error ->
                                if (_uiState.value.videoDetail?.owner?.mid == mid) {
                                    _uiState.value = _uiState.value.copy(
                                        creatorFollowing = false,
                                        followError = error.localizedMessage ?: "关注状态读取失败"
                                    )
                                }
                            }
                    }
                }

                // 2. Fetch Play Stream URL
                if (cid > 0) {
                    loadPlayUrl(bvid, cid, _uiState.value.selectedQuality)
                }

                // 3. Fetch Related Videos
                loadRelatedVideos(bvid)

                // 4. Fetch Comments
                loadComments(detail.aid, isRefresh = true)
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

    fun toggleCreatorFollow() {
        val mid = _uiState.value.videoDetail?.owner?.mid?.takeIf { it > 0 } ?: return
        val current = _uiState.value.creatorFollowing ?: return
        if (_uiState.value.isUpdatingFollow) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingFollow = true, followError = null)
            videoRepository.setCreatorFollowing(mid, !current)
                .onSuccess { following -> _uiState.value = _uiState.value.copy(creatorFollowing = following, isUpdatingFollow = false) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(isUpdatingFollow = false, followError = error.localizedMessage ?: "关注操作失败") }
        }
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
            _uiState.value = _uiState.value.copy(isLoadingRelated = true, relatedError = null)
            val result = videoRepository.getRelatedVideos(bvid)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    relatedVideos = list.distinctBy { it.bvid.ifBlank { it.aid.toString() } },
                    isLoadingRelated = false
                )
            }.onFailure { error ->
                Log.e("PlayerViewModel", "Failed to load related videos for $bvid: ${error.message}", error)
                _uiState.value = _uiState.value.copy(
                    isLoadingRelated = false,
                    relatedError = error.localizedMessage ?: "加载相关推荐失败"
                )
            }
        }
    }

    fun reloadRelatedVideos() {
        val bvid = _uiState.value.bvid
        if (bvid.isNotBlank()) {
            loadRelatedVideos(bvid)
        }
    }

    fun loadComments(aid: Long = 0, isRefresh: Boolean = true) {
        val targetAid = if (aid > 0) aid else (_uiState.value.videoDetail?.aid ?: return)

        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(
                    isLoadingComments = true,
                    commentsError = null,
                    commentsNextCursor = 0,
                    commentsHasMore = true
                )
            } else {
                if (_uiState.value.isLoadingComments || _uiState.value.isLoadingMoreComments || !_uiState.value.commentsHasMore) {
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    isLoadingMoreComments = true,
                    commentsError = null
                )
            }

            val nextCursor = if (isRefresh) 0L else _uiState.value.commentsNextCursor
            val mode = _uiState.value.commentsMode

            val result = videoRepository.getVideoReplies(
                aid = targetAid,
                next = nextCursor,
                mode = mode
            )

            result.onSuccess { replyData ->
                val topList = if (isRefresh) {
                    (replyData.topReplies ?: emptyList()).map { it.copy(isTop = true) }
                } else {
                    emptyList()
                }
                val newReplies = replyData.replies ?: emptyList()
                val cursor = replyData.cursor

                val updatedComments = if (isRefresh) {
                    (topList + newReplies).distinctBy { it.rpid }
                } else {
                    (_uiState.value.comments + newReplies).distinctBy { it.rpid }
                }

                val hasMore = if (cursor != null) {
                    !cursor.isEnd && newReplies.isNotEmpty()
                } else {
                    newReplies.isNotEmpty()
                }

                val next = cursor?.next ?: (nextCursor + 1)
                val totalCount = cursor?.allCount?.takeIf { it > 0 }
                    ?: (_uiState.value.videoDetail?.stat?.reply?.toInt() ?: updatedComments.size)

                _uiState.value = _uiState.value.copy(
                    comments = updatedComments,
                    commentsTotalCount = totalCount,
                    commentsNextCursor = next,
                    commentsHasMore = hasMore,
                    isLoadingComments = false,
                    isLoadingMoreComments = false
                )
            }.onFailure { error ->
                Log.e("PlayerViewModel", "Failed to load replies for aid=$targetAid", error)
                _uiState.value = _uiState.value.copy(
                    isLoadingComments = false,
                    isLoadingMoreComments = false,
                    commentsError = error.localizedMessage ?: "加载评论失败"
                )
            }
        }
    }

    fun loadMoreComments() {
        val aid = _uiState.value.videoDetail?.aid ?: return
        loadComments(aid = aid, isRefresh = false)
    }

    fun setCommentsMode(mode: Int) {
        if (_uiState.value.commentsMode == mode) return
        _uiState.value = _uiState.value.copy(commentsMode = mode)
        val aid = _uiState.value.videoDetail?.aid ?: return
        loadComments(aid = aid, isRefresh = true)
    }

    fun openSubReplies(comment: ReplyItem) {
        _uiState.value = _uiState.value.copy(
            activeRootComment = comment,
            subReplies = comment.replies ?: emptyList(),
            subRepliesPage = 1,
            subRepliesTotalCount = comment.rcount,
            subRepliesHasMore = true,
            isLoadingSubReplies = true,
            subRepliesError = null,
            showSubRepliesSheet = true
        )
        loadSubReplies(comment.rpid, page = 1, isRefresh = true)
    }

    fun closeSubReplies() {
        _uiState.value = _uiState.value.copy(
            showSubRepliesSheet = false,
            activeRootComment = null,
            subReplies = emptyList(),
            isLoadingSubReplies = false,
            isLoadingMoreSubReplies = false,
            subRepliesError = null
        )
    }

    fun loadSubReplies(rootRpid: Long, page: Int = 1, isRefresh: Boolean = false) {
        val aid = _uiState.value.videoDetail?.aid ?: return
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSubReplies = true,
                    subRepliesError = null
                )
            } else {
                if (_uiState.value.isLoadingSubReplies || _uiState.value.isLoadingMoreSubReplies || !_uiState.value.subRepliesHasMore) {
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    isLoadingMoreSubReplies = true,
                    subRepliesError = null
                )
            }

            val result = videoRepository.getSubReplies(aid, rootRpid, page = page)
            result.onSuccess { data ->
                val newReplies = data.replies ?: emptyList()
                val merged = if (isRefresh) {
                    newReplies
                } else {
                    (_uiState.value.subReplies + newReplies).distinctBy { it.rpid }
                }
                val total = data.page?.count ?: _uiState.value.subRepliesTotalCount
                val hasMore = newReplies.isNotEmpty() && (merged.size < total)

                _uiState.value = _uiState.value.copy(
                    activeRootComment = data.root ?: _uiState.value.activeRootComment,
                    subReplies = merged,
                    subRepliesPage = page,
                    subRepliesTotalCount = total,
                    subRepliesHasMore = hasMore,
                    isLoadingSubReplies = false,
                    isLoadingMoreSubReplies = false
                )
            }.onFailure { e ->
                Log.e("PlayerViewModel", "Failed to load sub-replies for rootRpid=$rootRpid", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingSubReplies = false,
                    isLoadingMoreSubReplies = false,
                    subRepliesError = e.localizedMessage ?: "加载回复失败"
                )
            }
        }
    }

    fun loadMoreSubReplies() {
        val rootRpid = _uiState.value.activeRootComment?.rpid ?: return
        val nextPage = _uiState.value.subRepliesPage + 1
        loadSubReplies(rootRpid, page = nextPage, isRefresh = false)
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
