package com.main.todonotes.data.remote.api

import com.main.todonotes.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TodoApi {
    @POST("/api/auth/register")
    suspend fun register(@Body request: AuthRequestDto): Response<Unit>

    @POST("/api/auth/login")
    suspend fun login(@Body request: AuthRequestDto): AuthResponseDto

    @POST("/api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): AuthResponseDto

    @POST("/api/auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto): Response<Unit>

    @GET("/api/v1/Todos")
    suspend fun getTodos(): List<TodoDto>

    @POST("/api/v1/Todos")
    suspend fun createTodo(@Body request: TodoRequestDto): Response<Unit>

    @PUT("/api/v1/Todos/{id}")
    suspend fun updateTodo(
        @Path("id") id: String,
        @Body request: TodoRequestDto
    ): Response<Unit>

    @DELETE("/api/v1/Todos/{id}")
    suspend fun deleteTodo(@Path("id") id: String): Response<Unit>
}
