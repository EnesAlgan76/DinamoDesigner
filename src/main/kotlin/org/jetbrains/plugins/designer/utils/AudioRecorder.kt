package org.jetbrains.plugins.designer.utils

import java.io.File
import javax.sound.sampled.*

class AudioRecorder {
    private var targetDataLine: TargetDataLine? = null
    private var audioThread: Thread? = null
    private var isRecording = false
    private var audioFile: File? = null

    companion object {
        private const val SAMPLE_RATE = 16000f
        private const val SAMPLE_SIZE_IN_BITS = 16
        private const val CHANNELS = 1
        private const val SIGNED = true
        private const val BIG_ENDIAN = false
    }

    /**
     * Starts audio recording
     */
    fun startRecording(): File? {
        try {
            val audioFormat = AudioFormat(
                SAMPLE_RATE,
                SAMPLE_SIZE_IN_BITS,
                CHANNELS,
                SIGNED,
                BIG_ENDIAN
            )

            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)

            if (!AudioSystem.isLineSupported(info)) {
                throw RuntimeException("Mikrofon desteklenmiyor")
            }

            targetDataLine = AudioSystem.getLine(info) as TargetDataLine
            targetDataLine?.open(audioFormat)
            targetDataLine?.start()

            // Create temporary file for audio
            audioFile = File.createTempFile("recording_", ".wav")
            isRecording = true

            // Start recording in background thread
            audioThread = Thread {
                try {
                    val audioInputStream = AudioInputStream(targetDataLine)
                    AudioSystem.write(
                        audioInputStream,
                        AudioFileFormat.Type.WAVE,
                        audioFile!!
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            audioThread?.start()

            return audioFile

        } catch (e: Exception) {
            throw RuntimeException("Ses kaydı başlatılamadı: ${e.message}", e)
        }
    }

    /**
     * Stops audio recording
     */
    fun stopRecording(): File? {
        isRecording = false

        try {
            targetDataLine?.stop()
            targetDataLine?.close()

            // Wait for recording thread to finish
            audioThread?.join(1000)

            return audioFile
        } catch (e: Exception) {
            throw RuntimeException("Ses kaydı durdurulamadı: ${e.message}", e)
        }
    }

    /**
     * Checks if recording is in progress
     */
    fun isRecording(): Boolean = isRecording
}