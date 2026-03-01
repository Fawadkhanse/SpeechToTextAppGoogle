package com.example.speechtotextapp.helper

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import androidx.annotation.RequiresPermission

class AudioRecorder {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val audioData = mutableListOf<Byte>()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        audioData.clear()
        isRecording = true
        audioRecord?.startRecording()

        Thread {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    audioData.addAll(buffer.take(read))
                }
            }
        }.start()
    }

    fun stopRecording(): String {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        // Return Base64 encoded audio
        return Base64.encodeToString(audioData.toByteArray(), Base64.NO_WRAP)
    }
}