package com.example.speechtotextapp.data

import com.example.speechtotextapp.model.VertexTtsSynthesizeRequest
import com.example.speechtotextapp.model.VertexTtsSynthesizeResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface VertexTtsApiService {
    // Vertex AI TTS endpoint — uses Bearer token (not API key)
    // Project: your-project-id, Location: us-central1
    @POST("v1/text:synthesize")
    suspend fun synthesize(
        @Header("Authorization") authorization: String,   // "Bearer <token>"
        @Body request: VertexTtsSynthesizeRequest
    ): Response<VertexTtsSynthesizeResponse>
}

object VertexTtsRetrofitClient {

    // ✅ Vertex AI TTS endpoint (same API surface as Cloud TTS but routed through Vertex)
    // Replace YOUR_PROJECT_ID with actual project id
    private const val BASE_URL = "https://us-central1-texttospeech.googleapis.com/"

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
        bearerToken: String,
        request: VertexTtsSynthesizeRequest
    ): Response<VertexTtsSynthesizeResponse> =
        service.synthesize("Bearer $bearerToken", request)
}
