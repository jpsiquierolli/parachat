package com.example.parachat

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ParachatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            try {
                // Fallback for missing or incorrect google-services.json config
                FirebaseDatabase.getInstance("https://parachat-50788-default-rtdb.firebaseio.com/").setPersistenceEnabled(true)
            } catch (e2: Exception) {
                // Log and ignore to prevent crash
                android.util.Log.e("ParachatApplication", "Failed to enable Firebase persistence: ${e.message}")
            }
        }
    }
}
