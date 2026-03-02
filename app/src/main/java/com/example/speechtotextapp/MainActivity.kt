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
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.speechtotextapp.data.RetrofitClient
import com.example.speechtotextapp.auth.ServiceAccountAuth
import com.example.speechtotextapp.data.SpeechV2RetrofitClient
import com.example.speechtotextapp.data.TtsRetrofitClient
import com.example.speechtotextapp.helper.AudioRecorder
import com.example.speechtotextapp.model.AutoDecodingConfig
import com.example.speechtotextapp.model.RecognitionAudio
import com.example.speechtotextapp.model.RecognitionConfig
import com.example.speechtotextapp.model.SpeechRequest
import com.example.speechtotextapp.model.SpeechV2Config
import com.example.speechtotextapp.model.SpeechV2Request
import com.example.speechtotextapp.model.TtsAudioConfig
import com.example.speechtotextapp.model.TtsInput
import com.example.speechtotextapp.model.TtsSynthesizeRequest
import com.example.speechtotextapp.model.TtsV2AudioConfig
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

    private val API_KEY = getString(R.string.key)
    private val RECORD_PERMISSION_CODE = 101

    private var currentSttVersion = SttVersion.V1
    private var selectedV2Model   = "chirp"

    private lateinit var btnSttV1: Button
    private lateinit var btnSttV2: Button
    private lateinit var layoutV2Models: View
    private lateinit var btnModelChirp: Button
    private lateinit var btnModelLong: Button
    private lateinit var btnModelShort: Button
    private lateinit var btnModelLatest: Button
    private lateinit var tvSttLabel: TextView
    private lateinit var tvSttBadge: TextView
    private val v2ModelButtons get() = listOf(btnModelChirp, btnModelLong, btnModelShort, btnModelLatest)
    private val v2Models      = listOf("chirp", "long", "short", "latest_long")
    private val v2ModelLabels = listOf("Chirp", "Long", "Short", "Latest")

    private lateinit var btnRecord: Button
    private lateinit var btnSpeak: Button
    private lateinit var btnLangEn: Button
    private lateinit var btnLangUr: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvLangLabel: TextView
    private lateinit var tvEngineLabel: TextView
    private lateinit var tvEngineDesc: TextView
    private lateinit var btnEngineAndroid: Button
    private lateinit var btnEngineChirp: Button
    private lateinit var btnEngineVertex: Button
    private lateinit var cardAndroidVoice: CardView
    private lateinit var cardGeminiVoice: CardView
    private lateinit var cardVertexVoice: CardView
    private lateinit var layoutUrduVoice: View
    private lateinit var btnUrduFemale: Button
    private lateinit var btnUrduMale: Button
    private lateinit var tvUrduVoiceLabel: TextView
    private var selectedUrduVoice = "ur-PK-Standard-A"

    private lateinit var btnVoice1: Button; private lateinit var btnVoice2: Button
    private lateinit var btnVoice3: Button; private lateinit var btnVoice4: Button
    private lateinit var btnVoice5: Button; private lateinit var btnVoice6: Button
    private val androidVoiceButtons get() = listOf(btnVoice1, btnVoice2, btnVoice3, btnVoice4, btnVoice5, btnVoice6)
    private lateinit var tvAndroidVoiceLabel: TextView
    data class AndroidVoiceProfile(val label: String, val pitch: Float, val speed: Float)
    private val androidVoices = listOf(
        AndroidVoiceProfile("Female 1", 1.2f, 1.0f), AndroidVoiceProfile("Male 1", 0.8f, 0.95f),
        AndroidVoiceProfile("Female 2", 1.5f, 1.1f), AndroidVoiceProfile("Male 2", 0.6f, 0.85f),
        AndroidVoiceProfile("Child", 1.8f, 1.2f),    AndroidVoiceProfile("Robot", 0.5f, 0.7f)
    )
    private var selectedAndroidVoiceIndex = 0

    private lateinit var btnVAchernar: Button; private lateinit var btnVAoede: Button
    private lateinit var btnVKore: Button;     private lateinit var btnVZephyr: Button
    private lateinit var btnVCharon: Button;   private lateinit var btnVPuck: Button
    private lateinit var btnVFenrir: Button;   private lateinit var btnVOrus: Button
    private val chirpVoiceButtons get() = listOf(btnVAchernar, btnVAoede, btnVKore, btnVZephyr, btnVCharon, btnVPuck, btnVFenrir, btnVOrus)
    private lateinit var tvGeminiVoiceLabel: TextView
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
    private var selectedChirpVoiceIndex = 0

    private lateinit var btnVertexVoice1: Button; private lateinit var btnVertexVoice2: Button
    private lateinit var btnVertexVoice3: Button; private lateinit var btnVertexVoice4: Button
    private lateinit var btnVertexVoice5: Button; private lateinit var btnVertexVoice6: Button
    private val v2VoiceButtons get() = listOf(btnVertexVoice1, btnVertexVoice2, btnVertexVoice3, btnVertexVoice4, btnVertexVoice5, btnVertexVoice6)
    private lateinit var tvVertexVoiceLabel: TextView
    data class CloudV2Voice(val displayName: String, val gender: String, val voiceId: String)
    private val cloudV2Voices = listOf(
        CloudV2Voice("Studio-O", "Female", "en-US-Studio-O"),
        CloudV2Voice("Studio-Q", "Male",   "en-US-Studio-Q"),
        CloudV2Voice("Neural2-C","Female", "en-US-Neural2-C"),
        CloudV2Voice("Neural2-D","Male",   "en-US-Neural2-D"),
        CloudV2Voice("Neural2-F","Female", "en-US-Neural2-F"),
        CloudV2Voice("Neural2-J","Male",   "en-US-Neural2-J")
    )
    private var selectedV2VoiceIndex = 0

    private var audioTrack: AudioTrack? = null
    private var isSpeaking = false
    private var androidTts: TextToSpeech? = null
    private var androidTtsReady = false

    private val audioRecorder     = AudioRecorder()
    private var lastTranscript    = ""
    private var didStartRecording = false
    private var selectedLangCode  = "en-US"
    private var selectedTtsLocale: Locale = Locale.US
    private var currentEngine     = TtsEngine.ANDROID

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSttV1       = findViewById(R.id.btnSttV1)
        btnSttV2       = findViewById(R.id.btnSttV2)
        layoutV2Models = findViewById(R.id.layoutV2Models)
        btnModelChirp  = findViewById(R.id.btnModelChirp)
        btnModelLong   = findViewById(R.id.btnModelLong)
        btnModelShort  = findViewById(R.id.btnModelShort)
        btnModelLatest = findViewById(R.id.btnModelLatest)
        tvSttLabel     = findViewById(R.id.tvSttLabel)
        tvSttBadge     = findViewById(R.id.tvSttBadge)
        btnRecord      = findViewById(R.id.btnRecord)
        btnSpeak       = findViewById(R.id.btnSpeak)
        btnLangEn      = findViewById(R.id.btnLangEn)
        btnLangUr      = findViewById(R.id.btnLangUr)
        tvStatus       = findViewById(R.id.tvStatus)
        tvResult       = findViewById(R.id.tvResult)
        tvLangLabel    = findViewById(R.id.tvLangLabel)
        tvEngineLabel  = findViewById(R.id.tvEngineLabel)
        tvEngineDesc   = findViewById(R.id.tvEngineDesc)
        btnEngineAndroid = findViewById(R.id.btnEngineAndroid)
        btnEngineChirp   = findViewById(R.id.btnEngineChirp)
        btnEngineVertex  = findViewById(R.id.btnEngineVertex)
        cardAndroidVoice = findViewById(R.id.cardAndroidVoice)
        cardGeminiVoice  = findViewById(R.id.cardGeminiVoice)
        cardVertexVoice  = findViewById(R.id.cardVertexVoice)
        layoutUrduVoice  = findViewById(R.id.layoutUrduVoice)
        btnUrduFemale    = findViewById(R.id.btnUrduFemale)
        btnUrduMale      = findViewById(R.id.btnUrduMale)
        tvUrduVoiceLabel = findViewById(R.id.tvUrduVoiceLabel)
        btnVoice1 = findViewById(R.id.btnVoice1); btnVoice2 = findViewById(R.id.btnVoice2)
        btnVoice3 = findViewById(R.id.btnVoice3); btnVoice4 = findViewById(R.id.btnVoice4)
        btnVoice5 = findViewById(R.id.btnVoice5); btnVoice6 = findViewById(R.id.btnVoice6)
        tvAndroidVoiceLabel = findViewById(R.id.tvAndroidVoiceLabel)
        btnVAchernar = findViewById(R.id.btnVAchernar); btnVAoede  = findViewById(R.id.btnVAoede)
        btnVKore     = findViewById(R.id.btnVKore);     btnVZephyr = findViewById(R.id.btnVZephyr)
        btnVCharon   = findViewById(R.id.btnVCharon);   btnVPuck   = findViewById(R.id.btnVPuck)
        btnVFenrir   = findViewById(R.id.btnVFenrir);   btnVOrus   = findViewById(R.id.btnVOrus)
        tvGeminiVoiceLabel = findViewById(R.id.tvGeminiVoiceLabel)
        btnVertexVoice1 = findViewById(R.id.btnVertexVoice1); btnVertexVoice2 = findViewById(R.id.btnVertexVoice2)
        btnVertexVoice3 = findViewById(R.id.btnVertexVoice3); btnVertexVoice4 = findViewById(R.id.btnVertexVoice4)
        btnVertexVoice5 = findViewById(R.id.btnVertexVoice5); btnVertexVoice6 = findViewById(R.id.btnVertexVoice6)
        tvVertexVoiceLabel = findViewById(R.id.tvVertexVoiceLabel)

        btnSpeak.isEnabled = false
        androidTts = TextToSpeech(this, this)
        checkPermission()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }

        btnSttV1.setOnClickListener { selectSttVersion(SttVersion.V1) }
        btnSttV2.setOnClickListener { selectSttVersion(SttVersion.V2) }
        v2ModelButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectV2Model(i) } }
        selectSttVersion(SttVersion.V1)
        selectV2Model(0)

        btnLangEn.setOnClickListener { selectLanguage("en") }
        btnLangUr.setOnClickListener { selectLanguage("ur") }
        selectLanguage("en")

        btnEngineAndroid.setOnClickListener { selectEngine(TtsEngine.ANDROID) }
        btnEngineChirp.setOnClickListener   { selectEngine(TtsEngine.CHIRP_V1) }
        btnEngineVertex.setOnClickListener  { selectEngine(TtsEngine.CLOUD_V1BETA1) }
        selectEngine(TtsEngine.ANDROID)

        btnUrduFemale.setOnClickListener {
            selectedUrduVoice = "ur-PK-Standard-A"
            highlightBtn(btnUrduFemale, true); highlightBtn(btnUrduMale, false)
            tvUrduVoiceLabel.text = "ur-PK-Standard-A (Female) via Cloud TTS"
        }
        btnUrduMale.setOnClickListener {
            selectedUrduVoice = "ur-PK-Standard-B"
            highlightBtn(btnUrduFemale, false); highlightBtn(btnUrduMale, true)
            tvUrduVoiceLabel.text = "ur-PK-Standard-B (Male) via Cloud TTS"
        }

        androidVoiceButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectAndroidVoice(i) } }
        selectAndroidVoice(0)
        chirpVoiceButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectChirpVoice(i) } }
        selectChirpVoice(0)
        v2VoiceButtons.forEachIndexed { i, btn -> btn.setOnClickListener { selectV2Voice(i) } }
        selectV2Voice(0)

        btnRecord.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) startRecording()
                    else { checkPermission(); tvStatus.text = "⚠️ Microphone permission needed" }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> { stopAndRecognize(); true }
                else -> false
            }
        }
        btnSpeak.setOnClickListener {
            if (lastTranscript.isNotEmpty() && !isSpeaking) speakText(lastTranscript)
        }
    }

    private fun selectSttVersion(version: SttVersion) {
        currentSttVersion = version
        highlightBtn(btnSttV1, version == SttVersion.V1)
        highlightBtn(btnSttV2, version == SttVersion.V2)
        when (version) {
            SttVersion.V1 -> {
                layoutV2Models.visibility = View.GONE
                tvSttLabel.text = "speech.googleapis.com/v1 · API key · Classic models"
            }
            SttVersion.V2 -> {
                layoutV2Models.visibility = View.VISIBLE
                tvSttLabel.text = "speech.googleapis.com/v2 · Chirp · Auto audio detect"
            }
        }
    }

    private fun selectV2Model(index: Int) {
        selectedV2Model = v2Models[index]
        v2ModelButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        if (currentSttVersion == SttVersion.V2)
            tvSttLabel.text = "speech.googleapis.com/v2 · Model: ${v2ModelLabels[index]}"
    }

    private fun startRecording() {
        stopPlayback()
        btnSpeak.isEnabled = false
        tvSttBadge.visibility = View.GONE
        tvStatus.text = "🔴 Recording... (release to stop)"
        tvResult.text = ""; lastTranscript = ""; didStartRecording = true
        audioRecorder.startRecording()
    }

    private fun stopAndRecognize() {
        if (!didStartRecording) return
        didStartRecording = false
        tvStatus.text = "⏳ Processing audio..."
        lifecycleScope.launch {
            val base64Audio = withContext(Dispatchers.IO) { audioRecorder.stopRecording() }
            if (base64Audio.isEmpty()) {
                tvStatus.text = "⚠️ No audio captured. Hold the button longer!"
                btnSpeak.isEnabled = false; return@launch
            }
            tvStatus.text = when (currentSttVersion) {
                SttVersion.V1 -> "⏳ Recognizing (STT V1)..."
                SttVersion.V2 -> "⏳ Recognizing (STT V2 · $selectedV2Model)..."
            }
            val transcript = when (currentSttVersion) {
                SttVersion.V1 -> recognizeSpeechV1(base64Audio)
                SttVersion.V2 -> recognizeSpeechV2(base64Audio)
            }
            lastTranscript = transcript; tvResult.text = transcript
            if (transcript.startsWith("❌") || transcript.startsWith("🔇")) {
                tvStatus.text = "⚠️ Try again."; btnSpeak.isEnabled = false
                tvSttBadge.visibility = View.GONE
            } else {
                tvStatus.text = "✅ Transcript ready!"; btnSpeak.isEnabled = true
                tvSttBadge.text = when (currentSttVersion) {
                    SttVersion.V1 -> "✓ STT V1 · v1/speech:recognize"
                    SttVersion.V2 -> "✓ STT V2 · $selectedV2Model model"
                }
                tvSttBadge.visibility = View.VISIBLE
                speakText(transcript)
            }
        }
    }

    private suspend fun recognizeSpeechV1(base64Audio: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.recognize(API_KEY,
                    SpeechRequest(RecognitionConfig("LINEAR16", 16000, selectedLangCode),
                        RecognitionAudio(base64Audio)))
                if (response.isSuccessful) {
                    response.body()?.results?.takeIf { it.isNotEmpty() }
                        ?.get(0)?.alternatives?.get(0)?.transcript
                        ?: "🔇 No speech detected."
                } else "❌ STT V1 Error ${response.code()}"
            } catch (e: Exception) { "❌ ${e.message}" }
        }
    }

    private suspend fun recognizeSpeechV2(base64Audio: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val langCode = if (selectedLangCode == "ur-PK") "ur-PK" else "en-US"
                // Chirp model only supports en-US — fall back to "long" for Urdu
                val model = if (selectedLangCode == "ur-PK" && selectedV2Model == "chirp") "long"
                else selectedV2Model
                val request = SpeechV2Request(
                    recognizer = "projects/${SpeechV2RetrofitClient.PROJECT_ID}/locations/global/recognizers/_",
                    config     = SpeechV2Config(
                        autoDecodingConfig = AutoDecodingConfig(),
                        languageCodes      = listOf(langCode),
                        model              = model
                    ),
                    content = base64Audio
                )
                val response = SpeechV2RetrofitClient.recognize(request, model)
                if (response.isSuccessful) {
                    val results = response.body()?.results
                    if (!results.isNullOrEmpty()) {
                        results[0].languageCode?.let { Log.d("STT_V2", "Detected lang: $it") }
                        results[0].alternatives?.get(0)?.transcript ?: "No transcript"
                    } else "🔇 No speech detected (V2)."
                } else {
                    val err = response.errorBody()?.string() ?: "unknown"
                    Log.e("STT_V2", err)
                    "❌ STT V2 Error ${response.code()}: $err"
                }
            } catch (e: java.net.SocketTimeoutException) { "❌ Timed out (Chirp can be slow, try again)" }
            catch (e: Exception) { "❌ V2: ${e.message}" }
        }
    }

    private fun selectEngine(engine: TtsEngine) {
        currentEngine = engine
        highlightBtn(btnEngineAndroid, engine == TtsEngine.ANDROID)
        highlightBtn(btnEngineChirp,   engine == TtsEngine.CHIRP_V1)
        highlightBtn(btnEngineVertex,  engine == TtsEngine.CLOUD_V1BETA1)
        when (engine) {
            TtsEngine.ANDROID -> {
                tvEngineLabel.text = "📱 Android Built-in"; tvEngineDesc.text = "Device voice · works offline"
                cardAndroidVoice.visibility = View.VISIBLE; cardGeminiVoice.visibility = View.GONE; cardVertexVoice.visibility = View.GONE
                btnSpeak.text = "🔊 Play Transcript"
            }
            TtsEngine.CHIRP_V1 -> {
                tvEngineLabel.text = "✨ Chirp3-HD (TTS v1)"; tvEngineDesc.text = "texttospeech.googleapis.com/v1"
                cardAndroidVoice.visibility = View.GONE; cardGeminiVoice.visibility = View.VISIBLE; cardVertexVoice.visibility = View.GONE
                btnSpeak.text = "🔊 Play with Chirp3-HD"
            }
            TtsEngine.CLOUD_V1BETA1 -> {
                tvEngineLabel.text = "🚀 Cloud TTS v1beta1"; tvEngineDesc.text = "texttospeech.googleapis.com/v1beta1 · Studio & Neural2"
                cardAndroidVoice.visibility = View.GONE; cardGeminiVoice.visibility = View.GONE; cardVertexVoice.visibility = View.VISIBLE
                btnSpeak.text = "🔊 Play with Cloud TTS"
            }
        }
        updateUrduVoiceVisibility()
    }

    private fun updateUrduVoiceVisibility() {
        layoutUrduVoice.visibility = if (selectedLangCode == "ur-PK" && currentEngine != TtsEngine.ANDROID) View.VISIBLE else View.GONE
    }

    private fun selectLanguage(lang: String) {
        if (lang == "en") {
            selectedLangCode = "en-US"; selectedTtsLocale = Locale.US; tvLangLabel.text = "Language: English (en-US)"
            highlightBtn(btnLangEn, true); highlightBtn(btnLangUr, false)
        } else {
            selectedLangCode = "ur-PK"; selectedTtsLocale = Locale("ur", "PK"); tvLangLabel.text = "زبان: اردو (ur-PK)"
            highlightBtn(btnLangUr, true); highlightBtn(btnLangEn, false)
        }
        if (androidTtsReady) androidTts?.setLanguage(selectedTtsLocale)
        updateUrduVoiceVisibility()
    }

    private fun speakText(text: String) {
        when (currentEngine) {
            TtsEngine.ANDROID       -> speakWithAndroidTts(text)
            TtsEngine.CHIRP_V1      -> if (selectedLangCode == "ur-PK") {
                tvStatus.text = "ℹ️ Chirp3-HD is English only — switching to Cloud TTS for Urdu"
                speakUrduWithCloudTts(text)
            } else speakWithChirpTts(text)
            TtsEngine.CLOUD_V1BETA1 -> if (selectedLangCode == "ur-PK") speakUrduWithCloudTts(text) else speakWithCloudV1Beta1(text)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = androidTts?.setLanguage(selectedTtsLocale)
            androidTtsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            if (androidTtsReady) {
                androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = runOnUiThread { isSpeaking = true; btnSpeak.text = "⏸ Speaking..."; btnSpeak.isEnabled = false }
                    override fun onDone(id: String?) = runOnUiThread { isSpeaking = false; btnSpeak.text = "🔊 Play Again"; btnSpeak.isEnabled = true; tvStatus.text = "✅ Done!" }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) = runOnUiThread { isSpeaking = false; btnSpeak.isEnabled = true; btnSpeak.text = "🔊 Play Again" }
                })
            }
        }
    }

    private fun speakWithAndroidTts(text: String) {
        if (!androidTtsReady) { tvStatus.text = "⚠️ Android TTS not ready"; return }
        val voice = androidVoices[selectedAndroidVoiceIndex]
        androidTts?.setLanguage(selectedTtsLocale); androidTts?.setPitch(voice.pitch); androidTts?.setSpeechRate(voice.speed)
        androidTts?.stop(); androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UID")
        tvStatus.text = "🔊 Playing ${voice.label}..."
    }

    private fun selectAndroidVoice(index: Int) {
        selectedAndroidVoiceIndex = index
        androidVoiceButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        tvAndroidVoiceLabel.text = "Voice: ${androidVoices[index].label}"
    }

    private fun speakWithChirpTts(text: String) {
        val voice = chirpVoices[selectedChirpVoiceIndex]
        val req = TtsSynthesizeRequest(TtsInput(text), TtsVoice("en-US", voice.voiceId), TtsAudioConfig("LINEAR16", 24000))
        isSpeaking = true; btnSpeak.text = "⏳ Generating..."; btnSpeak.isEnabled = false; tvStatus.text = "✨ ${voice.displayName}..."
        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { TtsRetrofitClient.synthesize(API_KEY, req) }
                if (r.isSuccessful && !r.body()?.audioContent.isNullOrEmpty()) { tvStatus.text = "🔊 Playing ${voice.displayName}..."; playPcmAudio(Base64.decode(r.body()!!.audioContent!!, Base64.DEFAULT)) }
                else { tvStatus.text = "❌ Chirp3-HD error ${r.code()}"; resetSpeakButton() }
            } catch (e: Exception) { tvStatus.text = "❌ ${e.message}"; resetSpeakButton() }
        }
    }

    private fun speakUrduWithCloudTts(text: String) {
        val req = TtsSynthesizeRequest(TtsInput(text), TtsVoice("ur-PK", selectedUrduVoice), TtsAudioConfig("LINEAR16", 24000))
        isSpeaking = true; btnSpeak.text = "⏳ Generating..."; btnSpeak.isEnabled = false; tvStatus.text = "🌐 Urdu ($selectedUrduVoice)..."
        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { TtsRetrofitClient.synthesize(API_KEY, req) }
                if (r.isSuccessful && !r.body()?.audioContent.isNullOrEmpty()) { tvStatus.text = "🔊 Playing Urdu..."; playPcmAudio(Base64.decode(r.body()!!.audioContent!!, Base64.DEFAULT)) }
                else { tvStatus.text = "❌ Urdu TTS error ${r.code()}"; resetSpeakButton() }
            } catch (e: Exception) { tvStatus.text = "❌ ${e.message}"; resetSpeakButton() }
        }
    }

    private fun selectChirpVoice(index: Int) {
        selectedChirpVoiceIndex = index
        chirpVoiceButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        tvGeminiVoiceLabel.text = "Voice: ${chirpVoices[index].displayName} (${chirpVoices[index].gender})"
    }

    private fun speakWithCloudV1Beta1(text: String) {
        val voice = cloudV2Voices[selectedV2VoiceIndex]
        val req = TtsV2SynthesizeRequest(TtsInput(text), TtsV2Voice("en-US", voice.voiceId), TtsV2AudioConfig("LINEAR16", 24000))
        isSpeaking = true; btnSpeak.text = "⏳ Generating..."; btnSpeak.isEnabled = false; tvStatus.text = "🚀 ${voice.displayName} (v1beta1)..."
        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { TtsRetrofitClient.synthesizeV2(API_KEY, req) }
                if (r.isSuccessful && !r.body()?.audioContent.isNullOrEmpty()) { tvStatus.text = "🔊 Playing ${voice.displayName}..."; playPcmAudio(Base64.decode(r.body()!!.audioContent!!, Base64.DEFAULT)) }
                else { tvStatus.text = "❌ v1beta1 error ${r.code()}"; resetSpeakButton() }
            } catch (e: Exception) { tvStatus.text = "❌ ${e.message}"; resetSpeakButton() }
        }
    }

    private fun selectV2Voice(index: Int) {
        selectedV2VoiceIndex = index
        v2VoiceButtons.forEachIndexed { i, btn -> highlightBtn(btn, i == index) }
        tvVertexVoiceLabel.text = "Voice: ${cloudV2Voices[index].displayName} (${cloudV2Voices[index].gender})"
    }

    private suspend fun playPcmAudio(pcm: ByteArray, sampleRate: Int = 24000) {
        withContext(Dispatchers.IO) {
            val ch = AudioFormat.CHANNEL_OUT_MONO; val fmt = AudioFormat.ENCODING_PCM_16BIT
            val buf = AudioTrack.getMinBufferSize(sampleRate, ch, fmt).coerceAtLeast(pcm.size)
            audioTrack?.release()
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(fmt).setSampleRate(sampleRate).setChannelMask(ch).build())
                .setBufferSizeInBytes(buf).setTransferMode(AudioTrack.MODE_STATIC).build()
            audioTrack?.write(pcm, 0, pcm.size); audioTrack?.play()
            Thread.sleep((pcm.size.toLong() * 1000L) / (sampleRate * 2) + 300)
        }
        withContext(Dispatchers.Main) { tvStatus.text = "✅ Done! Hold button to record again."; resetSpeakButton() }
    }

    private fun resetSpeakButton() { isSpeaking = false; btnSpeak.text = "🔊 Play Again"; btnSpeak.isEnabled = lastTranscript.isNotEmpty() }
    private fun stopPlayback() { androidTts?.stop(); audioTrack?.stop(); audioTrack?.release(); audioTrack = null; isSpeaking = false }

    private fun highlightBtn(btn: Button, active: Boolean) {
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(if (active) 0xFF1A73E8.toInt() else 0xFFE0E0E0.toInt())
        btn.setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xFF757575.toInt())
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_PERMISSION_CODE)
    }

    override fun onDestroy() { androidTts?.stop(); androidTts?.shutdown(); stopPlayback(); super.onDestroy() }
}