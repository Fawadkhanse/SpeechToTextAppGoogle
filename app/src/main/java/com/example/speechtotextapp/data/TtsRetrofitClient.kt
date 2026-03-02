package com.example.speechtotextapp.data

import com.example.speechtotextapp.model.TtsSynthesizeRequest
import com.example.speechtotextapp.model.TtsSynthesizeResponse
import com.example.speechtotextapp.model.TtsV2SynthesizeRequest
import com.example.speechtotextapp.model.TtsV2SynthesizeResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ── v1: Chirp3-HD + Urdu Standard voices ─────────────────────────────────────
interface TtsApiService {
    @POST("v1/text:synthesize")
    suspend fun synthesize(
        @Query("key") apiKey: String,
        @Body request: TtsSynthesizeRequest
    ): Response<TtsSynthesizeResponse>
}

// ── v1beta1: Studio & Neural2 voices ─────────────────────────────────────────
// Studio/Neural2 also work on v1, but v1beta1 unlocks extra features.
// Same API key — no Bearer token needed.
interface TtsV2ApiService {
    @POST("v1beta1/text:synthesize")
    suspend fun synthesize(
        @Query("key") apiKey: String,
        @Body request: TtsV2SynthesizeRequest
    ): Response<TtsV2SynthesizeResponse>
}

object TtsRetrofitClient {

    private const val BASE_URL = "https://texttospeech.googleapis.com/"

    private fun buildClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(buildClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ── v1: Chirp3-HD / Urdu ─────────────────────────────────────────────────
    private val v1Service: TtsApiService by lazy {
        buildRetrofit().create(TtsApiService::class.java)
    }

    suspend fun synthesize(apiKey: String, request: TtsSynthesizeRequest): Response<TtsSynthesizeResponse> =
        v1Service.synthesize(apiKey, request)

    // ── v1beta1: Studio / Neural2 ─────────────────────────────────────────────
    private val v2Service: TtsV2ApiService by lazy {
        buildRetrofit().create(TtsV2ApiService::class.java)
    }

    suspend fun synthesizeV2(apiKey: String, request: TtsV2SynthesizeRequest): Response<TtsV2SynthesizeResponse> =
        v2Service.synthesize(apiKey, request)
}