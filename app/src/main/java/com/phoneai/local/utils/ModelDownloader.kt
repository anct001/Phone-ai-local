package com.phoneai.local.utils

import android.content.Context
import android.util.Log
import com.phoneai.local.model.ModelConfig
import kotlinx.coroutines.Dispatchers
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
 * Writes to "<name>.part" and renames atomically on success. Cancel the
 * collecting coroutine to pause — the .part file is kept for the next resume.
 */
object ModelDownloader {

    private const val TAG = "ModelDownloader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    data class Progress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val isDone: Boolean = false,
        val error: String? = null
    ) {
        val percent: Int
            get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
    }

    fun destFile(context: Context, config: ModelConfig): File =
        File(context.filesDir, config.fileName)

    fun download(context: Context, config: ModelConfig): Flow<Progress> = flow {
        val dest = destFile(context, config)
        val part = File(context.filesDir, "${config.fileName}.part")

        if (dest.exists()) {
            emit(Progress(dest.length(), dest.length(), isDone = true))
            return@flow
        }

        val resumeFrom = if (part.exists()) part.length() else 0L
        Log.i(TAG, "Downloading ${config.downloadUrl} resume=$resumeFrom")

        val request = Request.Builder()
            .url(config.downloadUrl)
            .apply { if (resumeFrom > 0) header("Range", "bytes=$resumeFrom-") }
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            emit(Progress(0L, 0L, error = "Không kết nối được: ${e.message}"))
            return@flow
        }

        if (!response.isSuccessful) {
            response.close()
            emit(Progress(0L, 0L, error = "HTTP ${response.code}"))
            return@flow
        }

        val serverResumed = response.code == 206
        val startAt = if (serverResumed) resumeFrom else 0L
        val body = response.body
        if (body == null) {
            response.close()
            emit(Progress(0L, 0L, error = "Empty response body"))
            return@flow
        }

        val contentLen = body.contentLength()
        val totalBytes: Long = when {
            contentLen > 0 -> startAt + contentLen
            else           -> config.sizeMb.toLong() * 1024L * 1024L
        }

        try {
            FileOutputStream(part, serverResumed).use { out ->
                val inputStream = body.byteStream()
                val buffer = ByteArray(64 * 1024)
                var downloaded = startAt
                var lastEmit = 0L

                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    // isActive is accessible here because flow{} runs in a coroutine
                    // and flowOn(IO) propagates cancellation through the IO dispatcher.
                    if (!isActive) {
                        Log.i(TAG, "Download cancelled at $downloaded bytes")
                        return@flow
                    }
                    out.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (downloaded - lastEmit >= 512L * 1024L) {
                        emit(Progress(downloaded, totalBytes))
                        lastEmit = downloaded
                    }
                }
            }

            if (part.renameTo(dest)) {
                emit(Progress(dest.length(), dest.length(), isDone = true))
                Log.i(TAG, "Download complete: ${dest.absolutePath}")
            } else {
                emit(Progress(0L, 0L, error = "Không thể lưu file — kiểm tra dung lượng"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            emit(Progress(0L, 0L, error = e.message ?: "Lỗi không xác định"))
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)
}
