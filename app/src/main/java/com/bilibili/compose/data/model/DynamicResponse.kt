package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

/**
 * Dynamic portal API response data (/x/polymer/web-dynamic/v1/portal)
 */
data class DynamicPortalData(
    @SerializedName("my_info")
    val myInfo: DynamicMyInfo? = null,
    @SerializedName("up_list")
    val upList: DynamicUpList? = null
)

data class DynamicMyInfo(
    val mid: String? = null,
    val name: String? = null,
    val face: String? = null
)

data class DynamicUpList(
    @SerializedName("has_more")
    val hasMore: Boolean = false,
    val items: List<DynamicCreator> = emptyList(),
    val offset: String? = null
)

/**
 * Creator/UP item in dynamic up_list
 */
data class DynamicCreator(
    val mid: Long = 0,
    val uname: String = "",
    val face: String = "",
    @SerializedName("has_update")
    val hasUpdate: Boolean = false
)

/**
 * Dynamic feed API response data (/x/polymer/web-dynamic/v1/feed/all & /feed/space)
 */
data class DynamicFeedData(
    @SerializedName("has_more")
    val hasMore: Boolean = false,
    val items: List<DynamicItem> = emptyList(),
    val offset: String? = null,
    @SerializedName("update_baseline")
    val updateBaseline: String? = null
)

/**
 * Single dynamic item
 */
data class DynamicItem(
    @SerializedName("id_str")
    val idStr: String = "",
    val type: String = "",
    val modules: DynamicModules? = null,
    val orig: DynamicItem? = null
) {
    /**
     * Convenient helper to get author name
     */
    val authorName: String
        get() = modules?.moduleAuthor?.name.orEmpty()

    /**
     * Convenient helper to get author mid
     */
    val authorMid: Long
        get() = modules?.moduleAuthor?.mid ?: 0L

    /**
     * Convenient helper to get text description
     */
    val contentText: String
        get() = modules?.moduleDynamic?.desc?.text
            ?: modules?.moduleDynamic?.major?.opus?.summary?.text
            ?: ""

    /**
     * Convenient helper to get video archive info if type is AV
     */
    val videoArchive: DynamicArchive?
        get() = modules?.moduleDynamic?.major?.archive

    /**
     * Convenient helper to get picture list (from draw or opus)
     */
    val pictureUrls: List<String>
        get() {
            val drawPics = modules?.moduleDynamic?.major?.draw?.items?.mapNotNull { it.src }
            if (!drawPics.isNullOrEmpty()) return drawPics
            val opusPics = modules?.moduleDynamic?.major?.opus?.pics?.mapNotNull { it.url }
            if (!opusPics.isNullOrEmpty()) return opusPics
            return emptyList()
        }
}

data class DynamicModules(
    @SerializedName("module_author")
    val moduleAuthor: DynamicAuthor? = null,
    @SerializedName("module_dynamic")
    val moduleDynamic: DynamicContent? = null,
    @SerializedName("module_stat")
    val moduleStat: DynamicStat? = null
)

data class DynamicAuthor(
    val mid: Long = 0,
    val name: String = "",
    val face: String = "",
    @SerializedName("pub_time")
    val pubTime: String? = null,
    @SerializedName("pub_time_text")
    val pubTimeText: String? = null,
    @SerializedName("pub_action")
    val pubAction: String? = null,
    val vip: DynamicVip? = null
)

data class DynamicVip(
    val label: DynamicVipLabel? = null
)

data class DynamicVipLabel(
    val text: String? = null
)

data class DynamicContent(
    val desc: DynamicDesc? = null,
    val major: DynamicMajor? = null
)

data class DynamicDesc(
    val text: String? = null
)

data class DynamicMajor(
    val type: String? = null,
    val archive: DynamicArchive? = null,
    val draw: DynamicDraw? = null,
    val opus: DynamicOpus? = null
)

data class DynamicArchive(
    val aid: String? = null,
    val bvid: String? = null,
    val title: String? = null,
    val cover: String? = null,
    @SerializedName("duration_text")
    val durationText: String? = null,
    val stat: ArchiveStat? = null
)

data class ArchiveStat(
    val play: String? = null,
    val danmaku: String? = null
)

data class DynamicDraw(
    val items: List<DynamicDrawItem>? = null
)

data class DynamicDrawItem(
    val src: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

data class DynamicOpus(
    val title: String? = null,
    val summary: OpusSummary? = null,
    val pics: List<DynamicPicItem>? = null
)

data class OpusSummary(
    val text: String? = null
)

data class DynamicPicItem(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

data class DynamicStat(
    val comment: StatCount? = null,
    val forward: StatCount? = null,
    val like: StatCount? = null
)

data class StatCount(
    val count: Int = 0
)
