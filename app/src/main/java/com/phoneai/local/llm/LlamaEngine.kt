package com.phoneai.local.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * JNI bridge to llama.cpp (CPU / ARM NEON backend).
 * All heavy work runs on IO dispatcher; tokens stream via Flow.
 *
 * GPU offload is currently disabled — building Vulkan shaders requires
 * glslc (Vulkan SDK) on the Windows host. To enable it:
 *   1. Install Vulkan SDK and add glslc to PATH
 *   2. Set GGML_VULKAN=ON in CMakeLists.txt
 *   3. Change DEFAULT_N_GPU_LAYERS to 28 (or use DeviceInfo.suggestGpuLayers)
 */
class LlamaEngine {

    companion object {
        private const val TAG = "LlamaEngine"

        // CPU-only preset for Snapdragon 8s Gen 4:
        // - 0 GPU layers   → all computation on ARM NEON CPU
        // - 6 CPU threads  → 6 of the 8 cores for inference, 2 for UI
        // - 4096 ctx       → ~600 MB KV cache
        const val DEFAULT_N_CTX        = 4096
        const val DEFAULT_N_THREADS    = 6
        const val DEFAULT_N_GPU_LAYERS = 0

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
