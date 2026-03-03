package com.example.speechtotextapp.data

import com.example.speechtotextapp.model.TtsV2NewSynthesizeRequest
import com.example.speechtotextapp.model.TtsV2NewSynthesizeResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface TtsV2NewApiService {
    // ✅ FIXED: TTS API only supports v1 (v2 endpoint does not exist)
    @POST("v1/text:synthesize")
    suspend fun synthesize(
        @Query("key") apiKey: String,
        @Body request: TtsV2NewSynthesizeRequest
    ): Response<TtsV2NewSynthesizeResponse>
}

object TtsV2NewRetrofitClient {

    // ✅ Regional endpoint required for Chirp3-HD voices (global doesn't serve them)
    private const val BASE_URL = "https://us-texttospeech.googleapis.com/"

    val service: TtsV2NewApiService by lazy {
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
            .create(TtsV2NewApiService::class.java)
    }

    suspend fun synthesize(
        apiKey: String,
        request: TtsV2NewSynthesizeRequest
    ): Response<TtsV2NewSynthesizeResponse> = service.synthesize(apiKey, request)
}