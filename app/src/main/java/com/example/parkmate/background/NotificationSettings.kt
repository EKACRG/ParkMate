package com.example.parkmate.background

import android.content.Context

// preferenze condivise delle impostazioni di notifica: lo Step 2 legge l'anticipo, la pagina
// impostazioni (Step 4) scriverà qui con la stessa chiave, senza duplicarla altrove
private const val PREFS_NAME = "parkmate_notification_settings"
private const val KEY_EXPIRY_WARNING_MINUTES = "expiry_warning_minutes"
private const val KEY_REMINDER_INTERVAL_MINUTES = "reminder_interval_minutes"

const val DEFAULT_EXPIRY_WARNING_MINUTES = 10
const val DEFAULT_REMINDER_INTERVAL_MINUTES = 30

// 15 minuti è il minimo imposto da WorkManager per un PeriodicWorkRequest
private const val MIN_REMINDER_INTERVAL_MINUTES = 15

fun getExpiryWarningMinutes(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_EXPIRY_WARNING_MINUTES, DEFAULT_EXPIRY_WARNING_MINUTES)
}

fun getReminderIntervalMinutes(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val minutes = prefs.getInt(KEY_REMINDER_INTERVAL_MINUTES, DEFAULT_REMINDER_INTERVAL_MINUTES)
    return minutes.coerceAtLeast(MIN_REMINDER_INTERVAL_MINUTES)
}
