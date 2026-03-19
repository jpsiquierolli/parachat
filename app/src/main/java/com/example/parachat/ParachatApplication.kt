package com.example.parachat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ParachatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase is used for authentication only.
    }
}
