package com.phoneai.local.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log

object DeviceInfo {

    private const val TAG = "DeviceInfo"

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

    /**
     * Suggests n_gpu_layers based on available RAM.
     * Gemma 3 4B Q4_K_M has 35 layers total.
     */
    fun suggestGpuLayers(context: Context): Int {
        val availMb = availableRamMb(context)
        return when {
            availMb >= 6000 -> 35   // all layers on GPU
            availMb >= 4000 -> 28   // most layers on GPU
            availMb >= 2000 -> 18   // hybrid
            else            -> 0    // CPU only
        }
    }
}
