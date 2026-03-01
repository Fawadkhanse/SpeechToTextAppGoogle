package com.example.speechtotextapp.model

data class SpeechRequest(
    val config: RecognitionConfig,
    val audio: RecognitionAudio
)

data class RecognitionConfig(
    val encoding: String = "LINEAR16",
    val sampleRateHertz: Int = 16000,
    val languageCode: String = "en-US"
)

data class RecognitionAudio(
    val content: String  // base64 encoded audio
)

// Response models
data class SpeechResponse(
    val results: List<SpeechResult>?
)

data class SpeechResult(
    val alternatives: List<SpeechAlternative>?
)

data class SpeechAlternative(
    val transcript: String?,
    val confidence: Float?
)