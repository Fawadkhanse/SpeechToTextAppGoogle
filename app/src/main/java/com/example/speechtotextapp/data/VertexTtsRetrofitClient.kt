package com.example.speechtotextapp.data

import com.example.speechtotextapp.model.VertexTtsSynthesizeRequest
import com.example.speechtotextapp.model.VertexTtsSynthesizeResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Uses API key (same as Chirp3-HD) — no Bearer token, never expires
interface VertexTtsApiService {
    @POST("v1/text:synthesize")
    suspend fun synthesize(
        @Query("key") apiKey: String,
        @Body request: VertexTtsSynthesizeRequest
    ): Response<VertexTtsSynthesizeResponse>
}

object VertexTtsRetrofitClient {

    // Same endpoint as Chirp3-HD — Studio & Neural2 voices work here too
    private const val BASE_URL = "https://texttospeech.googleapis.com/"
    private val service: VertexTtsApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VertexTtsApiService::class.java)
    }

    suspend fun synthesize(
        apiKey: String,
        request: VertexTtsSynthesizeRequest
    ): Response<VertexTtsSynthesizeResponse> = service.synthesize(apiKey, request)
}