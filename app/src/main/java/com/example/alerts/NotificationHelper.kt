package com.example.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

  private val notificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Real-time water tank milestone alerts and motor safeguards"
        enableLights(true)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 300, 150, 300)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun showWaterMilestoneNotification(
    id: Int,
    gujaratiTitle: String,
    englishSubtitle: String,
    levelPercent: Int? = null,
    isUrgent: Boolean = false
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      )
      if (permission != PackageManager.PERMISSION_GRANTED) {
        return
      }
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      id,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val soundUri = RingtoneManager.getDefaultUri(
      if (isUrgent) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION
    )

    val contentText = if (levelPercent != null) {
      "$englishSubtitle • Live: $levelPercent%"
    } else {
      englishSubtitle
    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_water_alert)
      .setContentTitle(gujaratiTitle)
      .setContentText(contentText)
      .setStyle(NotificationCompat.BigTextStyle().bigText("$gujaratiTitle\n$contentText"))
      .setPriority(if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
      .setCategory(if (isUrgent) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
      .setAutoCancel(true)
      .setSound(soundUri)
      .setContentIntent(pendingIntent)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (isUrgent) {
      builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
    }

    notificationManager.notify(id, builder.build())
  }

  companion object {
    const val CHANNEL_ID = "water_level_alerts"
    const val CHANNEL_NAME = "HydroSense Water Alerts"

    const val NOTIFICATION_ID_25 = 2025
    const val NOTIFICATION_ID_50 = 2050
    const val NOTIFICATION_ID_75 = 2075
    const val NOTIFICATION_ID_95 = 2095
    const val NOTIFICATION_ID_TARGET = 2080
    const val NOTIFICATION_ID_TEST = 2099
  }
}
