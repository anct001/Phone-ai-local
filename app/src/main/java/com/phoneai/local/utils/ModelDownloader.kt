package com.phoneai.local.utils

import android.content.Context
import android.util.Log
import com.phoneai.local.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads a GGUF model from its HuggingFace URL with resumable progress.
 *
 * Downloads to "<name>.part" and atomically renames on success, so a partially
 * downloaded file is never mistaken for a complete model. Cancel the collecting
 * coroutine to pause — the .part file is kept and resumed next time.
 */
object ModelDownloader {

    private const val TAG = "ModelDownloader"

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

    fun destFile(context: Context, config: ModelConfig): File =
        File(context.filesDir, config.fileName)

    fun download(context: Context, config: ModelConfig): Flow<Progress> = flow {
        val destFile = destFile(context, config)
        val tempFile = File(context.filesDir, "${config.fileName}.part")

        if (destFile.exists()) {
            emit(Progress(destFile.length(), destFile.length(), isDone = true))
            return@flow
        }

        val resumeOffset = if (tempFile.exists()) tempFile.length() else 0L
        Log.i(TAG, "Downloading ${config.downloadUrl} (resume from $resumeOffset bytes)")

        val requestBuilder = Request.Builder().url(config.downloadUrl)
        if (resumeOffset > 0) requestBuilder.header("Range", "bytes=$resumeOffset-")

        val response = client.newCall(requestBuilder.build()).execute()
        try {
            // 200 = full body, 206 = partial (resume accepted)
            if (!response.isSuccessful) {
                emit(Progress(0, 0, error = "HTTP ${response.code}"))
                return@flow
            }
            val serverResumed = response.code == 206
            val startOffset = if (serverResumed) resumeOffset else 0L

            val body = response.body ?: run {
                emit(Progress(0, 0, error = "Empty response body"))
                return@flow
            }
            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) startOffset + contentLength else config.sizeMb * 1024L * 1024L

            // append=serverResumed: continue the .part file; otherwise overwrite
            FileOutputStream(tempFile, serverResumed).use { out ->
                body.byteStream().use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = startOffset
                    var read: Int
                    var lastEmit = 0L

                    while (stream.read(buffer).also { read = it } != -1) {
                        if (!currentCoroutineContext().isActive) {
                            Log.i(TAG, "Download paused at $downloaded bytes")
                            return@flow   // keep .part for resume
                        }
                        out.write(buffer, 0, read)
                        downloaded += read
                        // throttle emissions to ~every 512 KB to avoid flooding UI
                        if (downloaded - lastEmit >= 512 * 1024) {
                            emit(Progress(downloaded, totalBytes))
                            lastEmit = downloaded
                        }
                    }
                }
            }

            if (tempFile.renameTo(destFile)) {
                emit(Progress(destFile.length(), destFile.length(), isDone = true))
                Log.i(TAG, "Download complete: ${destFile.absolutePath}")
            } else {
                emit(Progress(0, 0, error = "Không thể lưu file model"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            emit(Progress(0, 0, error = e.message ?: "Lỗi tải xuống"))
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)
}
