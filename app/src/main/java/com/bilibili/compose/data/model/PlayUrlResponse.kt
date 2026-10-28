package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class PlayUrlData(
    @SerializedName("from")
    val from: String = "",
    @SerializedName("result")
    val result: String = "",
    @SerializedName("message")
    val message: String = "",
    @SerializedName("quality")
    val quality: Int = 0,
    @SerializedName("format")
    val format: String = "",
    @SerializedName("timelength")
    val timelength: Long = 0,
    @SerializedName("accept_format")
    val acceptFormat: String = "",
    @SerializedName("accept_description")
    val acceptDescription: List<String> = emptyList(),
    @SerializedName("accept_quality")
    val acceptQuality: List<Int> = emptyList(),
    @SerializedName("video_codecid")
    val videoCodecid: Int = 0,
    @SerializedName("durl")
    val durl: List<DurlItem> = emptyList()
)

data class DurlItem(
    @SerializedName("order")
    val order: Int = 0,
    @SerializedName("length")
    val length: Long = 0,
    @SerializedName("size")
    val size: Long = 0,
    @SerializedName("ahead")
    val ahead: String = "",
    @SerializedName("vhead")
    val vhead: String = "",
    @SerializedName("url")
    val url: String = "",
    @SerializedName("backup_url")
    val backupUrl: List<String> = emptyList()
)
