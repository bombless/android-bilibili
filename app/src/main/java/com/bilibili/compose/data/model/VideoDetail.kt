package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class VideoViewData(
    @SerializedName("bvid")
    val bvid: String = "",
    @SerializedName("aid")
    val aid: Long = 0,
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("pubdate")
    val pubdate: Long = 0,
    @SerializedName("desc")
    val desc: String = "",
    @SerializedName("pic")
    val pic: String = "",
    @SerializedName("duration")
    val duration: Long = 0,
    @SerializedName("owner")
    val owner: Owner? = null,
    @SerializedName("stat")
    val stat: Stat? = null,
    @SerializedName("pages")
    val pages: List<VideoPage> = emptyList()
)

data class VideoPage(
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("from")
    val from: String = "",
    @SerializedName("part")
    val part: String = "",
    @SerializedName("duration")
    val duration: Long = 0,
    @SerializedName("vid")
    val vid: String = "",
    @SerializedName("weblink")
    val weblink: String = ""
)
