package com.example.speechtotextapp.model

// ── Google Vertex AI Text-to-Speech (Chirp3-HD via Vertex AI endpoint) ────────
// Endpoint: us-central1-aiplatform.googleapis.com
// Uses OAuth2 Bearer token (not API key)

data class VertexTtsSynthesizeRequest(
    val input: VertexTtsInput,
    val voice: VertexTtsVoice,
    val audioConfig: VertexTtsAudioConfig
)

data class VertexTtsInput(
    val text: String
)

data class VertexTtsVoice(
    val languageCode: String,   // e.g. "en-US"
    val name: String            // e.g. "en-US-Studio-O"
)

data class VertexTtsAudioConfig(
    val audioEncoding: String = "LINEAR16",  // or "MP3", "OGG_OPUS"
    val speakingRate: Double = 1.0,          // 0.25 to 4.0
    val pitch: Double = 0.0,                 // -20.0 to 20.0
    val volumeGainDb: Double = 0.0           // -96.0 to 16.0
)

data class VertexTtsSynthesizeResponse(
    val audioContent: String?   // base64-encoded audio
)