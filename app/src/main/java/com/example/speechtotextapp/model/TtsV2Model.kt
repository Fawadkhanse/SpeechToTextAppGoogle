package com.example.speechtotextapp.model

// ── Google Cloud TTS v2 ───────────────────────────────────────────────────────
// Endpoint: texttospeech.googleapis.com/v2/projects/{project}/locations/{location}/voices/{voice}:synthesize
// OR simpler: texttospeech.googleapis.com/v2/text:synthesize (API key supported)

data class TtsV2NewSynthesizeRequest(
    val input: TtsInput,
    val voice: TtsV2NewVoice,
    val audioConfig: TtsV2NewAudioConfig
)

data class TtsV2NewVoice(
    val languageCode: String,   // e.g. "en-US"
    val name: String            // e.g. "en-US-Chirp3-HD-Aoede"
)

data class TtsV2NewAudioConfig(
    val audioEncoding: String = "LINEAR16",
    val sampleRateHertz: Int = 24000
)

data class TtsV2NewSynthesizeResponse(
    val audioContent: String?   // base64-encoded LINEAR16 PCM audio
)
