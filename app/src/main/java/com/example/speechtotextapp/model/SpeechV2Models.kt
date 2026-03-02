package com.example.speechtotextapp.model

// ── Google Cloud Speech-to-Text V2 ────────────────────────────────────────────
// Endpoint: POST /v2/projects/{PROJECT_ID}/locations/global/recognizers/_:recognize
// Key differences from V1:
//   - config.languageCode  →  config.languageCodes (array)
//   - config.encoding      →  config.autoDecodingConfig: {} (auto detect)
//   - config.sampleRateHertz → removed (auto detected)
//   - model field added    →  "chirp", "long", "short", "telephony" etc.
//   - audio.content        →  top-level "content" field (not nested under "audio")

data class SpeechV2Request(
    val recognizer: String,          // "projects/{id}/locations/global/recognizers/_"
    val config: SpeechV2Config,
    val content: String              // base64 audio — top-level in V2, not nested
)
// In your model file, add explicit encoding config
data class ExplicitDecodingConfig(
    val encoding: String,
    val sampleRateHertz: Int,
    val audioChannelCount: Int
)
data class SpeechV2Config(
    val explicitDecodingConfig: ExplicitDecodingConfig,
    val languageCodes: List<String>,  // e.g. ["en-US"] — array in V2
    val model: String                 // "chirp", "long", "short", "telephony", "latest_long"
)

// Empty object — tells V2 to auto-detect encoding/sampleRate/channels
class AutoDecodingConfig

// ── V2 Response ───────────────────────────────────────────────────────────────
data class SpeechV2Response(
    val results: List<SpeechV2Result>?
)

data class SpeechV2Result(
    val alternatives: List<SpeechV2Alternative>?,
    val languageCode: String?          // V2 tells you which language it detected
)

data class SpeechV2Alternative(
    val transcript: String?,
    val confidence: Float?
)
