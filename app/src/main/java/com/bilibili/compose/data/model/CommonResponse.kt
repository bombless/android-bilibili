package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class BiliResponse<T>(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String = "",
    @SerializedName("ttl")
    val ttl: Int = 0,
    @SerializedName("data")
    val data: T? = null
) {
    val isSuccess: Boolean get() = code == 0
}
