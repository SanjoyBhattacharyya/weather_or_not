package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

class PlanReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventTitle = intent.getStringExtra("event_title") ?: "Scheduled Plan"
        val eventId = intent.getIntExtra("event_id", 0)
        val eventTune = intent.getStringExtra("event_tune") ?: "Default"

        // Trigger custom audio feedback based on selection
        playCustomTune(eventTune)

        // Show Notification
        showNotification(context, eventTitle, eventId, eventTune)
    }

    private fun playCustomTune(tune: String) {
        try {
            val toneType = when (tune) {
                "Cosmic Pip" -> ToneGenerator.TONE_CDMA_PIP
                "Echo Beep" -> ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE
                "High Alert" -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                "Wave Chime" -> ToneGenerator.TONE_CDMA_ABBR_ALERT
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            val valVolume = 85
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, valVolume)
            
            // Play dynamic sequence tones mapping back to the selected tone label
            when (tune) {
                "Cosmic Pip" -> {
                    toneGen.startTone(toneType, 150)
                }
                "Echo Beep" -> {
                    toneGen.startTone(toneType, 220)
                }
                "High Alert" -> {
                    toneGen.startTone(toneType, 400)
                }
                "Wave Chime" -> {
                    toneGen.startTone(toneType, 100)
                }
                else -> {
                    toneGen.startTone(toneType, 300)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotification(context: Context, title: String, id: Int, tune: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "plan_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Plan Reminders"
            val descriptionText = "Notifications for your planned outdoor schedules"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, id, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Weather or Not Reminder! ⏰")
            .setContentText("It's time for: $title (Tune: $tune)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(id, builder.build())
    }

    companion object {
        fun schedule(
            context: Context, 
            id: Int, 
            title: String, 
            dayOffset: Int, 
            hour: Int, 
            minute: Int, 
            tune: String
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PlanReminderReceiver::class.java).apply {
                putExtra("event_title", title)
                putExtra("event_id", id)
                putExtra("event_tune", tune)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                if (dayOffset == 1) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // Avoid immediate triggers on past moments
                if (timeInMillis <= System.currentTimeMillis() && dayOffset == 0) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        fun cancel(context: Context, id: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PlanReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
