package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class ReplyData(
    @SerializedName("cursor")
    val cursor: ReplyCursor? = null,
    @SerializedName("page")
    val page: ReplyPage? = null,
    @SerializedName("replies")
    val replies: List<ReplyItem>? = emptyList(),
    @SerializedName("top_replies")
    val topReplies: List<ReplyItem>? = emptyList(),
    @SerializedName("top")
    val top: ReplyTop? = null
)

data class ReplyCursor(
    @SerializedName("is_begin")
    val isBegin: Boolean = true,
    @SerializedName("prev")
    val prev: Long = 0,
    @SerializedName("next")
    val next: Long = 0,
    @SerializedName("is_end")
    val isEnd: Boolean = false,
    @SerializedName("mode")
    val mode: Int = 3,
    @SerializedName("all_count")
    val allCount: Int = 0,
    @SerializedName("name")
    val name: String = ""
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

data class SubReplyData(
    @SerializedName("page")
    val page: ReplyPage? = null,
    @SerializedName("root")
    val root: ReplyItem? = null,
    @SerializedName("replies")
    val replies: List<ReplyItem>? = emptyList()
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
    val content: ReplyContent? = null,
    @SerializedName("replies")
    val replies: List<ReplyItem>? = null,
    @SerializedName("reply_control")
    val replyControl: ReplyControl? = null,
    var isTop: Boolean = false
)

data class ReplyControl(
    @SerializedName("time_desc")
    val timeDesc: String? = null,
    @SerializedName("sub_reply_entry_text")
    val subReplyEntryText: String? = null,
    @SerializedName("sub_reply_title_text")
    val subReplyTitleText: String? = null
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
    val avatar: String = "",
    @SerializedName("level_info")
    val levelInfo: LevelInfo? = null,
    @SerializedName("vip")
    val vip: ReplyVip? = null
)

data class ReplyVip(
    @SerializedName("vipType")
    val vipType: Int = 0,
    @SerializedName("vipStatus")
    val vipStatus: Int = 0,
    @SerializedName("nickname_color")
    val nicknameColor: String = ""
)

data class ReplyContent(
    @SerializedName("message")
    val message: String = "",
    @SerializedName("plat")
    val plat: Int = 0
)
