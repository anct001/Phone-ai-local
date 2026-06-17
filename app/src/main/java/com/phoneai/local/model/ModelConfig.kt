package com.phoneai.local.model

/**
 * Recommended Gemma 3 4B variants.
 * Download from: https://huggingface.co/bartowski/gemma-3-4b-it-GGUF
 */
data class ModelConfig(
    val displayName: String,
    val fileName: String,       // GGUF filename expected in app's files dir
    val sizeGb: Float,
    val nCtx: Int,
    val nGpuLayers: Int,
    val nThreads: Int,
    val description: String
) {
    companion object {
        // Snapdragon 8s Gen 4 + 16 GB RAM presets
        val GEMMA3_4B_Q4_K_M = ModelConfig(
            displayName  = "Gemma 3 4B (Q4_K_M) — Recommended",
            fileName     = "gemma-3-4b-it-Q4_K_M.gguf",
            sizeGb       = 2.5f,
            nCtx         = 4096,
            nGpuLayers   = 28,
            nThreads     = 4,
            description  = "Best balance: ~30-40 tok/s, ~2.5 GB RAM"
        )

        val GEMMA3_4B_Q5_K_M = ModelConfig(
            displayName  = "Gemma 3 4B (Q5_K_M) — Higher quality",
            fileName     = "gemma-3-4b-it-Q5_K_M.gguf",
            sizeGb       = 3.0f,
            nCtx         = 4096,
            nGpuLayers   = 28,
            nThreads     = 4,
            description  = "Better quality, ~20% slower, ~3 GB RAM"
        )

        val GEMMA3_4B_Q3_K_M = ModelConfig(
            displayName  = "Gemma 3 4B (Q3_K_M) — Fast mode",
            fileName     = "gemma-3-4b-it-Q3_K_M.gguf",
            sizeGb       = 2.0f,
            nCtx         = 8192,
            nGpuLayers   = 33,   // all layers — smallest model
            nThreads     = 4,
            description  = "Fastest, ~50+ tok/s, larger context (8k)"
        )

        val ALL = listOf(GEMMA3_4B_Q4_K_M, GEMMA3_4B_Q5_K_M, GEMMA3_4B_Q3_K_M)
        val DEFAULT = GEMMA3_4B_Q4_K_M
    }
}
