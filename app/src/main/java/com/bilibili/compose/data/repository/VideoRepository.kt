package com.bilibili.compose.data.repository

import android.util.Log
import com.bilibili.compose.data.api.BiliApiService
import com.bilibili.compose.data.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class VideoRepository(
    private val apiService: BiliApiService,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(RcmdReason::class.java, RcmdReasonAdapter())
        .create()
) {

    suspend fun getHistory(
        max: Long = 0,
        viewAt: Long = 0,
        business: String = "",
        type: String = "all",
        pageSize: Int = 20
    ): Result<HistoryData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getHistoryCursor(max, viewAt, business, pageSize, type)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取历史记录失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    suspend fun getRecommendVideos(freshIdx: Int = 1, pageSize: Int = 10): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRecommendVideos(pageSize = pageSize, freshIdx = freshIdx)
            if (response.isSuccess && response.data != null) {
                val valid = response.data.item.filter { it.bvid.isNotBlank() }
                Result.success(valid)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取推荐视频失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRankingVideos(rid: Int = 0, day: Int = 3): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRankingVideos(rid = rid, day = day)
            if (response.isSuccess && response.data != null) {
                val items = response.data.list.mapIndexed { index, rankingItem ->
                    rankingItem.toVideoItem(rank = index + 1)
                }
                Result.success(items)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取排行榜失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPreciousVideos(page: Int = 1, pageSize: Int = 50): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPreciousVideos(page = page, pageSize = pageSize)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data.list)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取入站必刷失败" }))
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
                val errorMsg = response.message.ifBlank { "获取推荐视频失败" }
                Log.w("VideoRepository", "getRelatedVideos failed for $bvid: code=${response.code}, msg=$errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "getRelatedVideos exception for $bvid", e)
            Result.failure(e)
        }
    }

    suspend fun getVideoReplies(
        aid: Long,
        next: Long = 0,
        mode: Int = 3,
        pageSize: Int = 20
    ): Result<ReplyData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getVideoReplies(
                type = 1,
                aid = aid,
                mode = mode,
                next = next,
                pageSize = pageSize
            )
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取评论失败 (code: ${response.code})" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubReplies(
        aid: Long,
        rootRpid: Long,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<SubReplyData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSubReplies(
                type = 1,
                aid = aid,
                rootRpid = rootRpid,
                page = page,
                pageSize = pageSize
            )
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取回复详情失败 (code: ${response.code})" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAiRecommendVideos(
        serverUrl: String,
        pageSize: Int = 10,
        prompt: String = ""
    ): Result<List<VideoItem>> = withContext(Dispatchers.IO) {
        try {
            var base = serverUrl.trim()
            if (base.endsWith("/")) base = base.dropLast(1)
            val endpoint = if (base.endsWith("/api/recommend")) base else "$base/api/recommend"
            val urlBuilder = StringBuilder(endpoint)
                .append("?pageSize=").append(pageSize)
            if (prompt.isNotBlank()) {
                urlBuilder.append("&prompt=").append(URLEncoder.encode(prompt, "UTF-8"))
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("AI 服务连接失败: HTTP ${response.code}"))
            }

            val bodyString = response.body?.string() ?: ""
            val type = object : TypeToken<BiliResponse<RcmdData>>() {}.type
            val commonRes: BiliResponse<RcmdData> = gson.fromJson(bodyString, type)

            if (commonRes.isSuccess && commonRes.data != null) {
                val valid = commonRes.data.item.filter { it.bvid.isNotBlank() }
                Result.success(valid)
            } else {
                Result.failure(Exception(commonRes.message.ifBlank { "获取 AI 推荐数据失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
