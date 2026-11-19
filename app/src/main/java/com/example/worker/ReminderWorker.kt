package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.DateUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Life OS Reminder"
        val module = inputData.getString(KEY_MODULE) ?: "LIFE OS"
        val quietStart = inputData.getInt(KEY_QUIET_START, 22)
        val quietEnd = inputData.getInt(KEY_QUIET_END, 7)

        val currentHour = Calendar.getInstance(DateUtils.IST_TIMEZONE).get(Calendar.HOUR_OF_DAY)
        val inQuietHours = if (quietStart > quietEnd) {
            currentHour >= quietStart || currentHour < quietEnd
        } else {
            currentHour in quietStart until quietEnd
        }
        if (inQuietHours) return Result.success()

        ensureChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("$module • Build Better. Live Better.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(title.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "life_os_reminders"
        const val KEY_TITLE = "title"
        const val KEY_MODULE = "module"
        const val KEY_QUIET_START = "quiet_start"
        const val KEY_QUIET_END = "quiet_end"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Life OS Smart Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Scheduled reminders for tasks, habits, focus, and hydration"
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        fun scheduleReminder(
            context: Context,
            reminderId: String,
            title: String,
            moduleType: String,
            triggerEpochMs: Long,
            quietStart: Int,
            quietEnd: Int
        ) {
            val delayMs = (triggerEpochMs - System.currentTimeMillis()).coerceAtLeast(1000L)
            val data = Data.Builder()
                .putString(KEY_TITLE, title)
                .putString(KEY_MODULE, moduleType)
                .putInt(KEY_QUIET_START, quietStart)
                .putInt(KEY_QUIET_END, quietEnd)
                .build()

            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_$reminderId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelReminder(context: Context, reminderId: String) {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$reminderId")
        }
    }
}
