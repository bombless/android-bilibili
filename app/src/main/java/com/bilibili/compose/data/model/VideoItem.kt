package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class VideoItem(
    @SerializedName("aid")
    val aid: Long = 0,
    @SerializedName("bvid")
    val bvid: String = "",
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("pic")
    val pic: String = "",
    @SerializedName("desc")
    val desc: String = "",
    @SerializedName("duration")
    val duration: Long = 0,
    @SerializedName("pubdate")
    val pubdate: Long = 0,
    @SerializedName("owner")
    val owner: Owner? = null,
    @SerializedName("stat")
    val stat: Stat? = null,
    @SerializedName("rcmd_reason")
    val rcmdReason: RcmdReason? = null
)

data class Owner(
    @SerializedName("mid")
    val mid: Long = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("face")
    val face: String = ""
)

data class Stat(
    @SerializedName("aid")
    val aid: Long = 0,
    @SerializedName("view")
    val view: Long = 0,
    @SerializedName("danmaku")
    val danmaku: Long = 0,
    @SerializedName("reply")
    val reply: Long = 0,
    @SerializedName("favorite")
    val favorite: Long = 0,
    @SerializedName("coin")
    val coin: Long = 0,
    @SerializedName("share")
    val share: Long = 0,
    @SerializedName("like")
    val like: Long = 0
)

data class RcmdReason(
    @SerializedName("content")
    val content: String = ""
)

data class PopularData(
    @SerializedName("list")
    val list: List<VideoItem> = emptyList(),
    @SerializedName("no_more")
    val noMore: Boolean = false
)
