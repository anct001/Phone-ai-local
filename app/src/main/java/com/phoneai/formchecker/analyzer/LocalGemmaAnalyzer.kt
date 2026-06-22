package com.phoneai.formchecker.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import java.io.File

/**
 * Engine phân tích chạy hoàn toàn trên thiết bị bằng Gemma 3n (đa phương thức)
 * thông qua MediaPipe LLM Inference API. Không cần mạng, không cần API key.
 *
 * Yêu cầu:
 *  - File model Gemma 3n (.task) hỗ trợ ảnh, đã import vào app.
 *  - Thiết bị đủ mạnh (RAM khuyến nghị >= 6–8GB).
 */
class LocalGemmaAnalyzer(
    private val context: Context,
    private val modelPath: String
) : FormAnalyzer {

    private val gson = Gson()
    private var llmInference: LlmInference? = null

    private fun ensureEngine(): LlmInference {
        llmInference?.let { return it }
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(4096)
            .setMaxNumImages(1)
            .build()
        return LlmInference.createFromOptions(context, options).also { llmInference = it }
    }

    override fun analyze(imageFile: File): FormAnalysisResult {
        val engine = ensureEngine()

        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(40)
            .setTemperature(0.2f)
            .setGraphOptions(
                GraphOptions.builder().setEnableVisionModality(true).build()
            )
            .build()

        val session = LlmInferenceSession.createFromOptions(engine, sessionOptions)
        try {
            val bitmap = decodeScaled(imageFile)
            val mpImage = BitmapImageBuilder(bitmap).build()

            // Prompt: gộp hướng dẫn hệ thống + yêu cầu, kèm ảnh.
            session.addQueryChunk(FormPrompt.SYSTEM_PROMPT + "\n\n" + FormPrompt.USER_PROMPT)
            session.addImage(mpImage)

            val response = session.generateResponse()
            val jsonText = FormPrompt.extractJson(response)
            val parsed = gson.fromJson(jsonText, FormAnalysisResult::class.java)
            return FormPrompt.sanitize(parsed)
        } finally {
            runCatching { session.close() }
        }
    }

    /** Giảm kích thước ảnh để tiết kiệm RAM khi đưa vào model on-device. */
    private fun decodeScaled(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val maxDim = 1536
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: throw RuntimeException("Không đọc được ảnh: ${file.absolutePath}")
    }

    override fun close() {
        runCatching { llmInference?.close() }
        llmInference = null
    }
}
