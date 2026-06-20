package com.main.todonotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthResponseDto(
    @SerializedName("token") val token: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("email") val email: String
)

data class RefreshRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

data class LogoutRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)
