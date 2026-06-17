package com.phoneai.local.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * JNI bridge to llama.cpp with Vulkan backend.
 * All heavy work runs on IO dispatcher; tokens stream via Flow.
 */
class LlamaEngine {

    companion object {
        private const val TAG = "LlamaEngine"

        // Tuned for Snapdragon 8s Gen 4:
        // - 28 GPU layers  → Adreno 740 handles most of the matmuls
        // - 4 CPU threads  → leave cores for UI / background
        // - 4096 ctx        → ~4k token window, fits in ~600 MB
        const val DEFAULT_N_CTX        = 4096
        const val DEFAULT_N_THREADS    = 4
        const val DEFAULT_N_GPU_LAYERS = 28

        init {
            System.loadLibrary("phoneai")
        }
    }

    // ── Native declarations ────────────────────────────────────────────────────

    private external fun nativeLoadModel(
        modelPath: String,
        nCtx: Int,
        nThreads: Int,
        nGpuLayers: Int
    ): Boolean

    private external fun nativeGenerate(
        prompt: String,
        maxNewTokens: Int,
        temperature: Float,
        topP: Float
    )

    private external fun nativeStop()
    private external fun nativeFree()
    private external fun nativeGetContextSize(): Int

    // Called from C++ nativeGenerate on each decoded piece
    @Suppress("unused")
    private fun onToken(token: String) {
        tokenCallback?.invoke(token)
    }

    private var tokenCallback: ((String) -> Unit)? = null

    // ── Public API ────────────────────────────────────────────────────────────

    var isLoaded: Boolean = false
        private set

    suspend fun loadModel(
        modelPath: String,
        nCtx: Int        = DEFAULT_N_CTX,
        nThreads: Int    = DEFAULT_N_THREADS,
        nGpuLayers: Int  = DEFAULT_N_GPU_LAYERS
    ): Boolean = withContext(Dispatchers.IO) {
        val ok = nativeLoadModel(modelPath, nCtx, nThreads, nGpuLayers)
        isLoaded = ok
        if (!ok) Log.e(TAG, "Model failed to load: $modelPath")
        ok
    }

    /**
     * Returns a cold Flow of token strings. Collect on any scope;
     * the flow completes when generation finishes or [stop] is called.
     */
    fun generate(
        prompt: String,
        maxNewTokens: Int = 512,
        temperature: Float = 0.7f,
        topP: Float = 0.9f
    ): Flow<String> = callbackFlow {
        tokenCallback = { token ->
            trySend(token)
        }
        withContext(Dispatchers.IO) {
            nativeGenerate(prompt, maxNewTokens, temperature, topP)
        }
        tokenCallback = null
        close()
        awaitClose { nativeStop() }
    }

    fun stop() = nativeStop()

    fun free() {
        nativeFree()
        isLoaded = false
    }

    fun contextSize(): Int = nativeGetContextSize()
}
