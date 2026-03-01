package com.example.speechtotextapp.model

// ── Google Cloud TTS (Chirp3-HD) - standard texttospeech.googleapis.com ──────

data class TtsSynthesizeRequest(
    val input: TtsInput,
    val voice: TtsVoice,
    val audioConfig: TtsAudioConfig
)

data class TtsInput(
    val text: String
    // Note: Chirp3-HD does NOT support prompt or SSML — plain text only
)

data class TtsVoice(
    val languageCode: String,  // e.g. "en-US"
    val name: String           // e.g. "en-US-Chirp3-HD-Charon"
    // Note: Chirp3-HD does NOT support speakingRate or pitch in audioConfig
)

data class TtsAudioConfig(
    val audioEncoding: String = "LINEAR16"
    // Chirp3-HD does NOT support pitch / speakingRate params
)

data class TtsSynthesizeResponse(
    val audioContent: String?   // base64-encoded LINEAR16 audio @ 24kHz
)