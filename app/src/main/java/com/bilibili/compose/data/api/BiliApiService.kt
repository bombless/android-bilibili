package com.bilibili.compose.data.api

import com.bilibili.compose.data.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BiliApiService {

    @GET("x/web-interface/history/cursor")
    suspend fun getHistoryCursor(
        @Query("max") max: Long = 0,
        @Query("view_at") viewAt: Long = 0,
        @Query("business") business: String = "",
        @Query("ps") pageSize: Int = 20,
        @Query("type") type: String = "all"
    ): BiliResponse<HistoryData>

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

    @GET("x/v2/reply/main")
    suspend fun getVideoReplies(
        @Query("type") type: Int = 1,
        @Query("oid") aid: Long,
        @Query("mode") mode: Int = 3,
        @Query("next") next: Long = 0,
        @Query("ps") pageSize: Int = 20
    ): BiliResponse<ReplyData>

    @GET("x/v2/reply/reply")
    suspend fun getSubReplies(
        @Query("type") type: Int = 1,
        @Query("oid") aid: Long,
        @Query("root") rootRpid: Long,
        @Query("pn") page: Int = 1,
        @Query("ps") pageSize: Int = 20
    ): BiliResponse<SubReplyData>

    @GET("x/web-interface/nav")
    suspend fun getNav(): BiliResponse<NavData>

    @GET("x/polymer/web-dynamic/v1/portal")
    suspend fun getDynamicPortal(
        @Query("up_list_more") upListMore: Int = 1
    ): BiliResponse<DynamicPortalData>

    @GET("x/polymer/web-dynamic/v1/feed/all")
    suspend fun getAllDynamics(
        @Query("timezone_offset") timezoneOffset: Int = -480,
        @Query("type") type: String = "all",
        @Query("offset") offset: String? = null,
        @Query("page") page: Int = 1
    ): BiliResponse<DynamicFeedData>

    @GET("x/polymer/web-dynamic/v1/feed/space")
    suspend fun getSpaceDynamics(
        @Query("host_mid") hostMid: Long,
        @Query("timezone_offset") timezoneOffset: Int = -480,
        @Query("offset") offset: String? = null,
        @Query("page") page: Int = 1
    ): BiliResponse<DynamicFeedData>
}

interface BiliPassportService {

    @GET("x/passport-login/web/qrcode/generate")
    suspend fun generateQrCode(): BiliResponse<QrGenerateData>

    @GET("x/passport-login/web/qrcode/poll")
    suspend fun pollQrCode(
        @Query("qrcode_key") key: String
    ): Response<BiliResponse<QrPollData>>
}
