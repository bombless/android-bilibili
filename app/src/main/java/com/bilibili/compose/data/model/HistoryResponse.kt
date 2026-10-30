package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class HistoryData(
    @SerializedName("cursor") val cursor: HistoryCursor = HistoryCursor(),
    @SerializedName("list") val list: List<HistoryEntry> = emptyList()
)

data class HistoryCursor(
    @SerializedName("max") val max: Long = 0,
    @SerializedName("view_at") val viewAt: Long = 0,
    @SerializedName("business") val business: String = ""
)

data class HistoryEntry(
    @SerializedName("title") val title: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("uri") val uri: String = "",
    @SerializedName("history") val history: HistoryTarget = HistoryTarget(),
    @SerializedName("author_name") val authorName: String = "",
    @SerializedName("author_face") val authorFace: String = "",
    @SerializedName("author_mid") val authorMid: Long = 0,
    @SerializedName("view_at") val viewAt: Long = 0,
    @SerializedName("progress") val progress: Long = 0,
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("business") val business: String = "",
    @SerializedName("kid") val kid: Long = 0
) {
    fun toVideoItem(): VideoItem = VideoItem(
        aid = history.oid,
        bvid = history.bvid,
        cid = history.cid,
        title = title,
        pic = cover,
        duration = duration,
        owner = Owner(mid = authorMid, name = authorName, face = authorFace)
    )
}

data class HistoryTarget(
    @SerializedName("oid") val oid: Long = 0,
    @SerializedName("bvid") val bvid: String = "",
    @SerializedName("cid") val cid: Long = 0,
    @SerializedName("business") val business: String = "archive"
)