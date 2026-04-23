package com.penpal.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var outputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecordingFlow: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    suspend fun startRecording(storyId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext Result.failure(SecurityException("RECORD_AUDIO permission not granted"))
            }

            val recordingsDir = File(context.filesDir, "recordings/$storyId")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }

            outputFile = File(recordingsDir, "${System.currentTimeMillis()}.wav")
            fileOutputStream = FileOutputStream(outputFile)

            writeWavHeader(fileOutputStream!!, sampleRate, 1, 16)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(Exception("AudioRecord initialization failed"))
            }

            audioRecord?.startRecording()
            isRecording = true
            _isRecording.value = true
            _durationMs.value = 0L

            val buffer = ByteArray(bufferSize)
            val startTime = System.currentTimeMillis()

            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    fileOutputStream?.write(buffer, 0, read)

                    var sum = 0
                    for (i in 0 until read step 2) {
                        val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                        sum += sample * sample
                    }
                    val rms = kotlin.math.sqrt(sum.toFloat() / read)
                    _amplitude.value = (rms / 32768f).coerceIn(0f, 1f)

                    _durationMs.value = System.currentTimeMillis() - startTime
                }
            }

            Result.success(outputFile!!.absolutePath)
        } catch (e: Exception) {
            stopRecording()
            Result.failure(e)
        }
    }

    suspend fun stopRecording(): String? = withContext(Dispatchers.IO) {
        isRecording = false
        _isRecording.value = false
        _amplitude.value = 0f

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        fileOutputStream?.close()
        fileOutputStream = null

        updateWavHeader(outputFile)

        outputFile?.absolutePath
    }

    fun cancelRecording() {
        isRecording = false
        _isRecording.value = false
        _amplitude.value = 0f

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        fileOutputStream?.close()
        fileOutputStream = null

        outputFile?.delete()
        outputFile = null
    }

    private fun writeWavHeader(out: FileOutputStream, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val totalDataLen = 0L
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()

        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        header[20] = 1
        header[21] = 0

        header[22] = channels.toByte()
        header[23] = 0

        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()

        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()

        header[32] = (blockAlign and 0xff).toByte()
        header[33] = ((blockAlign shr 8) and 0xff).toByte()

        header[34] = bitsPerSample.toByte()
        header[35] = 0

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        header[40] = 0
        header[41] = 0
        header[42] = 0
        header[43] = 0

        out.write(header)
    }

    private fun updateWavHeader(file: File?) {
        file ?: return

        try {
            val raf = RandomAccessFile(file, "rw")
            val dataSize = (raf.length() - 44).toInt()

            raf.seek(4)
            raf.write(dataSize and 0xff)
            raf.write((dataSize shr 8) and 0xff)
            raf.write((dataSize shr 16) and 0xff)
            raf.write((dataSize shr 24) and 0xff)

            raf.seek(40)
            raf.write(dataSize and 0xff)
            raf.write((dataSize shr 8) and 0xff)
            raf.write((dataSize shr 16) and 0xff)
            raf.write((dataSize shr 24) and 0xff)

            raf.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}