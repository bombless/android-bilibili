package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class NavData(
    @SerializedName("isLogin")
    val isLogin: Boolean = false,
    @SerializedName("email_verified")
    val emailVerified: Int = 0,
    @SerializedName("face")
    val face: String = "",
    @SerializedName("level_info")
    val levelInfo: LevelInfo? = null,
    @SerializedName("mid")
    val mid: Long = 0,
    @SerializedName("mobile_verified")
    val mobileVerified: Int = 0,
    @SerializedName("money")
    val money: Double = 0.0,
    @SerializedName("moral")
    val moral: Int = 0,
    @SerializedName("uname")
    val uname: String = "",
    @SerializedName("vipStatus")
    val vipStatus: Int = 0,
    @SerializedName("vipType")
    val vipType: Int = 0,
    @SerializedName("wallet")
    val wallet: Wallet? = null
)

data class LevelInfo(
    @SerializedName("current_level")
    val currentLevel: Int = 0,
    @SerializedName("current_min")
    val currentMin: Int = 0,
    @SerializedName("current_exp")
    val currentExp: Int = 0,
    @SerializedName("next_exp")
    val nextExp: String = ""
)

data class Wallet(
    @SerializedName("mid")
    val mid: Long = 0,
    @SerializedName("bcoin_balance")
    val bcoinBalance: Double = 0.0,
    @SerializedName("coupon_balance")
    val couponBalance: Double = 0.0
)
