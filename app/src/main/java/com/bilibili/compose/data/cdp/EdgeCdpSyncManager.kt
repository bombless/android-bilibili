package com.bilibili.compose.data.cdp

import android.util.Log
import com.bilibili.compose.data.api.BiliApiService
import com.bilibili.compose.data.local.UserSessionStore
import com.google.gson.JsonParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class CdpSyncResult(
    val success: Boolean,
    val message: String,
    val username: String? = null,
    val cookieCount: Int = 0
)

class EdgeCdpSyncManager(
    private val sessionStore: UserSessionStore,
    private val apiService: BiliApiService,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "EdgeCdpSyncManager"
        const val DEFAULT_PORT = 9222
    }

    /**
     * Try to connect to Edge CDP on port 9222, retrieve login cookies,
     * save them to sessionStore, and fetch the user profile.
     */
    suspend fun syncFromEdge(
        hosts: List<String> = listOf("10.0.2.2", "127.0.0.1", "localhost"),
        port: Int = DEFAULT_PORT
    ): CdpSyncResult = withContext(Dispatchers.IO) {
        var lastError: String? = null

        for (host in hosts) {
            try {
                Log.d(TAG, "Trying CDP on $host:$port...")
                val listUrl = URL("http://$host:$port/json/list")
                val conn = listUrl.openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                
                if (conn.responseCode != 200) {
                    lastError = "CDP HTTP ${conn.responseCode}"
                    continue
                }

                val jsonStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                conn.disconnect()

                val jsonArray = JsonParser.parseString(jsonStr).asJsonArray
                if (jsonArray.isEmpty) {
                    lastError = "Edge 浏览器未开启任何标签页"
                    continue
                }

                // Look for bilibili page first, otherwise take the first page
                var targetWsUrl: String? = null
                var matchedPageTitle = ""
                for (elem in jsonArray) {
                    val obj = elem.asJsonObject
                    val url = obj.get("url")?.asString.orEmpty()
                    val title = obj.get("title")?.asString.orEmpty()
                    val wsUrl = obj.get("webSocketDebuggerUrl")?.asString
                    
                    if (wsUrl != null) {
                        if (url.contains("bilibili.com") || title.contains("哔哩哔哩") || title.contains("动态")) {
                            targetWsUrl = wsUrl
                            matchedPageTitle = title
                            break
                        }
                        if (targetWsUrl == null && obj.get("type")?.asString == "page") {
                            targetWsUrl = wsUrl
                            matchedPageTitle = title
                        }
                    }
                }

                if (targetWsUrl == null) {
                    lastError = "未能找到可用的页面 WebSocket 调试地址"
                    continue
                }

                // Normalize host in wsUrl if connecting from emulator (e.g. replace 127.0.0.1 with 10.0.2.2)
                val normalizedWsUrl = if (host != "127.0.0.1" && host != "localhost") {
                    targetWsUrl.replace("127.0.0.1", host).replace("localhost", host)
                } else {
                    targetWsUrl
                }

                Log.d(TAG, "Connecting to WebSocket: $normalizedWsUrl for page '$matchedPageTitle'")
                val cookies = fetchCookiesViaWebSocket(normalizedWsUrl)
                if (cookies.isEmpty()) {
                    lastError = "未能从 Edge 读取到有效 Cookie（可能未登录 Bilibili）"
                    continue
                }

                // Save cookies to session store
                sessionStore.saveCookies(cookies)

                // Verify login by fetching user nav
                val navResp = try {
                    apiService.getNav()
                } catch (e: Exception) {
                    null
                }

                val uname = navResp?.data?.uname
                if (navResp?.data?.isLogin == true && !uname.isNullOrBlank()) {
                    sessionStore.saveUserNav(navResp.data)
                    return@withContext CdpSyncResult(
                        success = true,
                        message = "成功从 Edge 浏览器同步会话！欢迎，$uname",
                        username = uname,
                        cookieCount = cookies.size
                    )
                } else {
                    return@withContext CdpSyncResult(
                        success = true,
                        message = "已同步 ${cookies.size} 个 Cookie，但账号可能尚未在 Edge 登录",
                        username = null,
                        cookieCount = cookies.size
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync from $host:$port: ${e.message}")
                lastError = e.message ?: "连接异常"
            }
        }

        CdpSyncResult(
            success = false,
            message = "连接 Edge CDP 失败 ($lastError)。请确保 Edge 以 --remote-debugging-port=$port 启动。\n如果是真机调试，请运行: adb reverse tcp:$port tcp:$port"
        )
    }

    /**
     * Connect to the CDP WebSocket and call Network.getCookies
     */
    private suspend fun fetchCookiesViaWebSocket(wsUrl: String): Map<String, String> {
        val deferred = CompletableDeferred<Map<String, String>>()
        val request = Request.Builder().url(wsUrl).build()

        val wsClient = okHttpClient.newBuilder()
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        var webSocketRef: WebSocket? = null
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocketRef = webSocket
                val getCookiesCommand = """
                    {
                        "id": 1001,
                        "method": "Network.getCookies",
                        "params": {
                            "urls": [
                                "https://bilibili.com",
                                "https://api.bilibili.com",
                                "https://t.bilibili.com"
                            ]
                        }
                    }
                """.trimIndent()
                webSocket.send(getCookiesCommand)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JsonParser.parseString(text).asJsonObject
                    if (root.get("id")?.asInt == 1001) {
                        val cookiesMap = mutableMapOf<String, String>()
                        val result = root.getAsJsonObject("result")
                        val cookiesArray = result?.getAsJsonArray("cookies")
                        cookiesArray?.forEach { elem ->
                            val cookieObj = elem.asJsonObject
                            val name = cookieObj.get("name")?.asString.orEmpty()
                            val value = cookieObj.get("value")?.asString.orEmpty()
                            if (name in listOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5")) {
                                cookiesMap[name] = value
                            }
                        }
                        deferred.complete(cookiesMap)
                        webSocket.close(1000, "Done")
                    }
                } catch (e: Exception) {
                    deferred.completeExceptionally(e)
                    webSocket.close(1001, "Parse Error")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(t)
                }
            }
        }

        wsClient.newWebSocket(request, listener)

        return withTimeoutOrNull(4000) {
            try {
                deferred.await()
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()
    }
}
