package com.example.parkmate.ui.stats

import com.example.parkmate.data.local.entity.ParkingSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// il grafico raggruppa sempre per giorno. Cambia solo quanti giorni indietro si guarda.
// Il filtro periodo rende interattiva anche la heat map, non solo pan/zoom su dati fissi
enum class StatsPeriod(val label: String) {
    LAST_7_DAYS("Ultimi 7 giorni"),
    LAST_30_DAYS("Ultimi 30 giorni"),
    ALL("Tutto")
}

// giorno di inizio nel fuso del telefono, come nel filtro data dello storico
private fun startDay(session: ParkingSession): LocalDate =
    Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalDate()

fun List<ParkingSession>.filterByPeriod(period: StatsPeriod): List<ParkingSession> {
    if (period == StatsPeriod.ALL) return this
    val days = if (period == StatsPeriod.LAST_7_DAYS) 7 else 30
    val earliestDay = LocalDate.now().minusDays((days - 1).toLong())
    return filter { startDay(it) >= earliestDay }
}

// tutti i giorni del periodo, anche quelli senza soste,
// altrimenti l'asse si comprime e giorni lontani finiscono vicini.
fun List<ParkingSession>.dayBucketsForPeriod(period: StatsPeriod): List<LocalDate> {
    val today = LocalDate.now()
    val firstDay = when (period) {
        StatsPeriod.LAST_7_DAYS -> today.minusDays(6)
        StatsPeriod.LAST_30_DAYS -> today.minusDays(29)
        StatsPeriod.ALL -> minOfOrNull(::startDay) ?: today
    }
    return generateSequence(firstDay) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
}
