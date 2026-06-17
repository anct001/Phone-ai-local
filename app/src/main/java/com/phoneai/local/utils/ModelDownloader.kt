package com.phoneai.local.utils

import android.content.Context
import android.util.Log
import com.phoneai.local.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads GGUF model from HuggingFace with progress reporting.
 * Usage: pair with WorkManager for background resilient downloads.
 */
object ModelDownloader {

    private const val TAG = "ModelDownloader"

    // HuggingFace bartowski repo — all Gemma 3 4B quants
    private const val HF_BASE =
        "https://huggingface.co/bartowski/gemma-3-4b-it-GGUF/resolve/main"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // streaming download — no read timeout
        .build()

    data class Progress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val isDone: Boolean = false,
        val error: String? = null
    ) {
        val percent: Int get() =
            if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
    }

    fun download(context: Context, config: ModelConfig): Flow<Progress> = flow {
        val destFile = File(context.filesDir, config.fileName)
        val tempFile = File(context.filesDir, "${config.fileName}.part")

        val resumeOffset = if (tempFile.exists()) tempFile.length() else 0L
        val url = "$HF_BASE/${config.fileName}"

        Log.i(TAG, "Downloading $url (resume from $resumeOffset bytes)")

        val requestBuilder = Request.Builder().url(url)
        if (resumeOffset > 0) requestBuilder.header("Range", "bytes=$resumeOffset-")

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful && response.code != 206) {
            emit(Progress(0, 0, error = "HTTP ${response.code}"))
            return@flow
        }

        val contentLength = response.body?.contentLength() ?: -1L
        val totalBytes    = if (resumeOffset > 0) resumeOffset + contentLength else contentLength

        response.body?.byteStream()?.use { stream ->
            tempFile.outputStream().use { out ->
                if (resumeOffset > 0) {
                    // Append mode for resumed downloads
                    tempFile.outputStream().channel.also { it.position(resumeOffset) }
                }
                val buffer = ByteArray(8 * 1024)
                var downloaded = resumeOffset
                var read: Int

                while (stream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    downloaded += read
                    emit(Progress(downloaded, totalBytes))
                }
            }
        }

        tempFile.renameTo(destFile)
        emit(Progress(totalBytes, totalBytes, isDone = true))
        Log.i(TAG, "Download complete: ${destFile.absolutePath}")

    }.flowOn(Dispatchers.IO)
}
