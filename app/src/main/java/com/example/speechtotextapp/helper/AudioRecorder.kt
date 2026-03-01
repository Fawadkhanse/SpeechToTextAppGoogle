package com.example.speechtotextapp.helper

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log

class AudioRecorder {

    private val sampleRate   = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat  = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize   = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var isRecording               = false
    private val audioData                 = mutableListOf<Byte>()

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
                return
            }

            audioData.clear()
            isRecording = true
            audioRecord?.startRecording()

            Thread {
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        synchronized(audioData) {
                            audioData.addAll(buffer.take(read))
                        }
                    }
                }
            }.start()

            Log.d("AudioRecorder", "Recording started")

        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording: ${e.message}")
        }
    }

    fun stopRecording(): String {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val bytes = synchronized(audioData) { audioData.toByteArray() }
        Log.d("AudioRecorder", "Recording stopped. Captured ${bytes.size} bytes")

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}