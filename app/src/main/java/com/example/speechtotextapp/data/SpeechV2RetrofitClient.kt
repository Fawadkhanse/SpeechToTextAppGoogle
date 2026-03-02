package com.example.speechtotextapp.data

import com.example.speechtotextapp.model.ServiceAccountAuth
import com.example.speechtotextapp.model.SpeechV2Request
import com.example.speechtotextapp.model.SpeechV2Response
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// ── STT V2 uses Bearer token (not API key) ────────────────────────────────────
interface SpeechV2ApiService {
    @POST("v2/projects/{projectId}/locations/{location}/recognizers/{recognizer}:recognize")
    suspend fun recognize(
        @Header("Authorization") bearerToken: String,
        @Path("projectId")       projectId:   String,
        @Path("location")        location:    String,
        @Path("recognizer")      recognizer:  String,
        @Body                    request:     SpeechV2Request
    ): Response<SpeechV2Response>
}

object SpeechV2RetrofitClient {

    private const val BASE_URL   = "https://speech.googleapis.com/"
    const val PROJECT_ID         = "hale-carport-488819-q4"

    val service: SpeechV2ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpeechV2ApiService::class.java)
    }

    /**
     * Chirp model is only available in "us-central1".
     * All other models use "global".
     */
    fun locationForModel(model: String): String =
        if (model == "chirp") "us-central1" else "global"

    /**
     * Gets a fresh/cached OAuth2 Bearer token automatically,
     * then calls the STT V2 recognize endpoint.
     */
    suspend fun recognize(request: SpeechV2Request, model: String): Response<SpeechV2Response> {
        val token    = ServiceAccountAuth.getBearerToken()
        val location = locationForModel(model)
        return service.recognize(
            bearerToken = "Bearer $token",
            projectId   = PROJECT_ID,
            location    = location,
            recognizer  = "_",
            request     = request
        )
    }
}