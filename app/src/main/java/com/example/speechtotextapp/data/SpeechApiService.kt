package com.example.speechtotextapp.data

import com.example.speechtotextapp.model.SpeechRequest
import com.example.speechtotextapp.model.SpeechResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SpeechApiService {
    @POST("v1/speech:recognize")
    suspend fun recognize(
        @Query("key") apiKey: String,
        @Body request: SpeechRequest
    ): Response<SpeechResponse>
}