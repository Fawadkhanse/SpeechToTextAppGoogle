package com.example.speechtotextapp.data

import com.example.speechtotextapp.model.TtsSynthesizeRequest
import com.example.speechtotextapp.model.TtsSynthesizeResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TtsApiService {
    // Same base URL as Speech-to-Text — no Vertex AI needed
    @POST("v1/text:synthesize")
    suspend fun synthesize(
        @Query("key") apiKey: String,
        @Body request: TtsSynthesizeRequest
    ): Response<TtsSynthesizeResponse>
}

object TtsRetrofitClient {

    // ✅ Standard Cloud TTS endpoint — same domain as STT, no Vertex AI
    private const val BASE_URL = "https://texttospeech.googleapis.com/"

    private val service: TtsApiService by lazy {
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
            .create(TtsApiService::class.java)
    }

    suspend fun synthesize(apiKey: String, request: TtsSynthesizeRequest) =
        service.synthesize(apiKey, request)
}