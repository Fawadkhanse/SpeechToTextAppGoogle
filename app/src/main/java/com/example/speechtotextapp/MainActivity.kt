package com.example.speechtotextapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.speechtotextapp.data.RetrofitClient
import com.example.speechtotextapp.data.SpeechV2RetrofitClient
import com.example.speechtotextapp.data.TtsRetrofitClient
import com.example.speechtotextapp.data.TtsV2NewRetrofitClient
import com.example.speechtotextapp.databinding.ActivityMainBinding

import com.example.speechtotextapp.helper.AudioRecorder
import com.example.speechtotextapp.model.ExplicitDecodingConfig
import com.example.speechtotextapp.model.RecognitionAudio
import com.example.speechtotextapp.model.RecognitionConfig
import com.example.speechtotextapp.model.SpeechRequest
import com.example.speechtotextapp.model.SpeechV2Config
import com.example.speechtotextapp.model.SpeechV2Request
import com.example.speechtotextapp.model.TtsAudioConfig
import com.example.speechtotextapp.model.TtsInput
import com.example.speechtotextapp.model.TtsSynthesizeRequest
import com.example.speechtotextapp.model.TtsV2AudioConfig
import com.example.speechtotextapp.model.TtsV2NewAudioConfig
import com.example.speechtotextapp.model.TtsV2NewSynthesizeRequest
import com.example.speechtotextapp.model.TtsV2NewVoice
import com.example.speechtotextapp.model.TtsV2SynthesizeRequest
import com.example.speechtotextapp.model.TtsV2Voice
import com.example.speechtotextapp.model.TtsVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class SttVersion { V1, V2 }
enum class TtsEngine { ANDROID, CHIRP_V1, CLOUD_V1BETA1 }

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // ── ViewBinding ───────────────────────────────────────────────────────────
    private lateinit var binding: ActivityMainBinding


    private lateinit var API_KEY: String
    private val RECORD_PERMISSION_CODE = 101

    // ── STT state ─────────────────────────────────────────────────────────────
    private var currentSttVersion = SttVersion.V1
    private var selectedV2Model   = "long"

    // ── V1 voice state ────────────────────────────────────────────────────────
    private var selectedUrduVoice           = "ur-IN-Standard-B"
    private var selectedAndroidVoiceIndex   = 0
    private var selectedChirpVoiceIndex     = 0
    private var selectedV2VoiceIndex        = 0
    private var selectedV2TtsVoiceIndex     = 0

    // ── Voice data models ─────────────────────────────────────────────────────
    data class AndroidVoiceProfile(val label: String, val pitch: Float, val speed: Float)
    private val androidVoices = listOf(
        AndroidVoiceProfile("Female 1", 1.2f, 1.0f), AndroidVoiceProfile("Male 1",   0.8f, 0.95f),
        AndroidVoiceProfile("Female 2", 1.5f, 1.1f), AndroidVoiceProfile("Male 2",   0.6f, 0.85f),
        AndroidVoiceProfile("Child",    1.8f, 1.2f), AndroidVoiceProfile("Robot",    0.5f, 0.7f)
    )

    data class ChirpVoice(val displayName: String, val gender: String, val voiceId: String)
    private val chirpVoices = listOf(
        ChirpVoice("Aoede",  "Female", "en-US-Chirp3-HD-Aoede"),
        ChirpVoice("Kore",   "Female", "en-US-Chirp3-HD-Kore"),
        ChirpVoice("Zephyr", "Female", "en-US-Chirp3-HD-Zephyr"),
        ChirpVoice("Leda",   "Female", "en-US-Chirp3-HD-Leda"),
        ChirpVoice("Charon", "Male",   "en-US-Chirp3-HD-Charon"),
        ChirpVoice("Fenrir", "Male",   "en-US-Chirp3-HD-Fenrir"),
        ChirpVoice("Orus",   "Male",   "en-US-Chirp3-HD-Orus"),
        ChirpVoice("Puck",   "Male",   "en-US-Chirp3-HD-Puck")
    )

    data class CloudV2Voice(val displayName: String, val gender: String, val voiceId: String)
    private val cloudV2Voices = listOf(
        CloudV2Voice("Studio-O",  "Female", "en-US-Studio-O"),
        CloudV2Voice("Studio-Q",  "Male",   "en-US-Studio-Q"),
        CloudV2Voice("Neural2-C", "Female", "en-US-Neural2-C"),
        CloudV2Voice("Neural2-D", "Male",   "en-US-Neural2-D"),
        CloudV2Voice("Neural2-F", "Female", "en-US-Neural2-F"),
        CloudV2Voice("Neural2-J", "Male",   "en-US-Neural2-J")
    )

    data class TtsV2Voice2(val displayName: String, val gender: String, val voiceId: String)
    private val ttsV2Voices = listOf(
        TtsV2Voice2("Aoede",  "Female", "en-US-Chirp3-HD-Aoede"),
        TtsV2Voice2("Kore",   "Female", "en-US-Chirp3-HD-Kore"),
        TtsV2Voice2("Zephyr", "Female", "en-US-Chirp3-HD-Zephyr"),
        TtsV2Voice2("Charon", "Male",   "en-US-Chirp3-HD-Charon"),
        TtsV2Voice2("Fenrir", "Male",   "en-US-Chirp3-HD-Fenrir"),
        TtsV2Voice2("Puck",   "Male",   "en-US-Chirp3-HD-Puck")
    )

    // ── Derived button lists (using binding) ──────────────────────────────────
    private val v2ModelButtons      get() = with(binding) { listOf(btnModelChirp, btnModelLong, btnModelShort, btnModelLatest) }
    private val v2Models                  = listOf("chirp", "long", "short", "latest_long")
    private val v2ModelLabels             = listOf("Chirp", "Long", "Short", "Latest")
    private val androidVoiceButtons get() = with(binding) { listOf(btnVoice1, btnVoice2, btnVoice3, btnVoice4, btnVoice5, btnVoice6) }
    private val chirpVoiceButtons   get() = with(binding) { listOf(btnVAchernar, btnVAoede, btnVKore, btnVZephyr, btnVCharon, btnVPuck, btnVFenrir, btnVOrus) }
    private val v2VoiceButtons      get() = with(binding) { listOf(btnVertexVoice1, btnVertexVoice2, btnVertexVoice3, btnVertexVoice4, btnVertexVoice5, btnVertexVoice6) }
    private val v2TtsVoiceButtons   get() = with(binding) { listOf(btnV2TtsVoice1, btnV2TtsVoice2, btnV2TtsVoice3, btnV2TtsVoice4, btnV2TtsVoice5, btnV2TtsVoice6) }

    // ── Playback / misc state ─────────────────────────────────────────────────
    private var audioTrack: AudioTrack? = null
    private var isSpeaking              = false
    private var androidTts: TextToSpeech? = null
    private var androidTtsReady         = false
    private val audioRecorder           = AudioRecorder()
    private var lastTranscript          = ""
    private var didStartRecording       = false
    private var selectedLangCode        = "en-US"
    private var selectedTtsLocale: Locale = Locale.US
    private var currentEngine           = TtsEngine.ANDROID

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Inflate with ViewBinding (replaces setContentView + all findViewById)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        API_KEY = getString(R.string.key)

        binding.btnSpeak.isEnabled = false
        androidTts = TextToSpeech(this, this)
        checkPermission()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // STT version
        binding.btnSttV1.setOnClickListener { selectSttVersion(SttVersion.V1) }
        binding.btnSttV2.setOnClickListener { selectSttVersion(SttVersion.V2) }
        v2ModelButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectV2Model(i) } }
        selectSttVersion(SttVersion.V1)
        selectV2Model(1)

        // Language
        binding.btnLangEn.setOnClickListener { selectLanguage("en") }
        binding.btnLangUr.setOnClickListener { selectLanguage("ur") }
        selectLanguage("en")

        // V1 TTS engine
        binding.btnEngineAndroid.setOnClickListener { selectEngine(TtsEngine.ANDROID) }
        binding.btnEngineChirp.setOnClickListener   { selectEngine(TtsEngine.CHIRP_V1) }
        binding.btnEngineVertex.setOnClickListener  { selectEngine(TtsEngine.CLOUD_V1BETA1) }
        selectEngine(TtsEngine.ANDROID)

        // Urdu voice
        binding.btnUrduFemale.setOnClickListener {
            selectedUrduVoice = "ur-IN-Standard-A"
            highlightBtn(binding.btnUrduFemale, true)
            highlightBtn(binding.btnUrduMale, false)
            binding.tvUrduVoiceLabel.text = "ur-IN-Standard-A (Female)"
        }
        binding.btnUrduMale.setOnClickListener {
            selectedUrduVoice = "ur-IN-Standard-B"
            highlightBtn(binding.btnUrduFemale, false)
            highlightBtn(binding.btnUrduMale, true)
            binding.tvUrduVoiceLabel.text = "ur-IN-Standard-B (Male)"
        }

        // V1 voice selectors
        androidVoiceButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectAndroidVoice(i) } }
        selectAndroidVoice(0)
        chirpVoiceButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectChirpVoice(i) } }
        selectChirpVoice(0)
        v2VoiceButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectV2Voice(i) } }
        selectV2Voice(0)

        // V2 TTS voice selectors
        v2TtsVoiceButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectV2TtsVoice(i) } }
        selectV2TtsVoice(0)

        // Record button
        binding.btnRecord.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) startRecording()
                    else {
                        checkPermission()
                        binding.tvStatus.text = "⚠️ Microphone permission needed"
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> { stopAndRecognize(); true }
                else -> false
            }
        }

        binding.btnSpeak.setOnClickListener {
            if (lastTranscript.isNotEmpty() && !isSpeaking) speakText(lastTranscript)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STT Version Selection
    // ─────────────────────────────────────────────────────────────────────────

    private fun selectSttVersion(version: SttVersion) {
        currentSttVersion = version
        highlightBtn(binding.btnSttV1, version == SttVersion.V1)
        highlightBtn(binding.btnSttV2, version == SttVersion.V2)
        when (version) {
            SttVersion.V1 -> {
                binding.layoutV2Models.visibility = View.GONE
                binding.tvSttLabel.text = "speech.googleapis.com/v1 · API key · Classic models"
                binding.layoutV1Tts.visibility    = View.VISIBLE
                binding.layoutV2Tts.visibility    = View.GONE
                binding.cardV2TtsVoice.visibility = View.GONE
                selectEngine(currentEngine)
            }
            SttVersion.V2 -> {
                binding.layoutV2Models.visibility = View.VISIBLE
                binding.tvSttLabel.text = "speech.googleapis.com/v2 · OAuth2 · Explicit PCM decoding"
                binding.layoutV1Tts.visibility      = View.GONE
                binding.cardAndroidVoice.visibility = View.GONE
                binding.cardGeminiVoice.visibility  = View.GONE
                binding.cardVertexVoice.visibility  = View.GONE
                binding.layoutUrduVoice.visibility  = View.GONE
                binding.layoutV2Tts.visibility      = View.VISIBLE
                binding.cardV2TtsVoice.visibility   = View.VISIBLE
            }
        }
    }

    private fun selectV2Model(index: Int) {
        selectedV2Model = v2Models[index]
        v2ModelButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        if (currentSttVersion == SttVersion.V2)
            binding.tvSttLabel.text = "speech.googleapis.com/v2 · Model: ${v2ModelLabels[index]}"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recording
    // ─────────────────────────────────────────────────────────────────────────

    private fun startRecording() {
        stopPlayback()
        binding.btnSpeak.isEnabled    = false
        binding.tvSttBadge.visibility = View.GONE
        binding.tvStatus.text         = "🔴 Recording... (release to stop)"
        binding.tvResult.text         = ""
        lastTranscript                = ""
        didStartRecording             = true
        audioRecorder.startRecording()
    }

    private fun stopAndRecognize() {
        if (!didStartRecording) return
        didStartRecording     = false
        binding.tvStatus.text = "⏳ Processing audio..."
        lifecycleScope.launch {
            val base64Audio = withContext(Dispatchers.IO) { audioRecorder.stopRecording() }
            if (base64Audio.isEmpty()) {
                binding.tvStatus.text      = "⚠️ No audio captured. Hold the button longer!"
                binding.btnSpeak.isEnabled = false
                return@launch
            }
            binding.tvStatus.text = when (currentSttVersion) {
                SttVersion.V1 -> "⏳ Recognizing (STT V1)..."
                SttVersion.V2 -> "⏳ Recognizing (STT V2 · $selectedV2Model)..."
            }

            val transcript = when (currentSttVersion) {
                SttVersion.V1 -> recognizeSpeechV1(base64Audio)
                SttVersion.V2 -> recognizeSpeechV2(base64Audio)
            }

            lastTranscript        = transcript
            binding.tvResult.text = transcript

            if (transcript.startsWith("❌") || transcript.startsWith("🔇")) {
                binding.tvStatus.text      = "⚠️ Try again."
                binding.btnSpeak.isEnabled = false
                binding.tvSttBadge.visibility = View.GONE
            } else {
                binding.tvStatus.text      = "✅ Transcript ready!"
                binding.btnSpeak.isEnabled = true
                binding.tvSttBadge.text = when (currentSttVersion) {
                    SttVersion.V1 -> "✓ STT V1 · v1/speech:recognize"
                    SttVersion.V2 -> "✓ STT V2 · $selectedV2Model model"
                }
                binding.tvSttBadge.visibility = View.VISIBLE
                speakText(transcript)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STT V1
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun recognizeSpeechV1(base64Audio: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.recognize(
                    API_KEY,
                    SpeechRequest(
                        RecognitionConfig("LINEAR16", 16000, selectedLangCode),
                        RecognitionAudio(base64Audio)
                    )
                )
                if (response.isSuccessful) {
                    response.body()?.results?.takeIf { it.isNotEmpty() }
                        ?.get(0)?.alternatives?.get(0)?.transcript
                        ?: "🔇 No speech detected."
                } else "❌ STT V1 Error ${response.code()}"
            } catch (e: Exception) { "❌ ${e.message}" }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STT V2
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun recognizeSpeechV2(base64Audio: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = SpeechV2Request(
                    recognizer = "projects/${SpeechV2RetrofitClient.PROJECT_ID}/locations/global/recognizers/_",
                    config = SpeechV2Config(
                        explicitDecodingConfig = ExplicitDecodingConfig(
                            encoding          = "LINEAR16",
                            sampleRateHertz   = 16000,
                            au dioChannelCount = 1
                        ),
                        languageCodes = listOf(selectedLangCode),
                        model         = selectedV2Model
                    ),
                    content = base64Audio
                )
                val response = SpeechV2RetrofitClient.recognize(request, selectedV2Model)
                if (response.isSuccessful) {
                    val results = response.body()?.results
                    if (!results.isNullOrEmpty()) {
                        results[0].languageCode?.let { Log.d("STT_V2", "Detected lang: $it") }
                        results[0].alternatives?.get(0)?.transcript ?: "🔇 No transcript in result."
                    } else "🔇 No speech detected (V2)."
                } else {
                    val err = response.errorBody()?.string() ?: "unknown"
                    Log.e("STT_V2", "Error body: $err")
                    "❌ STT V2 Error ${response.code()}: $err"
                }
            } catch (e: java.net.SocketTimeoutException) {
                "❌ Timed out (Chirp can be slow — try again)"
            } catch (e: Exception) {
                "❌ V2: ${e.message}"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TTS Dispatch
    // ─────────────────────────────────────────────────────────────────────────

    private fun speakText(text: String) {
        if (currentSttVersion == SttVersion.V2) {
            speakWithTtsV2(text)
        } else {
            when (currentEngine) {
                TtsEngine.ANDROID  -> speakWithAndroidTts(text)
                TtsEngine.CHIRP_V1 -> {
                    if (selectedLangCode == "ur-IN") {
                        binding.tvStatus.text = "ℹ️ Chirp3-HD is English only — using Cloud TTS v1beta1 for Urdu"
                        speakUrduWithCloudTtsV2(text)
                    } else speakWithChirpTts(text)
                }
                TtsEngine.CLOUD_V1BETA1 -> {
                    if (selectedLangCode == "ur-IN") speakUrduWithCloudTtsV2(text)
                    else speakWithCloudV1Beta1(text)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // V2 TTS
    // ─────────────────────────────────────────────────────────────────────────

    private fun speakWithTtsV2(text: String) {
        val voice = ttsV2Voices[selectedV2TtsVoiceIndex]
        val req = TtsV2NewSynthesizeRequest(
            TtsInput(text),
            TtsV2NewVoice("en-US", voice.voiceId),
            TtsV2NewAudioConfig("LINEAR16", 24000)
        )
        isSpeaking = true
        binding.btnSpeak.text      = "⏳ Generating..."
        binding.btnSpeak.isEnabled = false
        binding.tvStatus.text      = "🔊 TTS v2 · ${voice.displayName}..."

        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { TtsV2NewRetrofitClient.synthesize(API_KEY, req) }
                if (r.isSuccessful && !r.body()?.audioContent.isNullOrEmpty()) {
                    binding.tvStatus.text = "🔊 Playing ${voice.displayName} (TTS v2)..."
                    playPcmAudio(Base64.decode(r.body()!!.audioContent!!, Base64.DEFAULT))
                } else {
                    val err = r.errorBody()?.string() ?: ""
                    Log.e("TTS_V2", "Error ${r.code()}: $err")
                    binding.tvStatus.text = "❌ TTS v2 error ${r.code()}"
                    resetSpeakButton()
                }
            } catch (e: Exception) {
                binding.tvStatus.text = "❌ ${e.message}"
                Log.e("TTS_V2", "Exception: ${e.message}")
                resetSpeakButton()
            }
        }
    }

    private fun selectV2TtsVoice(index: Int) {
        selectedV2TtsVoiceIndex = index
        v2TtsVoiceButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        binding.tvV2TtsVoiceLabel.text =
            "Voice: ${ttsV2Voices[index].displayName} (${ttsV2Voices[index].gender}) · TTS v2"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // V1 TTS Engine Selection
    // ─────────────────────────────────────────────────────────────────────────

    private fun selectEngine(engine: TtsEngine) {
        currentEngine = engine
        highlightBtn(binding.btnEngineAndroid, engine == TtsEngine.ANDROID)
        highlightBtn(binding.btnEngineChirp,   engine == TtsEngine.CHIRP_V1)
        highlightBtn(binding.btnEngineVertex,  engine == TtsEngine.CLOUD_V1BETA1)
        when (engine) {
            TtsEngine.ANDROID -> {
                binding.tvEngineLabel.text          = "📱 Android Built-in"
                binding.tvEngineDesc.text           = "Device voice · works offline"
                binding.cardAndroidVoice.visibility = View.VISIBLE
                binding.cardGeminiVoice.visibility  = View.GONE
                binding.cardVertexVoice.visibility  = View.GONE
                binding.btnSpeak.text               = "🔊 Play Transcript"
            }
            TtsEngine.CHIRP_V1 -> {
                binding.tvEngineLabel.text          = "✨ Chirp3-HD (TTS v1)"
                binding.tvEngineDesc.text           = "texttospeech.googleapis.com/v1"
                binding.cardAndroidVoice.visibility = View.GONE
                binding.cardGeminiVoice.visibility  = View.VISIBLE
                binding.cardVertexVoice.visibility  = View.GONE
                binding.btnSpeak.text               = "🔊 Play with Chirp3-HD"
            }
            TtsEngine.CLOUD_V1BETA1 -> {
                binding.tvEngineLabel.text          = "🚀 Cloud TTS v1beta1"
                binding.tvEngineDesc.text           = "texttospeech.googleapis.com/v1beta1 · Studio & Neural2 & Urdu"
                binding.cardAndroidVoice.visibility = View.GONE
                binding.cardGeminiVoice.visibility  = View.GONE
                binding.cardVertexVoice.visibility  = View.VISIBLE
                binding.btnSpeak.text               = "🔊 Play with Cloud TTS"
            }
        }
        updateUrduVoiceVisibility()
    }

    private fun updateUrduVoiceVisibility() {
        binding.layoutUrduVoice.visibility =
            if (selectedLangCode == "ur-IN" && currentEngine != TtsEngine.ANDROID) View.VISIBLE
            else View.GONE
    }

    private fun selectLanguage(lang: String) {
        if (lang == "en") {
            selectedLangCode          = "en-US"
            selectedTtsLocale         = Locale.US
            binding.tvLangLabel.text  = "Language: English (en-US)"
            highlightBtn(binding.btnLangEn, true)
            highlightBtn(binding.btnLangUr, false)
        } else {
            selectedLangCode          = "ur-IN"
            selectedTtsLocale         = Locale("ur", "PK")
            binding.tvLangLabel.text  = "زبان: اردو (ur-PK)"
            highlightBtn(binding.btnLangUr, true)
            highlightBtn(binding.btnLangEn, false)
        }
        if (androidTtsReady) androidTts?.setLanguage(selectedTtsLocale)
        updateUrduVoiceVisibility()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Android TTS (V1 only)
    // ─────────────────────────────────────────────────────────────────────────

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = androidTts?.setLanguage(selectedTtsLocale)
            androidTtsReady = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
            if (androidTtsReady) {
                androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = runOnUiThread {
                        isSpeaking = true
                        binding.btnSpeak.text      = "⏸ Speaking..."
                        binding.btnSpeak.isEnabled = false
                    }
                    override fun onDone(id: String?) = runOnUiThread {
                        isSpeaking = false
                        binding.btnSpeak.text      = "🔊 Play Again"
                        binding.btnSpeak.isEnabled = true
                        binding.tvStatus.text      = "✅ Done!"
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) = runOnUiThread {
                        isSpeaking = false
                        binding.btnSpeak.isEnabled = true
                        binding.btnSpeak.text      = "🔊 Play Again"
                    }
                })
            }
        }
    }

    private fun speakWithAndroidTts(text: String) {
        if (!androidTtsReady) { binding.tvStatus.text = "⚠️ Android TTS not ready"; return }
        val voice = androidVoices[selectedAndroidVoiceIndex]
        androidTts?.setLanguage(selectedTtsLocale)
        androidTts?.setPitch(voice.pitch)
        androidTts?.setSpeechRate(voice.speed)
        androidTts?.stop()
        androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UID")
        binding.tvStatus.text = "🔊 Playing ${voice.label}..."
    }

    private fun selectAndroidVoice(index: Int) {
        selectedAndroidVoiceIndex = index
        androidVoiceButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        binding.tvAndroidVoiceLabel.text = "Voice: ${androidVoices[index].label}"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chirp3-HD TTS v1 (V1 only)
    // ─────────────────────────────────────────────────────────────────────────

    private fun speakWithChirpTts(text: String) {
        val voice = chirpVoices[selectedChirpVoiceIndex]
        val req = TtsSynthesizeRequest(
            TtsInput(text),
            TtsVoice("en-US", voice.voiceId),
            TtsAudioConfig("LINEAR16", 24000)
        )
        isSpeaking = true
        binding.btnSpeak.text      = "⏳ Generating..."
        binding.btnSpeak.isEnabled = false
        binding.tvStatus.text      = "✨ ${voice.displayName}..."

        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { TtsRetrofitClient.synthesize(API_KEY, req) }
                if (r.isSuccessful && !r.body()?.audioContent.isNullOrEmpty()) {
                    binding.tvStatus.text = "🔊 Playing ${voice.displayName}..."
                    playPcmAudio(Base64.decode(r.body()!!.audioContent!!, Base64.DEFAULT))
                } else {
                    binding.tvStatus.text = "❌ Chirp3-HD error ${r.code()}"
                    resetSpeakButton()
                }
            } catch (e: Exception) {
                binding.tvStatus.text = "❌ ${e.message}"
                resetSpeakButton()
            }
        }
    }

    private fun selectChirpVoice(index: Int) {
        selectedChirpVoiceIndex = index
        chirpVoiceButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        binding.tvGeminiVoiceLabel.text =
            "Voice: ${chirpVoices[index].displayName} (${chirpVoices[index].gender})"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Urdu TTS v1beta1 (V1 only)
    // ─────────────────────────────────────────────────────────────────────────

    private fun speakUrduWithCloudTtsV2(text: String) {
        val req = TtsV2SynthesizeRequest(
            TtsInput(text),
            TtsV2Voice("ur-IN", selectedUrduVoice),
            TtsV2AudioConfig("LINEAR16", 24000)
        )
        isSpeaking = true
        binding.btnSpeak.text      = "⏳ Generating..."
        binding.btnSpeak.isEnabled = false
        binding.tvStatus.text      = "🌐 Urdu v1beta1 ($selectedUrduVoice)..."

        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { TtsRetrofitClient.synthesizeV2(API_KEY, req) }
                if (r.isSuccessful && !r.body()?.audioContent.isNullOrEmpty()) {
                    binding.tvStatus.text = "🔊 Playing Urdu..."
                    playPcmAudio(Base64.decode(r.body()!!.audioContent!!, Base64.DEFAULT))
                } else {
                    binding.tvStatus.text = "❌ Urdu TTS error ${r.code()}"
                    Log.e("URDU_TTS", "Error: ${r.errorBody()?.string()}")
                    resetSpeakButton()
                }
            } catch (e: Exception) {
                binding.tvStatus.text = "❌ ${e.message}"
                Log.e("URDU_TTS", "Exception: ${e.message}")
                resetSpeakButton()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cloud TTS v1beta1 English (V1 only)
    // ─────────────────────────────────────────────────────────────────────────

    private fun speakWithCloudV1Beta1(text: String) {
        val voice = cloudV2Voices[selectedV2VoiceIndex]
        val req = TtsV2SynthesizeRequest(
            TtsInput(text),
            TtsV2Voice("en-US", voice.voiceId),
            TtsV2AudioConfig("LINEAR16", 24000)
        )
        isSpeaking = true
        binding.btnSpeak.text      = "⏳ Generating..."
        binding.btnSpeak.isEnabled = false
        binding.tvStatus.text      = "🚀 ${voice.displayName} (v1beta1)..."

        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { TtsRetrofitClient.synthesizeV2(API_KEY, req) }
                if (r.isSuccessful && !r.body()?.audioContent.isNullOrEmpty()) {
                    binding.tvStatus.text = "🔊 Playing ${voice.displayName}..."
                    playPcmAudio(Base64.decode(r.body()!!.audioContent!!, Base64.DEFAULT))
                } else {
                    binding.tvStatus.text = "❌ v1beta1 error ${r.code()}"
                    resetSpeakButton()
                }
            } catch (e: Exception) {
                binding.tvStatus.text = "❌ ${e.message}"
                resetSpeakButton()
            }
        }
    }

    private fun selectV2Voice(index: Int) {
        selectedV2VoiceIndex = index
        v2VoiceButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        binding.tvVertexVoiceLabel.text =
            "Voice: ${cloudV2Voices[index].displayName} (${cloudV2Voices[index].gender})"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audio Playback
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun playPcmAudio(pcm: ByteArray, sampleRate: Int = 24000) {
        withContext(Dispatchers.IO) {
            val ch  = AudioFormat.CHANNEL_OUT_MONO
            val fmt = AudioFormat.ENCODING_PCM_16BIT
            val buf = AudioTrack.getMinBufferSize(sampleRate, ch, fmt).coerceAtLeast(pcm.size)
            audioTrack?.release()
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(fmt)
                        .setSampleRate(sampleRate)
                        .setChannelMask(ch)
                        .build()
                )
                .setBufferSizeInBytes(buf)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            audioTrack?.write(pcm, 0, pcm.size)
            audioTrack?.play()
            Thread.sleep((pcm.size.toLong() * 1000L) / (sampleRate * 2) + 300)
        }
        withContext(Dispatchers.Main) {
            binding.tvStatus.text = "✅ Done! Hold button to record again."
            resetSpeakButton()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun resetSpeakButton() {
        isSpeaking                 = false
        binding.btnSpeak.text      = "🔊 Play Again"
        binding.btnSpeak.isEnabled = lastTranscript.isNotEmpty()
    }

    private fun stopPlayback() {
        androidTts?.stop()
        audioTrack?.stop(); audioTrack?.release(); audioTrack = null
        isSpeaking = false
    }

    private fun highlightBtn(btn: android.widget.Button, active: Boolean) {
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (active) 0xFF1A73E8.toInt() else 0xFFE0E0E0.toInt()
        )
        btn.setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xFF757575.toInt())
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_PERMISSION_CODE
            )
    }

    override fun onDestroy() {
        androidTts?.stop(); androidTts?.shutdown()
        stopPlayback()
        super.onDestroy()
    }
}