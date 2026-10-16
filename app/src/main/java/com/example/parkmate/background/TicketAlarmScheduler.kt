package com.example.parkmate.background

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build

// chiavi degli extra nell'intent che arriva al TicketAlarmReceiver
const val EXTRA_SESSION_ID = "session_id"
const val EXTRA_VEHICLE_NAME = "vehicle_name"
const val EXTRA_ALARM_KIND = "alarm_kind"
const val ALARM_KIND_WARNING = "warning"
const val ALARM_KIND_EXACT = "exact"

class TicketAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun currentExpiryWarningMinutes(): Int = getExpiryWarningMinutes(context)

    fun updateExpiryWarningMinutes(minutes: Int) {
        setExpiryWarningMinutes(context, minutes)
    }

    // programma i due avvisi: quello anticipato e quello esatto. Restituisce se quello
    // anticipato è stato effettivamente programmato (false se la finestra è già passata)
    fun schedule(sessionId: Long, vehicleName: String, expiryTime: Long): Boolean {
        val now = System.currentTimeMillis()
        val warningMinutes = getExpiryWarningMinutes(context)
        val warningTime = expiryTime - warningMinutes * 60_000L
        val warningScheduled = warningTime > now

        if (warningScheduled) {
            scheduleOne(sessionId, vehicleName, warningTime, ALARM_KIND_WARNING)
        }
        // un orario di scadenza già passato farebbe scattare l'alarm esatto subito dopo averlo programmato
        if (expiryTime > now) {
            scheduleOne(sessionId, vehicleName, expiryTime, ALARM_KIND_EXACT)
        }
        return warningScheduled
    }

    // cancella entrambi gli avvisi di sessionId. Non fa nulla se non erano programmati
    fun cancel(sessionId: Long) {
        alarmManager.cancel(pendingIntentFor(sessionId, "", ALARM_KIND_WARNING))
        alarmManager.cancel(pendingIntentFor(sessionId, "", ALARM_KIND_EXACT))
    }

    private fun scheduleOne(sessionId: Long, vehicleName: String, triggerAt: Long, kind: String) {
        val pendingIntent = pendingIntentFor(sessionId, vehicleName, kind)
        if (canScheduleExact()) {
            // sopravvive alla Doze mode
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun pendingIntentFor(sessionId: Long, vehicleName: String, kind: String): android.app.PendingIntent {
        val intent = Intent(context, TicketAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_VEHICLE_NAME, vehicleName)
            putExtra(EXTRA_ALARM_KIND, kind)
        }

        // FLAG_UPDATE_CURRENT: se questo request code è già stato usato (es. riprogrammazione),
        // aggiorna gli extra del PendingIntent esistente invece di lasciare quelli vecchi.
        // FLAG_IMMUTABLE: obbligatorio da API 31 per i PendingIntent che il destinatario non deve poter modificare
        return android.app.PendingIntent.getBroadcast(
            context,
            requestCodeFor(sessionId, kind),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }
}

// il request code di un PendingIntent è int: *2 e *2+1 danno due codici
// diversi per la stessa session (anticipo ed esatto)
private fun requestCodeFor(sessionId: Long, kind: String): Int {
    val base = sessionId.toInt() * 2
    return if (kind == ALARM_KIND_WARNING) base else base + 1
}
