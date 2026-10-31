package com.bilibili.compose.data.model

import com.google.gson.annotations.SerializedName

data class UserRelationData(
    @SerializedName("attribute") val attribute: Int = 0
)