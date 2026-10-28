package com.bilibili.compose.data.api

import com.bilibili.compose.data.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BiliApiService {

    @GET("x/web-interface/popular")
    suspend fun getPopularVideos(
        @Query("pn") page: Int,
        @Query("ps") pageSize: Int = 20
    ): BiliResponse<PopularData>

    @GET("x/web-interface/index/top/feed/rcmd")
    suspend fun getRecommendVideos(
        @Query("ps") pageSize: Int = 10,
        @Query("fresh_idx") freshIdx: Int = 1
    ): BiliResponse<RcmdData>

    @GET("x/web-interface/ranking")
    suspend fun getRankingVideos(
        @Query("rid") rid: Int = 0,
        @Query("day") day: Int = 3
    ): BiliResponse<RankingData>

    @GET("x/web-interface/popular/precious")
    suspend fun getPreciousVideos(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): BiliResponse<PreciousData>

    @GET("x/web-interface/view")
    suspend fun getVideoView(
        @Query("bvid") bvid: String
    ): BiliResponse<VideoViewData>

    @GET("x/player/playurl")
    suspend fun getPlayUrl(
        @Query("bvid") bvid: String,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int = 64,
        @Query("fnval") fnval: Int = 1
    ): BiliResponse<PlayUrlData>

    @GET("x/web-interface/archive/related")
    suspend fun getRelatedVideos(
        @Query("bvid") bvid: String
    ): BiliResponse<List<VideoItem>>

    @GET("x/v2/reply")
    suspend fun getVideoReplies(
        @Query("type") type: Int = 1,
        @Query("oid") aid: Long,
        @Query("pn") page: Int = 1,
        @Query("ps") pageSize: Int = 20,
        @Query("sort") sort: Int = 2
    ): BiliResponse<ReplyData>

    @GET("x/web-interface/nav")
    suspend fun getNav(): BiliResponse<NavData>
}

interface BiliPassportService {

    @GET("x/passport-login/web/qrcode/generate")
    suspend fun generateQrCode(): BiliResponse<QrGenerateData>

    @GET("x/passport-login/web/qrcode/poll")
    suspend fun pollQrCode(
        @Query("qrcode_key") key: String
    ): Response<BiliResponse<QrPollData>>
}
