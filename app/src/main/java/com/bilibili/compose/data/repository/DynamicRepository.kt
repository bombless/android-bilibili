package com.bilibili.compose.data.repository

import com.bilibili.compose.data.api.BiliApiService
import com.bilibili.compose.data.model.DynamicCreator
import com.bilibili.compose.data.model.DynamicFeedData
import com.bilibili.compose.data.model.DynamicPortalData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DynamicRepository(private val apiService: BiliApiService) {

    /**
     * Get dynamic portal info (includes user info and creator UP list)
     */
    suspend fun getPortal(): Result<DynamicPortalData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDynamicPortal(upListMore = 1)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取动态门户失败 (${response.code})" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all dynamics stream (全部动态)
     */
    suspend fun getAllDynamics(page: Int = 1, offset: String? = null): Result<DynamicFeedData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllDynamics(offset = offset, page = page)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取全部动态失败 (${response.code})" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get dynamics for a specific creator / UP (根据创作者账号过滤动态)
     */
    suspend fun getCreatorDynamics(
        hostMid: Long,
        page: Int = 1,
        offset: String? = null
    ): Result<DynamicFeedData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSpaceDynamics(hostMid = hostMid, offset = offset, page = page)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取创作者动态失败 (${response.code})" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
