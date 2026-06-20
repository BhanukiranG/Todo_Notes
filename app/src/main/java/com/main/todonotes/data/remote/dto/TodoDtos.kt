package com.main.todonotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TodoDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("isCompleted") val isCompleted: Boolean
)

data class TodoRequestDto(
    @SerializedName("title") val title: String,
    @SerializedName("isCompleted") val isCompleted: Boolean? = null
)
