package com.example.speechtotextapp.data

import com.example.speechtotextapp.model.SpeechRequest
import com.example.speechtotextapp.model.SpeechResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://speech.googleapis.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val speechApi: SpeechApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpeechApiService::class.java)
    }

    // ✅ This is the function MainActivity calls: RetrofitClient.recognize(...)
    suspend fun recognize(apiKey: String, request: SpeechRequest): Response<SpeechResponse> {
        return speechApi.recognize(apiKey, request)
    }
}