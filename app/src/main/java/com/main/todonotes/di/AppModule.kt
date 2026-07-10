package com.main.todonotes.di

import com.main.todonotes.data.local.TokenManager
import com.main.todonotes.data.remote.AuthInterceptor
import com.main.todonotes.data.remote.TokenAuthenticator
import com.main.todonotes.data.remote.api.TodoApi
import com.main.todonotes.data.repository.AuthRepositoryImpl
import com.main.todonotes.data.repository.TodoRepositoryImpl
import com.main.todonotes.domain.repository.AuthRepository
import com.main.todonotes.domain.repository.TodoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val chuckerInterceptor = com.chuckerteam.chucker.api.ChuckerInterceptor.Builder(context).build()
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(chuckerInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://todo-web-api-dnit.onrender.com")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTodoApi(retrofit: Retrofit): TodoApi {
        return retrofit.create(TodoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: TodoApi,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepositoryImpl(api, tokenManager)
    }

    @Provides
    @Singleton
    fun provideTodoRepository(
        api: TodoApi,
        dao: com.main.todonotes.data.local.dao.TodoDao
    ): TodoRepository {
        return TodoRepositoryImpl(api, dao)
    }
}
