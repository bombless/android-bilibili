package com.bilibili.compose

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.bilibili.compose.data.api.RetrofitClient
import com.bilibili.compose.data.local.UserSessionStore
import com.bilibili.compose.data.repository.AuthRepository
import com.bilibili.compose.data.repository.VideoRepository
import okhttp3.OkHttpClient

class BiliApp : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: BiliApp
            private set
    }

    lateinit var sessionStore: UserSessionStore
        private set
    lateinit var retrofitClient: RetrofitClient
        private set
    lateinit var videoRepository: VideoRepository
        private set
    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        sessionStore = UserSessionStore(this)
        retrofitClient = RetrofitClient(sessionStore)
        videoRepository = VideoRepository(retrofitClient.apiService)
        authRepository = AuthRepository(
            passportService = retrofitClient.passportService,
            apiService = retrofitClient.apiService,
            sessionStore = sessionStore
        )
    }

    override fun newImageLoader(): ImageLoader {
        val imageOkHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", RetrofitClient.USER_AGENT)
                    .header("Referer", RetrofitClient.REFERER)
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageOkHttp)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
