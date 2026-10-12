package com.example.parkmate.background

import android.content.Context
import androidx.core.content.edit

// preferenze condivise delle impostazioni di notifica: lo Step 2 legge l'anticipo, la pagina
// impostazioni (Step 4) scriverà qui con la stessa chiave, senza duplicarla altrove
private const val PREFS_NAME = "parkmate_notification_settings"
private const val KEY_EXPIRY_WARNING_MINUTES = "expiry_warning_minutes"
private const val KEY_REMINDER_INTERVAL_MINUTES = "reminder_interval_minutes"

const val DEFAULT_EXPIRY_WARNING_MINUTES = 10
const val DEFAULT_REMINDER_INTERVAL_MINUTES = 30

// 15 minuti è il minimo imposto da WorkManager per un PeriodicWorkRequest. Pubblico: la
// pagina impostazioni lo mostra come limite in UI, non lo duplica come valore scritto a mano
const val MIN_REMINDER_INTERVAL_MINUTES = 15

fun getExpiryWarningMinutes(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_EXPIRY_WARNING_MINUTES, DEFAULT_EXPIRY_WARNING_MINUTES)
}

fun getReminderIntervalMinutes(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val minutes = prefs.getInt(KEY_REMINDER_INTERVAL_MINUTES, DEFAULT_REMINDER_INTERVAL_MINUTES)
    return minutes.coerceAtLeast(MIN_REMINDER_INTERVAL_MINUTES)
}

fun setExpiryWarningMinutes(context: Context, minutes: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putInt(KEY_EXPIRY_WARNING_MINUTES, minutes)
    }
}

fun setReminderIntervalMinutes(context: Context, minutes: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putInt(KEY_REMINDER_INTERVAL_MINUTES, minutes)
    }
}
