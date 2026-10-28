package com.bilibili.compose.data.repository

import com.bilibili.compose.data.api.BiliApiService
import com.bilibili.compose.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val apiService: BiliApiService) {

    suspend fun getPopularVideos(page: Int, pageSize: Int = 20): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPopularVideos(page, pageSize)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data.list)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取热门视频失败 (code: ${response.code})" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVideoDetail(bvid: String): Result<VideoViewData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getVideoView(bvid)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取视频信息失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayUrl(bvid: String, cid: Long, qn: Int = 64): Result<PlayUrlData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPlayUrl(bvid, cid, qn, fnval = 1)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取播放链接失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRelatedVideos(bvid: String): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRelatedVideos(bvid)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取推荐视频失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVideoReplies(aid: Long, page: Int, pageSize: Int = 20): Result<ReplyData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getVideoReplies(type = 1, aid = aid, page = page, pageSize = pageSize)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取评论失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
