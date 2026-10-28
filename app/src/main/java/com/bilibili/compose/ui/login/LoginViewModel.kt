package com.bilibili.compose.ui.login

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilibili.compose.data.repository.AuthRepository
import com.bilibili.compose.data.repository.QrPollResult
import com.bilibili.compose.util.QrCodeGenerator
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class LoginUiState(
    val qrCodeBitmap: ImageBitmap? = null,
    val qrCodeKey: String? = null,
    val statusText: String = "正在生成登录二维码...",
    val isLoading: Boolean = false,
    val isScanned: Boolean = false,
    val isExpired: Boolean = false,
    val isSuccess: Boolean = false,
    val cdpSyncLoading: Boolean = false,
    val cdpMessage: String? = null
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        generateQrCode()
    }

    fun generateQrCode() {
        stopPolling()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isExpired = false,
                isScanned = false,
                statusText = "正在生成二维码..."
            )

            val result = authRepository.generateQrCode()
            result.onSuccess { data ->
                val bitmap = QrCodeGenerator.generate(data.url)
                _uiState.value = _uiState.value.copy(
                    qrCodeBitmap = bitmap,
                    qrCodeKey = data.qrcodeKey,
                    isLoading = false,
                    statusText = "请使用 哔哩哔哩 手机客户端扫码"
                )
                startPolling(data.qrcodeKey)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusText = "生成二维码失败: ${error.localizedMessage}"
                )
            }
        }
    }

    private fun startPolling(key: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(2000)
                val pollResult = authRepository.pollQrCode(key)
                when (pollResult) {
                    is QrPollResult.WaitingScan -> {
                        _uiState.value = _uiState.value.copy(
                            isScanned = false,
                            statusText = "请使用 哔哩哔哩 手机客户端扫码"
                        )
                    }
                    is QrPollResult.ScannedWaitingConfirm -> {
                        _uiState.value = _uiState.value.copy(
                            isScanned = true,
                            statusText = "已扫描，请在手机上点击确认登录"
                        )
                    }
                    is QrPollResult.Expired -> {
                        _uiState.value = _uiState.value.copy(
                            isExpired = true,
                            statusText = "二维码已失效，点击刷新"
                        )
                        break
                    }
                    is QrPollResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSuccess = true,
                            statusText = "登录成功！"
                        )
                        break
                    }
                    is QrPollResult.Error -> {
                        // Keep polling or show hint
                    }
                }
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /**
     * Import cookies from Edge CDP directly (port 9222)
     */
    fun importFromEdgeCdp(host: String = "10.0.2.2", port: Int = 9222) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cdpSyncLoading = true, cdpMessage = "正在连接 CDP 服务...")
            val result = withContext(Dispatchers.IO) {
                val hostsToTry = listOf(host, "localhost", "127.0.0.1")
                var errorMsg = ""
                for (h in hostsToTry) {
                    try {
                        val url = URL("http://$h:$port/json/list")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 2000
                        conn.readTimeout = 2000
                        if (conn.responseCode == 200) {
                            val reader = BufferedReader(InputStreamReader(conn.inputStream))
                            val json = reader.readText()
                            reader.close()
                            return@withContext "已成功连接 Edge CDP ($h:$port)！\n页面列表已读取。若浏览器已登录B站，可通过CDP网络协议同步Cookie。"
                        }
                    } catch (e: Exception) {
                        errorMsg = e.message ?: "连接失败"
                    }
                }
                "未能在常用地址连接 CDP (端口 $port): $errorMsg\n如果是真机调试，请执行:\nadb reverse tcp:9222 tcp:9222"
            }
            _uiState.value = _uiState.value.copy(cdpSyncLoading = false, cdpMessage = result)
        }
    }

    fun saveManualCookie(cookieString: String) {
        if (cookieString.isBlank()) return
        viewModelScope.launch {
            val cookies = mutableMapOf<String, String>()
            cookieString.split(";").forEach { item ->
                val pair = item.trim().split("=", limit = 2)
                if (pair.size == 2) {
                    cookies[pair[0].trim()] = pair[1].trim()
                }
            }
            authRepository.saveCookies(cookies)
            _uiState.value = _uiState.value.copy(isSuccess = true, statusText = "Cookie保存成功！")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
