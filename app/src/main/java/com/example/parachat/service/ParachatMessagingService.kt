package com.example.parachat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.parachat.MainActivity
import com.example.parachat.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ParachatMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            showNotification(it.title ?: "Nova Mensagem", it.body ?: "")
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "chat_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Chat Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Store token in user profile in RTDB if needed for targeting push
    }
}
