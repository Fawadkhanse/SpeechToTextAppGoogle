package com.example.speechtotextapp.model

// ── Google Cloud Speech-to-Text V2 ────────────────────────────────────────────
// Endpoint: POST /v2/projects/{PROJECT_ID}/locations/{location}/recognizers/_:recognize
// Key differences from V1:
//   - config.languageCode     → config.languageCodes (array)
//   - config.encoding         → config.explicitDecodingConfig (raw PCM from AudioRecord)
//                               OR config.autoDecodingConfig (container formats only)
//   - config.sampleRateHertz  → moved inside explicitDecodingConfig
//   - model field added       → "chirp", "long", "short", "telephony", "latest_long"
//   - audio.content           → top-level "content" field (not nested under "audio")

data class SpeechV2Request(
    val recognizer: String,   // "projects/{id}/locations/global/recognizers/_"
    val config: SpeechV2Config,
    val content: String       // base64 audio — top-level in V2, not nested
)

data class SpeechV2Config(
    val explicitDecodingConfig: ExplicitDecodingConfig? = null,
    val autoDecodingConfig: AutoDecodingConfig? = null,
    val languageCodes: List<String>,   // e.g. ["en-US"] — array in V2
    val model: String                  // "chirp", "long", "short", "telephony", "latest_long"
)

// Use this for raw PCM from Android AudioRecord
data class ExplicitDecodingConfig(
    val encoding: String,        // "LINEAR16"
    val sampleRateHertz: Int,    // 16000
    val audioChannelCount: Int   // 1 (mono)
)

// Empty object — only works with container formats (OGG, FLAC, MP4), NOT raw PCM
class AutoDecodingConfig

// ── V2 Response ───────────────────────────────────────────────────────────────
data class SpeechV2Response(
    val results: List<SpeechV2Result>?
)

data class SpeechV2Result(
    val alternatives: List<SpeechV2Alternative>?,
    val languageCode: String?   // V2 reports which language was detected
)

data class SpeechV2Alternative(
    val transcript: String?,
    val confidence: Float?
)