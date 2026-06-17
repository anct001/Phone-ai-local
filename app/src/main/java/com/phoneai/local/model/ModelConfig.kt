package com.phoneai.local.model

/**
 * Full metadata for a downloadable GGUF model.
 *
 * The number of layers offloaded to the Vulkan GPU is NOT stored here — it is
 * decided at load time by [com.phoneai.local.utils.DeviceInfo] from [totalLayers]
 * and the device's free RAM.
 */
data class ModelConfig(
    val id: String,
    val displayName: String,
    val family: String,          // "Gemma 3", "Qwen2.5", ...
    val paramsLabel: String,     // "4B"
    val quant: String,           // "Q4_K_M"
    val fileName: String,        // GGUF filename stored in app files dir
    val downloadUrl: String,     // full HuggingFace resolve URL
    val sizeMb: Int,             // download size on disk
    val ramRequiredMb: Int,      // approx runtime RAM (weights + KV cache)
    val totalLayers: Int,        // transformer block count (for GPU offload)
    val nCtx: Int,               // context window
    val nThreads: Int,           // CPU threads
    val speedLabel: String,      // "~35 tok/s" (Snapdragon 8s Gen 4 estimate)
    val qualityStars: Int,       // 1..5
    val tags: List<String>,      // ["Tiếng Việt tốt", "Đa ngôn ngữ"]
    val description: String      // detailed Vietnamese description
) {
    val sizeGb: Float get() = sizeMb / 1024f

    companion object {
        val DEFAULT_ID = "gemma3-4b-q4"
    }
}
