package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class QrGenerateData(
    @SerializedName("url")
    val url: String = "",
    @SerializedName("qrcode_key")
    val qrcodeKey: String = ""
)

data class QrPollData(
    @SerializedName("url")
    val url: String = "",
    @SerializedName("refresh_token")
    val refreshToken: String = "",
    @SerializedName("timestamp")
    val timestamp: Long = 0,
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String = ""
)
