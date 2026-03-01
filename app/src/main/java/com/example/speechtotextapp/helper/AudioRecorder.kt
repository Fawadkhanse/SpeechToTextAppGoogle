package com.example.speechtotextapp.helper

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log

class AudioRecorder {

    private val sampleRate    = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat   = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize    = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        .coerceAtLeast(4096)   // safety floor

    private var audioRecord: AudioRecord? = null

    @Volatile private var isRecording = false

    // All captured bytes land here
    private val audioData = mutableListOf<Byte>()

    // ── Start ─────────────────────────────────────────────
    fun startRecording() {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord failed to initialize")
                audioRecord = null
                return
            }

            audioData.clear()
            isRecording = true
            audioRecord!!.startRecording()

            // Run recording loop on a dedicated thread
            Thread {
                Log.d("AudioRecorder", "Recording thread started")
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: break
                    if (read > 0) {
                        synchronized(audioData) {
                            for (i in 0 until read) audioData.add(buffer[i])
                        }
                    }
                }
                Log.d("AudioRecorder", "Recording thread ended")
            }.also {
                it.isDaemon = true
                it.start()
            }

            Log.d("AudioRecorder", "Recording started. bufferSize=$bufferSize")

        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "startRecording error: ${e.message}")
        }
    }

    // ── Stop ──────────────────────────────────────────────
    fun stopRecording(): String {
        isRecording = false

        // Give the recording thread a moment to flush its last buffer
        Thread.sleep(200)

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val bytes = synchronized(audioData) { audioData.toByteArray() }
        Log.d("AudioRecorder", "Stopped. Captured ${bytes.size} bytes")

        if (bytes.isEmpty()) {
            Log.w("AudioRecorder", "No audio captured!")
            return ""
        }

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}