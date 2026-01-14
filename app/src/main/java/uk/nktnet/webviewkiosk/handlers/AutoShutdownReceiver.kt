package uk.nktnet.webviewkiosk.handlers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import uk.nktnet.webviewkiosk.config.UserSettings
import java.util.Calendar

class AutoShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AutoShutdownReceiver", "Auto-shutdown triggered")
        
        // Request device to go to sleep (turn off screen)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            // Send the device to sleep mode (turn off screen)
            @Suppress("DEPRECATION")
            powerManager.goToSleep(android.os.SystemClock.uptimeMillis())
            
        } catch (e: Exception) {
            Log.e("AutoShutdownReceiver", "Failed to shutdown: ${e.message}")
        }
        
        // Reschedule for the next day
        scheduleAutoShutdown(context)
    }
    
    companion object {
        private const val ACTION_AUTO_SHUTDOWN = "uk.nktnet.webviewkiosk.AUTO_SHUTDOWN"
        
        fun scheduleAutoShutdown(context: Context) {
            val userSettings = UserSettings(context)
            val shutdownTime = userSettings.autoShutdownTime
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AutoShutdownReceiver::class.java).apply {
                action = ACTION_AUTO_SHUTDOWN
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Cancel any existing alarm
            alarmManager.cancel(pendingIntent)
            
            if (shutdownTime.isNullOrEmpty()) {
                Log.d("AutoShutdownReceiver", "Auto-shutdown disabled")
                return
            }
            
            // Parse the time (HH:MM format)
            val timeParts = shutdownTime.split(":")
            if (timeParts.size != 2) {
                Log.e("AutoShutdownReceiver", "Invalid time format: $shutdownTime")
                return
            }
            
            val hour = timeParts[0].toIntOrNull() ?: return
            val minute = timeParts[1].toIntOrNull() ?: return
            
            // Schedule for today or tomorrow depending on current time
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            Log.d("AutoShutdownReceiver", "Scheduling auto-shutdown for ${calendar.time}")
            
            // Set the alarm
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        
        fun cancelAutoShutdown(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AutoShutdownReceiver::class.java).apply {
                action = ACTION_AUTO_SHUTDOWN
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("AutoShutdownReceiver", "Auto-shutdown cancelled")
        }
    }
}
