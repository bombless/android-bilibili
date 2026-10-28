package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

enum class PopularTab(val title: String) {
    OVERALL("综合热门"),
    RANKING("排行榜"),
    PRECIOUS("入站必刷")
}

data class VideoItem(
    @SerializedName(value = "aid", alternate = ["id"])
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
    val rcmdReason: RcmdReason? = null,
    @SerializedName("achievement")
    val achievement: String? = null,
    val rank: Int? = null
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

data class RcmdData(
    @SerializedName("item")
    val item: List<VideoItem> = emptyList()
)

data class PreciousData(
    @SerializedName("list")
    val list: List<VideoItem> = emptyList(),
    @SerializedName("title")
    val title: String = "",
    @SerializedName("explain")
    val explain: String = ""
)

data class RankingItem(
    @SerializedName("aid")
    val aid: String = "",
    @SerializedName("bvid")
    val bvid: String = "",
    @SerializedName("cid")
    val cid: Long = 0,
    @SerializedName("title")
    val title: String = "",
    @SerializedName("pic")
    val pic: String = "",
    @SerializedName("author")
    val author: String = "",
    @SerializedName("mid")
    val mid: Long = 0,
    @SerializedName("play")
    val play: Long = 0,
    @SerializedName("video_review")
    val videoReview: Long = 0,
    @SerializedName("duration")
    val duration: String = "",
    @SerializedName("pts")
    val pts: Long = 0
) {
    fun toVideoItem(rank: Int): VideoItem {
        return VideoItem(
            aid = aid.toLongOrNull() ?: 0,
            bvid = bvid,
            cid = cid,
            title = title,
            pic = pic,
            duration = parseDurationStringToSeconds(duration),
            owner = Owner(mid = mid, name = author),
            stat = Stat(view = play, danmaku = videoReview),
            rank = rank
        )
    }

    private fun parseDurationStringToSeconds(str: String): Long {
        val parts = str.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0L
        }
    }
}

data class RankingData(
    @SerializedName("list")
    val list: List<RankingItem> = emptyList(),
    @SerializedName("note")
    val note: String = ""
)
