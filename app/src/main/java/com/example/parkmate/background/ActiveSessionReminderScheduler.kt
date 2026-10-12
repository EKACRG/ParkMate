package com.example.parkmate.background

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val REMINDER_WORK_NAME = "active_session_reminder"

// stesso ruolo di TicketAlarmScheduler: sa come programmare il Worker, non sa cosa sia una sessione
class ActiveSessionReminderScheduler(private val context: Context) {

    fun currentIntervalMinutes(): Int = getReminderIntervalMinutes(context)

    fun enqueue(policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP) {
        val request = PeriodicWorkRequestBuilder<ActiveSessionReminderWorker>(
            currentIntervalMinutes().toLong(), TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(REMINDER_WORK_NAME, policy, request)
    }

    // salva il nuovo intervallo e aggiorna il lavoro già pianificato, invece di ignorarlo come KEEP
    fun updateInterval(minutes: Int) {
        setReminderIntervalMinutes(context, minutes)
        enqueue(ExistingPeriodicWorkPolicy.UPDATE)
    }
}
