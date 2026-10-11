package com.example.parkmate.util

import java.time.Duration

// "Xh Ym". toMinutesPart() richiederebbe l'API 31 (minSdk è 26)
fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes() % 60
    return "${duration.toHours()}h ${minutes}m"
}
