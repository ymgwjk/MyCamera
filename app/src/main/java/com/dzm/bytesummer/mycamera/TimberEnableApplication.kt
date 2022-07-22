package com.dzm.bytesummer.mycamera

import android.app.Application
import timber.log.Timber

class TimberEnableApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}