package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.DateUtils
import com.example.data.LifeOsDatabase
import com.example.data.LifeOsRepository
import com.example.data.NewsAndVideoRepository
import com.example.data.NotificationCategory
import com.example.data.NotificationLogEntity
import com.example.data.NotificationPreferencesEntity
import com.example.data.TaskStatus
import com.example.data.TransactionType
import com.example.domain.MotivationQuotesProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Pure Kotlin notification policy calculations for unit-testable quiet hours,
 * completion-aware suppression, and daily summary formatting.
 */
object NotificationPolicyEngine {
    fun isWithinQuietHours(
        currentHour: Int,
        quietStartHour: Int,
        quietEndHour: Int,
        quietHoursEnabled: Boolean
    ): Boolean {
        if (!quietHoursEnabled) return false
        val h = currentHour.coerceIn(0, 23)
        val start = quietStartHour.coerceIn(0, 23)
        val end = quietEndHour.coerceIn(0, 23)
        if (start == end) return false
        return if (start > end) {
            h >= start || h < end
        } else {
            h in start until end
        }
    }

    fun shouldSendWaterReminder(todayWaterMl: Int, dailyTargetMl: Int): Boolean {
        return dailyTargetMl > 0 && todayWaterMl < dailyTargetMl
    }

    fun computeInitialDelayMillis(targetHour: Int, targetMinute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val next = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
            set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis + 60_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (next.timeInMillis - cal.timeInMillis).coerceAtLeast(60_000L)
    }

    fun formatDailySummary(
        tasksDone: Int,
        tasksTotal: Int,
        waterMl: Int,
        waterTargetMl: Int,
        steps: Int,
        stepsTarget: Int,
        todayExpense: Double,
        upcomingInterviews: Int
    ): String {
        val parts = mutableListOf(
            "Tasks $tasksDone/$tasksTotal",
            "Water ${waterMl}/${waterTargetMl} ml",
            "Steps $steps/$stepsTarget",
            "Spent ₹${todayExpense.toInt()}"
        )
        if (upcomingInterviews > 0) {
            parts += "$upcomingInterviews interview(s) ahead"
        }
        return parts.joinToString(" • ")
    }
}

object LifeOsNotificationCenter {
    const val EXTRA_DEEP_LINK_ROUTE = "lifeos_deep_link_route"
    const val EXTRA_DEEP_LINK_ARG = "lifeos_deep_link_arg"
    const val EXTRA_WATER_ML = "lifeos_water_quick_ml"
    const val EXTRA_NOTIFICATION_ID = "lifeos_notification_id"
    const val ACTION_QUICK_ADD_WATER = "com.lifeos.productivity.ACTION_QUICK_ADD_WATER"

    private const val WORK_WATER = "lifeos_work_hourly_water"
    private const val WORK_NEWS = "lifeos_work_hourly_news"
    private const val WORK_VIDEOS = "lifeos_work_hourly_videos"
    private const val WORK_SMART_GOALS = "lifeos_work_smart_goals"
    private const val WORK_DAILY_SUMMARY = "lifeos_work_daily_summary"
    const val KEY_UID = "uid"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun isDoNotDisturbActive(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        val filter = nm.currentInterruptionFilter
        return filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
            filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    fun ensureChannels(context: Context, prefs: NotificationPreferencesEntity?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val soundOn = prefs?.soundEnabled ?: true
        val vibrateOn = prefs?.vibrationEnabled ?: true

        NotificationCategory.entries.forEach { cat ->
            val importance = if (soundOn) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(cat.channelId, cat.label, importance).apply {
                description = cat.description
                enableVibration(vibrateOn)
            }
            nm.createNotificationChannel(channel)
        }
    }

    suspend fun postSmartNotification(
        context: Context,
        uid: String,
        category: NotificationCategory,
        title: String,
        message: String,
        deepLinkRoute: String = category.defaultDeepLink,
        deepLinkArg: String = "",
        dedupeKey: String = "",
        isPreview: Boolean = false
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        val db = LifeOsDatabase.getInstance(context.applicationContext)
        val v3Dao = db.v3Dao()
        val prefs = v3Dao.getNotificationPrefsOnce(uid) ?: NotificationPreferencesEntity(userId = uid)

        if (!isPreview) {
            if (!prefs.isCategoryEnabled(category)) return false
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (NotificationPolicyEngine.isWithinQuietHours(
                    currentHour, prefs.quietStartHour, prefs.quietEndHour, prefs.quietHoursEnabled
                )
            ) {
                return false
            }
            if (isDoNotDisturbActive(context)) return false
            if (dedupeKey.isNotBlank() && v3Dao.countByDedupeKey(uid, dedupeKey) > 0) {
                return false
            }
        }

        ensureChannels(context, prefs)
        val notificationId = (category.id + "_" + (dedupeKey.ifBlank { System.currentTimeMillis().toString() })).hashCode()

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_ROUTE, deepLinkRoute)
            if (deepLinkArg.isNotBlank()) {
                putExtra(EXTRA_DEEP_LINK_ARG, deepLinkArg)
            }
        }
        val tapPending = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, category.channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(tapPending)
            .setPriority(if (prefs.soundEnabled) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)

        if (!prefs.soundEnabled) {
            builder.setSilent(true)
        }

        // Explicit user-confirmed quick actions for Water notifications (+200 ml / +250 ml)
        if (category == NotificationCategory.WATER) {
            listOf(200, 250).forEachIndexed { idx, ml ->
                val actionIntent = Intent(context, WaterQuickAddReceiver::class.java).apply {
                    action = ACTION_QUICK_ADD_WATER
                    putExtra(EXTRA_WATER_ML, ml)
                    putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(KEY_UID, uid)
                }
                val actionPending = PendingIntent.getBroadcast(
                    context,
                    notificationId + idx + 11,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, "+$ml ml", actionPending)
            }
        }

        val posted = runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        }.getOrDefault(false)

        if (posted) {
            v3Dao.insertNotificationLog(
                NotificationLogEntity(
                    userId = uid,
                    categoryId = category.id,
                    title = title,
                    message = message,
                    deepLinkRoute = deepLinkRoute,
                    deepLinkArg = deepLinkArg,
                    dedupeKey = if (isPreview) "" else dedupeKey
                )
            )
        }
        return posted
    }

    fun syncAutomationSchedules(
        context: Context,
        uid: String,
        prefs: NotificationPreferencesEntity
    ) {
        val wm = WorkManager.getInstance(context.applicationContext)
        ensureChannels(context, prefs)

        if (uid.isBlank() || prefs.pauseAll) {
            wm.cancelUniqueWork(WORK_WATER)
            wm.cancelUniqueWork(WORK_NEWS)
            wm.cancelUniqueWork(WORK_VIDEOS)
            wm.cancelUniqueWork(WORK_SMART_GOALS)
            wm.cancelUniqueWork(WORK_DAILY_SUMMARY)
            return
        }

        val input = Data.Builder().putString(KEY_UID, uid).build()

        // 1. Hourly Water Reminder
        if (prefs.waterEnabled) {
            val interval = prefs.waterIntervalMinutes.coerceAtLeast(15).toLong()
            val req = PeriodicWorkRequestBuilder<HourlyWaterWorker>(interval, TimeUnit.MINUTES)
                .setInputData(input)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_WATER, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK_WATER)
        }

        // 2. Hourly IT & Technology News
        if (prefs.techNewsEnabled) {
            val interval = prefs.newsIntervalMinutes.coerceAtLeast(60).toLong()
            val netConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val req = PeriodicWorkRequestBuilder<HourlyNewsWorker>(interval, TimeUnit.MINUTES)
                .setConstraints(netConstraints)
                .setInputData(input)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NEWS, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK_NEWS)
        }

        // 3. Hourly Educational YouTube Videos
        if (prefs.youtubeVideosEnabled) {
            val interval = prefs.videoIntervalMinutes.coerceAtLeast(60).toLong()
            val netConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val req = PeriodicWorkRequestBuilder<HourlyYouTubeWorker>(interval, TimeUnit.MINUTES)
                .setConstraints(netConstraints)
                .setInputData(input)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_VIDEOS, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK_VIDEOS)
        }

        // 4. Smart Goals / Tasks / Habits / Steps / Classes / Interviews / Learning / Finance / Motivation
        val anySmartEnabled = prefs.stepsEnabled || prefs.goalsEnabled || prefs.tasksEnabled ||
            prefs.habitsEnabled || prefs.learningEnabled || prefs.classesEnabled ||
            prefs.interviewsEnabled || prefs.financeEnabled || prefs.motivationEnabled
        if (anySmartEnabled) {
            val req = PeriodicWorkRequestBuilder<SmartGoalsReminderWorker>(2, TimeUnit.HOURS)
                .setInputData(input)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_SMART_GOALS, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK_SMART_GOALS)
        }

        // 5. Smart Daily Summary
        if (prefs.dailySummaryEnabled) {
            val initialDelay = NotificationPolicyEngine.computeInitialDelayMillis(
                prefs.dailySummaryHour,
                prefs.dailySummaryMinute
            )
            val req = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .build()
            wm.enqueueUniquePeriodicWork(WORK_DAILY_SUMMARY, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK_DAILY_SUMMARY)
        }
    }
}

class HourlyWaterWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString(LifeOsNotificationCenter.KEY_UID)
            ?: Firebase.auth.currentUser?.uid
            ?: return Result.success()
        val db = LifeOsDatabase.getInstance(applicationContext)
        val prefsRepo = com.example.data.PreferencesRepository(applicationContext)
        val today = DateUtils.todayIso()
        val todayWater = db.dao().observeWaterLogs(uid).first()
            .filter { it.dateIso == today }
            .sumOf { it.amountMl }
        val targetMl = prefsRepo.observePreferences(uid).first().waterTargetMl.coerceAtLeast(250)
        if (!NotificationPolicyEngine.shouldSendWaterReminder(todayWater, targetMl)) {
            return Result.success()
        }
        val remaining = (targetMl - todayWater).coerceAtLeast(0)
        val hourBucket = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        LifeOsNotificationCenter.postSmartNotification(
            context = applicationContext,
            uid = uid,
            category = NotificationCategory.WATER,
            title = "Hydration Check • ${todayWater}/${targetMl} ml",
            message = "Stay sharp and hydrated — $remaining ml left to hit today's goal. Tap +200 ml or +250 ml below once you drink.",
            deepLinkRoute = "water",
            dedupeKey = "water_${today}_$hourBucket"
        )
        return Result.success()
    }
}

class HourlyNewsWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString(LifeOsNotificationCenter.KEY_UID)
            ?: Firebase.auth.currentUser?.uid
            ?: return Result.success()
        val db = LifeOsDatabase.getInstance(applicationContext)
        val v3Dao = db.v3Dao()
        val prefs = v3Dao.getNotificationPrefsOnce(uid) ?: return Result.success()
        if (!prefs.isCategoryEnabled(NotificationCategory.TECH_NEWS)) return Result.success()

        val articles = NewsAndVideoRepository.fetchLiveTechNews().getOrNull().orEmpty()
        val fresh = articles.firstOrNull { it.url.isNotBlank() && it.url != prefs.lastSeenNewsUrl } ?: return Result.success()

        val sent = LifeOsNotificationCenter.postSmartNotification(
            context = applicationContext,
            uid = uid,
            category = NotificationCategory.TECH_NEWS,
            title = "Tech Update • ${fresh.source}",
            message = fresh.title,
            deepLinkRoute = "learn",
            deepLinkArg = "news",
            dedupeKey = "news_${fresh.url.hashCode()}"
        )
        if (sent) {
            v3Dao.upsertNotificationPrefs(prefs.copy(lastSeenNewsUrl = fresh.url, updatedAtMs = System.currentTimeMillis()))
        }
        return Result.success()
    }
}

class HourlyYouTubeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString(LifeOsNotificationCenter.KEY_UID)
            ?: Firebase.auth.currentUser?.uid
            ?: return Result.success()
        val db = LifeOsDatabase.getInstance(applicationContext)
        val v3Dao = db.v3Dao()
        val prefs = v3Dao.getNotificationPrefsOnce(uid) ?: return Result.success()
        if (!prefs.isCategoryEnabled(NotificationCategory.YOUTUBE_VIDEOS)) return Result.success()

        if (!NewsAndVideoRepository.hasYouTubeApiKey()) return Result.success()
        val videos = NewsAndVideoRepository.searchYouTubeVideos(
            query = "Software Engineering",
            category = "Artificial Intelligence",
            language = "English"
        ).getOrNull().orEmpty()
        val fresh = videos.firstOrNull { it.watchUrl.isNotBlank() && it.watchUrl != prefs.lastSeenVideoUrl }
            ?: return Result.success()

        // Encode details safely so tapping opens LIFE OS in-app Video Detail Screen (NEVER auto-launches YouTube!)
        val payload = com.example.ui.screens.VideoDetailPayload(
            title = fresh.title,
            channel = fresh.channelTitle,
            publishedDate = fresh.publishedAt,
            url = fresh.watchUrl,
            thumbnailUrl = fresh.thumbnailUrl,
            category = fresh.category,
            language = fresh.language
        ).encode()

        val sent = LifeOsNotificationCenter.postSmartNotification(
            context = applicationContext,
            uid = uid,
            category = NotificationCategory.YOUTUBE_VIDEOS,
            title = "New Learning Video • ${fresh.channelTitle}",
            message = fresh.title,
            deepLinkRoute = "video_detail",
            deepLinkArg = payload,
            dedupeKey = "video_${fresh.watchUrl.hashCode()}"
        )
        if (sent) {
            v3Dao.upsertNotificationPrefs(prefs.copy(lastSeenVideoUrl = fresh.watchUrl, updatedAtMs = System.currentTimeMillis()))
        }
        return Result.success()
    }
}

class SmartGoalsReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString(LifeOsNotificationCenter.KEY_UID)
            ?: Firebase.auth.currentUser?.uid
            ?: return Result.success()
        val db = LifeOsDatabase.getInstance(applicationContext)
        val dao = db.dao()
        val v3Dao = db.v3Dao()
        val today = DateUtils.todayIso()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Morning Motivation Quote (once daily between 7 AM - 11 AM)
        if (hour in 7..11) {
            val quote = MotivationQuotesProvider.quoteForDate(today)
            LifeOsNotificationCenter.postSmartNotification(
                context = applicationContext,
                uid = uid,
                category = NotificationCategory.MOTIVATION,
                title = "Daily Motivation • ${quote.author}",
                message = "\"${quote.text}\"",
                deepLinkRoute = "motivation_goal",
                dedupeKey = "motivation_$today"
            )
        }

        // Pending Tasks Check (skip if all completed!)
        val pendingTasks = dao.observeTasks(uid).first()
            .filter { it.dueDateIso == today && it.status != TaskStatus.COMPLETED.name }
        if (pendingTasks.isNotEmpty() && hour in 10..19) {
            LifeOsNotificationCenter.postSmartNotification(
                context = applicationContext,
                uid = uid,
                category = NotificationCategory.TASKS,
                title = "${pendingTasks.size} Task(s) Remaining Today",
                message = "Next up: ${pendingTasks.first().title}",
                deepLinkRoute = "plan",
                dedupeKey = "tasks_${today}_${hour / 4}"
            )
        }

        // Unfinished Habits Check (skip if all completed!)
        val habits = dao.observeHabits(uid).first()
        val todayHabitLogs = dao.observeHabitLogs(uid).first().filter { it.dateIso == today && it.completed }
        val unfinishedHabits = habits.count { h -> todayHabitLogs.none { it.habitId == h.id } }
        if (unfinishedHabits > 0 && hour in 16..20) {
            LifeOsNotificationCenter.postSmartNotification(
                context = applicationContext,
                uid = uid,
                category = NotificationCategory.HABITS,
                title = "Protect Your Streak",
                message = "$unfinishedHabits habit(s) still open today. Check in to keep your streak alive.",
                deepLinkRoute = "track",
                dedupeKey = "habits_$today"
            )
        }

        // Walking / Steps Check (afternoon nudge if below half target)
        val todaySteps = dao.observeStepLogs(uid).first().firstOrNull { it.dateIso == today }?.steps ?: 0
        if (hour in 15..19 && todaySteps < 4000) {
            LifeOsNotificationCenter.postSmartNotification(
                context = applicationContext,
                uid = uid,
                category = NotificationCategory.STEPS,
                title = "Time for a Quick Walk",
                message = "You're at $todaySteps steps today. A brisk 15-minute walk boosts focus and energy.",
                deepLinkRoute = "steps",
                dedupeKey = "steps_$today"
            )
        }

        // Upcoming Interview Check
        val interviewsToday = v3Dao.observeJobApplications(uid).first()
            .filter { it.interviewDateIso == today }
        if (interviewsToday.isNotEmpty()) {
            val first = interviewsToday.first()
            LifeOsNotificationCenter.postSmartNotification(
                context = applicationContext,
                uid = uid,
                category = NotificationCategory.INTERVIEWS,
                title = "Interview Today • ${first.company}",
                message = "${first.role} ${first.interviewTime.ifBlank { "" }} — tap to review your prep checklist.",
                deepLinkRoute = "jobs",
                dedupeKey = "interview_${first.id}_$today"
            )
        }

        return Result.success()
    }
}

class DailySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString(LifeOsNotificationCenter.KEY_UID)
            ?: Firebase.auth.currentUser?.uid
            ?: return Result.success()
        val db = LifeOsDatabase.getInstance(applicationContext)
        val dao = db.dao()
        val v3Dao = db.v3Dao()
        val today = DateUtils.todayIso()

        val tasks = dao.observeTasks(uid).first().filter { it.dueDateIso == today }
        val doneTasks = tasks.count { it.status == TaskStatus.COMPLETED.name }
        val waterMl = dao.observeWaterLogs(uid).first().filter { it.dateIso == today }.sumOf { it.amountMl }
        val steps = dao.observeStepLogs(uid).first().firstOrNull { it.dateIso == today }?.steps ?: 0
        val expenseToday = v3Dao.observeTransactions(uid).first()
            .filter { it.dateIso == today && it.type == TransactionType.EXPENSE.name }
            .sumOf { it.amount }
        val upcomingInterviews = v3Dao.observeJobApplications(uid).first()
            .count { it.interviewDateIso.isNotBlank() && it.interviewDateIso >= today }

        val summary = NotificationPolicyEngine.formatDailySummary(
            tasksDone = doneTasks,
            tasksTotal = tasks.size,
            waterMl = waterMl,
            waterTargetMl = 2500,
            steps = steps,
            stepsTarget = 8000,
            todayExpense = expenseToday,
            upcomingInterviews = upcomingInterviews
        )

        LifeOsNotificationCenter.postSmartNotification(
            context = applicationContext,
            uid = uid,
            category = NotificationCategory.DAILY_SUMMARY,
            title = "LIFE OS Daily Summary",
            message = summary,
            deepLinkRoute = "home",
            dedupeKey = "summary_$today"
        )
        return Result.success()
    }
}

/**
 * Handles explicit user tap on "+200 ml" or "+250 ml" action buttons in Water notifications.
 * Never adds water automatically — executes strictly on user tap.
 */
class WaterQuickAddReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LifeOsNotificationCenter.ACTION_QUICK_ADD_WATER) return
        val ml = intent.getIntExtra(LifeOsNotificationCenter.EXTRA_WATER_ML, 0)
        if (ml <= 0) return
        val uid = intent.getStringExtra(LifeOsNotificationCenter.KEY_UID)
            ?: Firebase.auth.currentUser?.uid
            ?: return
        val notifId = intent.getIntExtra(LifeOsNotificationCenter.EXTRA_NOTIFICATION_ID, -1)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = LifeOsRepository(context.applicationContext)
                repo.logWater(uid, DateUtils.todayIso(), ml)
                if (notifId != -1) {
                    NotificationManagerCompat.from(context).cancel(notifId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * Re-registers WorkManager notification schedules after device reboot if signed in.
 */
class LifeOsBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val uid = Firebase.auth.currentUser?.uid ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = LifeOsDatabase.getInstance(context.applicationContext)
                val prefs = db.v3Dao().getNotificationPrefsOnce(uid) ?: NotificationPreferencesEntity(userId = uid)
                LifeOsNotificationCenter.syncAutomationSchedules(context.applicationContext, uid, prefs)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
