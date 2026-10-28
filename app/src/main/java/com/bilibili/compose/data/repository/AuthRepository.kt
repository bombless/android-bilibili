package com.bilibili.compose.data.repository

import com.bilibili.compose.data.api.BiliApiService
import com.bilibili.compose.data.api.BiliPassportService
import com.bilibili.compose.data.local.UserSessionStore
import com.bilibili.compose.data.model.NavData
import com.bilibili.compose.data.model.QrGenerateData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed class QrPollResult {
    data object WaitingScan : QrPollResult()
    data object ScannedWaitingConfirm : QrPollResult()
    data object Expired : QrPollResult()
    data class Success(val message: String) : QrPollResult()
    data class Error(val message: String) : QrPollResult()
}

class AuthRepository(
    private val passportService: BiliPassportService,
    private val apiService: BiliApiService,
    private val sessionStore: UserSessionStore
) {

    val isLoggedInFlow: Flow<Boolean> = sessionStore.isLoggedInFlow
    val userNavFlow: Flow<NavData?> = sessionStore.userNavFlow

    suspend fun generateQrCode(): Result<QrGenerateData> = withContext(Dispatchers.IO) {
        try {
            val response = passportService.generateQrCode()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "生成二维码失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pollQrCode(key: String): QrPollResult = withContext(Dispatchers.IO) {
        try {
            val response = passportService.pollQrCode(key)
            if (!response.isSuccessful) {
                return@withContext QrPollResult.Error("网络请求失败: ${response.code()}")
            }

            val body = response.body() ?: return@withContext QrPollResult.Error("响应数据为空")
            val data = body.data ?: return@withContext QrPollResult.Error(body.message.ifBlank { "状态未知" })

            when (data.code) {
                0 -> {
                    // Login success!
                    val setCookieHeaders = response.headers().values("Set-Cookie")
                    sessionStore.saveFromSetCookieHeaders(setCookieHeaders)
                    if (data.url.isNotBlank()) {
                        sessionStore.saveFromUrl(data.url)
                    }
                    // Fetch profile to verify
                    fetchUserProfile()
                    QrPollResult.Success(data.message.ifBlank { "登录成功" })
                }
                86101 -> QrPollResult.WaitingScan
                86090 -> QrPollResult.ScannedWaitingConfirm
                86038 -> QrPollResult.Expired
                else -> QrPollResult.Error(data.message.ifBlank { "登录状态: ${data.code}" })
            }
        } catch (e: Exception) {
            QrPollResult.Error("轮询出错: ${e.message}")
        }
    }

    suspend fun fetchUserProfile(): Result<NavData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getNav()
            if (response.isSuccess && response.data != null) {
                sessionStore.saveUserNav(response.data)
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message.ifBlank { "获取个人信息失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCookies(cookies: Map<String, String>) = withContext(Dispatchers.IO) {
        sessionStore.saveCookies(cookies)
        fetchUserProfile()
    }

    suspend fun saveFromUrl(url: String) = withContext(Dispatchers.IO) {
        sessionStore.saveFromUrl(url)
        fetchUserProfile()
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionStore.clearSession()
    }
}
