package com.example.parkmate.background

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.parkmate.MainActivity

// extra letto da MainActivity al tap sulla notifica
const val EXTRA_TICKET_SESSION_ID = "ticket_session_id"

/**
 * Riceve i due avvisi di scadenza programmati da TicketAlarmScheduler e posta la notifica
 * sul canale expiry. Deve poter arrivare anche se la nostra app non è in esecuzione in quel
 * momento, e solo un BroadcastReceiver lo garantisce
 */
class TicketAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val vehicleName = intent.getStringExtra(EXTRA_VEHICLE_NAME) ?: "Veicolo"
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        val isExact = intent.getStringExtra(EXTRA_ALARM_KIND) == ALARM_KIND_EXACT

        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) return

        // il tap apre la Mappa con la scheda di questa sosta già aperta, se ancora attiva
        val contentIntent = PendingIntent.getActivity(
            context,
            sessionId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_MAP, true)
                putExtra(EXTRA_TICKET_SESSION_ID, sessionId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_EXPIRY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isExact) "Ticket scaduto" else "Il ticket sta per scadere")
            .setContentText(vehicleName)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        // stesso id per i due avvisi della stessa session: il secondo (esatto) sostituisce il primo (anticipato)
        // nella tendina, invece di accumularsi come notifica separata
        NotificationManagerCompat.from(context).notify(sessionId.toInt(), notification)
    }
}
