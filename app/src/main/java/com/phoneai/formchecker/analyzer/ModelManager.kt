package com.phoneai.formchecker.analyzer

import android.content.Context
import android.net.Uri
import com.phoneai.formchecker.BuildConfig
import java.io.File

/**
 * Quản lý file model AI on-device (Gemma 3n) và lựa chọn engine phân tích.
 *
 * Model rất lớn (~1.5–3GB) nên KHÔNG nhúng vào APK. Người dùng tự tải file
 * model (.task / .litertlm) rồi import vào app qua bộ chọn file; file sẽ được
 * copy vào bộ nhớ riêng của app.
 */
object ModelManager {

    private const val PREFS = "form_checker_prefs"
    private const val KEY_ENGINE = "engine"
    const val MODEL_FILE_NAME = "gemma-3n.task"

    /** Thư mục chứa model trong bộ nhớ riêng của app. */
    fun modelDir(context: Context): File =
        File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    fun modelFile(context: Context): File =
        File(modelDir(context), MODEL_FILE_NAME)

    fun isModelReady(context: Context): Boolean {
        val f = modelFile(context)
        return f.exists() && f.length() > 50L * 1024 * 1024 // > 50MB mới coi là hợp lệ
    }

    fun modelSizeMb(context: Context): Long =
        if (modelFile(context).exists()) modelFile(context).length() / (1024 * 1024) else 0

    /**
     * Copy model từ Uri (người dùng chọn) vào bộ nhớ app.
     * @param onProgress nhận số byte đã copy để cập nhật tiến trình.
     */
    fun importModel(context: Context, uri: Uri, onProgress: (Long) -> Unit) {
        val dest = modelFile(context)
        val tmp = File(modelDir(context), "$MODEL_FILE_NAME.tmp")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Không mở được file đã chọn" }
            tmp.outputStream().use { output ->
                val buffer = ByteArray(1 shl 20) // 1MB
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    total += read
                    onProgress(total)
                }
            }
        }
        if (dest.exists()) dest.delete()
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
    }

    fun deleteModel(context: Context) {
        modelFile(context).delete()
    }

    /** Engine được chọn. Mặc định ưu tiên local (đúng định hướng app). */
    fun getEngine(context: Context): AnalyzerEngine {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_ENGINE, AnalyzerEngine.LOCAL_GEMMA.name)
        return runCatching { AnalyzerEngine.valueOf(name!!) }
            .getOrDefault(AnalyzerEngine.LOCAL_GEMMA)
    }

    fun setEngine(context: Context, engine: AnalyzerEngine) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ENGINE, engine.name).apply()
    }

    fun hasCloudKey(): Boolean = BuildConfig.CLAUDE_API_KEY.isNotEmpty()

    /**
     * Tạo analyzer phù hợp với engine + tài nguyên hiện có.
     * Ưu tiên engine người dùng chọn; nếu không khả dụng thì fallback.
     */
    fun createAnalyzer(context: Context): FormAnalyzer {
        val engine = getEngine(context)
        return when (engine) {
            AnalyzerEngine.LOCAL_GEMMA -> {
                check(isModelReady(context)) {
                    "Chưa có model AI on-device. Vào màn hình chính → \"Quản lý model AI\" để import file model (.task)."
                }
                LocalGemmaAnalyzer(context, modelFile(context).absolutePath)
            }
            AnalyzerEngine.CLAUDE_CLOUD -> {
                check(hasCloudKey()) { "Chưa cấu hình CLAUDE_API_KEY." }
                com.phoneai.formchecker.api.ClaudeApiService()
            }
        }
    }
}
