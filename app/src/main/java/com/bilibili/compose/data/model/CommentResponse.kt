package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class ReplyData(
    @SerializedName("page")
    val page: ReplyPage? = null,
    @SerializedName("replies")
    val replies: List<ReplyItem>? = emptyList(),
    @SerializedName("top")
    val top: ReplyTop? = null
)

data class ReplyPage(
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("num")
    val num: Int = 1,
    @SerializedName("size")
    val size: Int = 20
)

data class ReplyTop(
    @SerializedName("upper")
    val upper: ReplyItem? = null
)

data class ReplyItem(
    @SerializedName("rpid")
    val rpid: Long = 0,
    @SerializedName("oid")
    val oid: Long = 0,
    @SerializedName("type")
    val type: Int = 1,
    @SerializedName("mid")
    val mid: Long = 0,
    @SerializedName("root")
    val root: Long = 0,
    @SerializedName("parent")
    val parent: Long = 0,
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("rcount")
    val rcount: Int = 0,
    @SerializedName("like")
    val like: Int = 0,
    @SerializedName("ctime")
    val ctime: Long = 0,
    @SerializedName("member")
    val member: ReplyMember? = null,
    @SerializedName("content")
    val content: ReplyContent? = null
)

data class ReplyMember(
    @SerializedName("mid")
    val mid: String = "",
    @SerializedName("uname")
    val uname: String = "",
    @SerializedName("sex")
    val sex: String = "",
    @SerializedName("sign")
    val sign: String = "",
    @SerializedName("avatar")
    val avatar: String = ""
)

data class ReplyContent(
    @SerializedName("message")
    val message: String = "",
    @SerializedName("plat")
    val plat: Int = 0
)
