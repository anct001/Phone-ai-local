package com.phoneai.local.llm

import android.content.Context
import android.util.Log
import com.phoneai.local.model.ChatMessage
import com.phoneai.local.model.ModelConfig
import com.phoneai.local.model.toGemma3Prompt
import kotlinx.coroutines.flow.Flow
import java.io.File

class InferenceManager(private val context: Context) {

    private val engine = LlamaEngine()

    companion object {
        private const val TAG = "InferenceManager"
        private const val SYSTEM_PROMPT = """Bạn là trợ lý AI thông minh, thân thiện và hữu ích.
Trả lời ngắn gọn, súc tích và chính xác bằng ngôn ngữ của người dùng.
Nếu không biết, hãy thành thật nói không biết thay vì bịa đặt."""
    }

    val isModelLoaded: Boolean get() = engine.isLoaded

    fun modelFilePath(config: ModelConfig): File =
        File(context.filesDir, config.fileName)

    fun isModelDownloaded(config: ModelConfig): Boolean =
        modelFilePath(config).exists()

    suspend fun loadModel(config: ModelConfig): Result<Unit> {
        val file = modelFilePath(config)
        if (!file.exists()) {
            return Result.failure(IllegalStateException("Model file not found: ${file.path}"))
        }
        Log.i(TAG, "Loading ${config.displayName} (${config.sizeGb} GB)")
        val ok = engine.loadModel(
            modelPath  = file.absolutePath,
            nCtx       = config.nCtx,
            nThreads   = config.nThreads,
            nGpuLayers = config.nGpuLayers
        )
        return if (ok) Result.success(Unit)
        else Result.failure(RuntimeException("llama.cpp failed to load model"))
    }

    fun chat(
        history: List<ChatMessage>,
        maxNewTokens: Int = 512,
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Flow<String> {
        val prompt = history.toGemma3Prompt(SYSTEM_PROMPT)
        Log.d(TAG, "Prompt length: ${prompt.length} chars")
        return engine.generate(prompt, maxNewTokens, temperature, topP)
    }

    fun stopGeneration() = engine.stop()

    fun unloadModel() = engine.free()
}
