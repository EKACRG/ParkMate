package com.example.parkmate.background

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import android.content.Context
import androidx.work.WorkerParameters
import com.example.parkmate.MainActivity
import com.example.parkmate.ParkMateApplication
import com.example.parkmate.data.local.entity.ParkType
import com.example.parkmate.util.formatDuration
import java.time.Duration

// id fisso: è un riepilogo che evolve, non una notifica per evento come i ticket
private const val REMINDER_NOTIFICATION_ID = 1

// letto da MainActivity: dice al NavHost di saltare sulla Mappa invece che su Veicoli
const val EXTRA_OPEN_MAP = "open_map"

class ActiveSessionReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val application = applicationContext as ParkMateApplication
        val sessions = application.parkingSessionRepository.getActiveSessionsSnapshot()

        if (sessions.isEmpty()) {
            NotificationManagerCompat.from(applicationContext).cancel(REMINDER_NOTIFICATION_ID)
            return Result.success()
        }

        val granted = ContextCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val now = System.currentTimeMillis()
        val lines = sessions.map { session ->
            val vehicleName = checkNotNull(application.vehicleRepository.getById(session.vehicleId)) {
                "Vehicle ${session.vehicleId} must exist for an active ParkingSession (FK RESTRICT)"
            }.name
            val elapsed = Duration.ofMillis((now - session.startTime).coerceAtLeast(0))
            "$vehicleName (${session.type.shortLabel()}): ${formatDuration(elapsed)}"
        }

        // il tap riapre solo l'app: un riepilogo con più soste non ha un'unica sessione su cui puntare
        val contentIntent = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java).putExtra(EXTRA_OPEN_MAP, true),
            PendingIntent.FLAG_IMMUTABLE
        )

        val style = NotificationCompat.InboxStyle()
        lines.forEach { style.addLine(it) }

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Soste in corso")
            .setContentText(lines.first())
            .setStyle(style)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(REMINDER_NOTIFICATION_ID, notification)
        return Result.success()
    }
}

private fun ParkType.shortLabel(): String = when (this) {
    ParkType.FREE -> "libera"
    ParkType.HOURLY_PAID -> "oraria"
    ParkType.FIXED_TICKET -> "ticket"
}
