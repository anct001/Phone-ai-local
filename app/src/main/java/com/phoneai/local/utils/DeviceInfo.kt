package com.phoneai.local.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.phoneai.local.model.ModelConfig

object DeviceInfo {

    private const val TAG = "DeviceInfo"

    /** Headroom to keep free for the OS + app UI while a model is loaded. */
    private const val RAM_HEADROOM_MB = 1200L

    /** Available RAM in MB */
    fun availableRamMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem / (1024 * 1024)
    }

    /** Total physical RAM in MB */
    fun totalRamMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / (1024 * 1024)
    }

    /** True if device has Vulkan 1.1+ support (required for GPU offload) */
    fun hasVulkan11(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q  // API 29 = Android 10

    fun logDeviceInfo(context: Context) {
        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.i(TAG, "CPU ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
        Log.i(TAG, "API level: ${Build.VERSION.SDK_INT}")
        Log.i(TAG, "Total RAM: ${totalRamMb(context)} MB")
        Log.i(TAG, "Available RAM: ${availableRamMb(context)} MB")
        Log.i(TAG, "Vulkan 1.1+: ${hasVulkan11()}")
    }

    // ── Compatibility assessment ───────────────────────────────────────────────

    enum class Fit {
        RECOMMENDED,  // plenty of headroom, runs fast
        GOOD,         // fits comfortably
        HEAVY,        // fits but will be slow / hot
        INSUFFICIENT  // not enough RAM — risky / will OOM
    }

    data class Compatibility(
        val fit: Fit,
        val label: String,       // short badge text (Vietnamese)
        val reason: String,      // one-line explanation
        val gpuLayers: Int       // suggested layers to offload to Vulkan GPU
    )

    /**
     * Compares a model's RAM footprint against this device's free memory and
     * returns a human-friendly verdict plus the GPU offload count to use.
     */
    fun assess(context: Context, config: ModelConfig): Compatibility {
        val avail = availableRamMb(context)
        val need  = config.ramRequiredMb + RAM_HEADROOM_MB
        val gpu   = suggestGpuLayers(context, config)

        return when {
            need > avail -> Compatibility(
                fit = Fit.INSUFFICIENT,
                label = "Thiếu RAM",
                reason = "Cần ~${config.ramRequiredMb} MB nhưng chỉ còn ~$avail MB trống.",
                gpuLayers = gpu
            )
            config.ramRequiredMb <= avail * 0.45 -> Compatibility(
                fit = Fit.RECOMMENDED,
                label = "Khuyên dùng",
                reason = "Chạy mượt với nhiều RAM dư (${config.speedLabel}).",
                gpuLayers = gpu
            )
            config.ramRequiredMb <= avail * 0.70 -> Compatibility(
                fit = Fit.GOOD,
                label = "Phù hợp",
                reason = "Chạy tốt trên máy của bạn (${config.speedLabel}).",
                gpuLayers = gpu
            )
            else -> Compatibility(
                fit = Fit.HEAVY,
                label = "Hơi nặng",
                reason = "Chạy được nhưng tốn nhiều RAM, máy có thể nóng/chậm hơn.",
                gpuLayers = gpu
            )
        }
    }

    /**
     * How many transformer layers to offload to the Vulkan GPU.
     * Full offload when there's comfortable headroom; otherwise partial.
     */
    fun suggestGpuLayers(context: Context, config: ModelConfig): Int {
        val avail = availableRamMb(context)
        val need  = config.ramRequiredMb
        return when {
            avail >= need * 1.4 -> config.totalLayers          // full GPU offload
            avail >= need * 1.1 -> (config.totalLayers * 0.75).toInt()
            avail >= need       -> (config.totalLayers * 0.5).toInt()
            else                -> (config.totalLayers * 0.25).toInt()
        }.coerceIn(0, config.totalLayers)
    }
}
