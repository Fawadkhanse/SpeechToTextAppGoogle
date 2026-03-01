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

    // Use a larger buffer to reduce read frequency
    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4,
        4096
    )

    private var audioRecord:    AudioRecord? = null
    private var recordingThread: Thread?     = null

    @Volatile
    private var isRecording = false

    private val audioData = mutableListOf<Byte>()

    fun startRecording() {
        // Clean up any previous instance
        cleanup()

        try {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord failed to initialize")
                recorder.release()
                return
            }

            audioRecord = recorder
            audioData.clear()
            isRecording = true
            recorder.startRecording()

            recordingThread = Thread {
                val buffer = ByteArray(bufferSize)
                Log.d("AudioRecorder", "Recording thread started")

                // Keep reading while isRecording is true AND record is active
                while (isRecording) {
                    val read = recorder.read(buffer, 0, bufferSize)
                    when {
                        read > 0 -> synchronized(audioData) {
                            audioData.addAll(buffer.take(read))
                        }
                        read == AudioRecord.ERROR_INVALID_OPERATION -> {
                            Log.w("AudioRecorder", "ERROR_INVALID_OPERATION — stopping loop")
                            break
                        }
                        read == AudioRecord.ERROR_BAD_VALUE -> {
                            Log.w("AudioRecorder", "ERROR_BAD_VALUE — stopping loop")
                            break
                        }
                    }
                }

                Log.d("AudioRecorder", "Recording thread exited")
            }.also { it.start() }

            Log.d("AudioRecorder", "Recording started, bufferSize=$bufferSize")

        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording: ${e.message}")
        }
    }

    fun stopRecording(): String {
        // ── KEY FIX ────────────────────────────────────────────────────────
        // 1. Stop the AudioRecord FIRST — this causes any blocking read()
        //    call to return immediately with an error/0, unblocking the thread.
        // 2. Then set isRecording = false so the thread exits its loop.
        // 3. Join the thread so we wait for it to fully exit before release().
        // 4. Only THEN call release() — no more crash.
        // ───────────────────────────────────────────────────────────────────
        try {
            audioRecord?.stop()           // unblocks read() in the thread
        } catch (e: Exception) {
            Log.w("AudioRecorder", "stop() threw: ${e.message}")
        }

        isRecording = false               // signals thread loop to exit

        try {
            recordingThread?.join(2000)   // wait up to 2s for thread to finish
        } catch (e: InterruptedException) {
            Log.w("AudioRecorder", "join interrupted")
        }
        recordingThread = null

        try {
            audioRecord?.release()        // safe to release now — thread is done
        } catch (e: Exception) {
            Log.w("AudioRecorder", "release() threw: ${e.message}")
        }
        audioRecord = null

        val bytes = synchronized(audioData) { audioData.toByteArray() }
        Log.d("AudioRecorder", "Stopped. Captured ${bytes.size} bytes")

        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun cleanup() {
        isRecording = false
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { recordingThread?.join(1000) } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord    = null
        recordingThread = null
        audioData.clear()
    }
}