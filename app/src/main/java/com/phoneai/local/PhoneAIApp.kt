package com.phoneai.local

import android.app.Application
import com.phoneai.local.utils.DeviceInfo

class PhoneAIApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DeviceInfo.logDeviceInfo(this)
    }
}
