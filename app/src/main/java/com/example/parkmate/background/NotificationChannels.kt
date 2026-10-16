package com.example.parkmate.background

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

// id dei canali come costanti: li useranno anche chi costruisce le notifiche vere (alarm, worker)
const val NOTIFICATION_CHANNEL_EXPIRY = "expiry"
const val NOTIFICATION_CHANNEL_REMINDERS = "reminders"
const val NOTIFICATION_CHANNEL_GEOFENCE = "geofence"

// crea i tre canali di notifica
fun createNotificationChannels(context: Context) {
    val manager = NotificationManagerCompat.from(context)

    val expiryChannel = NotificationChannelCompat.Builder(
        NOTIFICATION_CHANNEL_EXPIRY,
        NotificationManagerCompat.IMPORTANCE_HIGH
    )
        .setName("Scadenza ticket")
        .setDescription("Avvisi quando una sosta a biglietto fisso sta per scadere o è scaduta")
        .build()

    val remindersChannel = NotificationChannelCompat.Builder(
        NOTIFICATION_CHANNEL_REMINDERS,
        NotificationManagerCompat.IMPORTANCE_DEFAULT
    )
        .setName("Promemoria sosta")
        .setDescription("Riepilogo periodico delle soste attive")
        .build()

    val geofenceChannel = NotificationChannelCompat.Builder(
        NOTIFICATION_CHANNEL_GEOFENCE,
        NotificationManagerCompat.IMPORTANCE_HIGH
    )
        .setName("Ingresso o uscita da un luogo salvato")
        .setDescription("Proposta di avviare o terminare una sosta entrando o uscendo da un luogo salvato con geofence")
        .build()

    manager.createNotificationChannelsCompat(listOf(expiryChannel, remindersChannel, geofenceChannel))
}
