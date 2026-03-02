package com.example.speechtotextapp.model

// ── Google Cloud TTS v1 (Chirp3-HD & Urdu Standard) ──────────────────────────
// Endpoint: texttospeech.googleapis.com/v1/text:synthesize

data class TtsSynthesizeRequest(
    val input: TtsInput,
    val voice: TtsVoice,
    val audioConfig: TtsAudioConfig
)

data class TtsInput(
    val text: String
)

data class TtsVoice(
    val languageCode: String,  // e.g. "en-US" or "ur-PK"
    val name: String           // e.g. "en-US-Chirp3-HD-Charon" or "ur-PK-Standard-A"
)

data class TtsAudioConfig(
    val audioEncoding: String = "LINEAR16",
    val sampleRateHertz: Int? = null   // null = use voice default (24000 for Chirp3-HD)
)

data class TtsSynthesizeResponse(
    val audioContent: String?  // base64-encoded LINEAR16 PCM audio
)

// ── Google Cloud TTS v1beta1 (Studio & Neural2) ───────────────────────────────
// Endpoint: texttospeech.googleapis.com/v1beta1/text:synthesize
// Studio and Neural2 voices work on v1 too, but v1beta1 unlocks extra features
// like effectsProfileId. Same API key — no Bearer token needed.

data class TtsV2SynthesizeRequest(
    val input: TtsInput,
    val voice: TtsV2Voice,
    val audioConfig: TtsV2AudioConfig
)

data class TtsV2Voice(
    val languageCode: String,  // e.g. "en-US"
    val name: String           // e.g. "en-US-Studio-O" or "en-US-Neural2-C"
)

data class TtsV2AudioConfig(
    val audioEncoding: String = "LINEAR16",
    val sampleRateHertz: Int  = 24000
)

data class TtsV2SynthesizeResponse(
    val audioContent: String?  // base64-encoded LINEAR16 PCM audio
)