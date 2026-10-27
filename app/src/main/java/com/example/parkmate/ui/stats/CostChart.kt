package com.example.parkmate.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkmate.data.local.entity.ParkingSession
import com.example.parkmate.ui.components.EmptyState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

private val DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM")

private val BAR_WIDTH = 16.dp
private val BAR_SPACING = 8.dp
private val SLOT_WIDTH = BAR_WIDTH + BAR_SPACING
private val BAR_AREA_HEIGHT = 180.dp
private val DAY_LABEL_AREA_HEIGHT = 28.dp
private val CHART_HEIGHT = BAR_AREA_HEIGHT + DAY_LABEL_AREA_HEIGHT
private val DAY_LABEL_FONT_SIZE = 9.sp

// ogni quante barre mostrare l'etichetta del giorno quando non entrano tutte
private const val THINNED_LABEL_STRIDE = 5

// duplicato minimo di StatsPeriod.kt, dove è private
private fun startDay(session: ParkingSession): LocalDate =
    Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun CostChart(allSessions: List<ParkingSession>, period: StatsPeriod) {
    val sessionsInPeriod = remember(allSessions, period) { allSessions.filterByPeriod(period) }
    if (sessionsInPeriod.isEmpty()) {
        EmptyState("Nessuna sosta nel periodo selezionato.")
        return
    }

    // sulla lista completa, non su sessionsInPeriod: per "Tutto" serve vedere anche le soste
    // più vecchie per trovare il primo giorno
    val days = remember(allSessions, period) { allSessions.dayBucketsForPeriod(period) }

    // solo le sessioni con finalCost non null; un valore per ogni giorno, anche a zero
    val costsByDay = remember(sessionsInPeriod, days) {
        val totalsByDay = sessionsInPeriod
            .mapNotNull { session -> session.finalCost?.let { startDay(session) to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, costs) -> costs.sum() }
        days.map { day -> totalsByDay[day] ?: 0.0 }
    }
    // massimo a zero: tutte le soste del periodo erano libere (finalCost null, escluse sopra)
    // o comunque a costo zero. Niente da disegnare, e costsByDay[i] / 0.0 andrebbe evitato
    val maxCost = costsByDay.maxOrNull() ?: 0.0
    if (maxCost == 0.0) {
        EmptyState("Nessun costo nel periodo selezionato: le soste libere non hanno costo.")
        return
    }
    // arrotondato per eccesso e usato sia per l'etichetta dell'asse Y sia per scalare le barre:
    // se scalassimo sul massimo reale (es. 11.40) la barra più alta toccherebbe la cima e
    // sembrerebbe valere quanto l'etichetta (es. "€12"), che invece è solo un arrotondamento
    val displayMaxCost = ceil(maxCost)

    var selectedIndex by rememberSaveable(period) { mutableStateOf<Int?>(null) }

    val scrollState = rememberScrollState()
    // scrollTo(maxValue) sposta la vista sull'ultimo giorno, non sul primo (default)
    LaunchedEffect(period, days.size) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val slotWidthPx = with(LocalDensity.current) { SLOT_WIDTH.toPx() }
    val barWidthPx = with(LocalDensity.current) { BAR_WIDTH.toPx() }
    val barAreaHeightPx = with(LocalDensity.current) { BAR_AREA_HEIGHT.toPx() }
    val dayLabelAreaHeightPx = with(LocalDensity.current) { DAY_LABEL_AREA_HEIGHT.toPx() }
    val totalWidth = SLOT_WIDTH * days.size
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Costo per giorno (€)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        val detailText = selectedIndex?.let { index ->
            "${days[index].format(DAY_LABEL_FORMAT)}: €%.2f".format(costsByDay[index])
        } ?: "Tocca una barra per il dettaglio"
        Text(detailText, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row {
            // asse Y, fuori dallo scroll orizzontale: resta leggibile mentre si scorrono le barre.
            // lo spacer finale, alto come l'area delle etichette giorno sotto il Canvas, allinea
            // "€0" al fondo delle barre invece che al fondo di tutto il grafico
            Column(modifier = Modifier.height(CHART_HEIGHT)) {
                Text("€%.0f".format(displayMaxCost), style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.weight(1f))
                Text("€0", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(DAY_LABEL_AREA_HEIGHT))
            }
            Spacer(modifier = Modifier.width(8.dp))

            Canvas(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .width(totalWidth)
                    .height(CHART_HEIGHT)
                    // offset.x è in px sulla larghezza totale (non sul riquadro visibile):
                    // diviso per slotWidthPx dà l'indice del giorno toccato
                    .pointerInput(days.size) {
                        detectTapGestures { offset ->
                            val index = (offset.x / slotWidthPx).toInt()
                            selectedIndex = index.takeIf { it in days.indices }
                        }
                    }
            ) {
                days.indices.forEach { index ->
                    val barHeight = (costsByDay[index] / displayMaxCost).toFloat() * barAreaHeightPx
                    val left = index * slotWidthPx
                    drawRect(
                        color = if (index == selectedIndex) Color(0xFF1565C0) else Color(0xFF64B5F6),
                        topLeft = Offset(left, barAreaHeightPx - barHeight),
                        size = Size(barWidthPx, barHeight)
                    )

                    // contate a ritroso dal giorno più recente (ultimo indice), non dal primo:
                    // lo scroll parte già sulla fine, quindi è l'ultimo giorno che deve avere
                    // sempre l'etichetta, non il primo
                    val distanceFromMostRecent = days.lastIndex - index
                    val showLabel = period == StatsPeriod.LAST_7_DAYS ||
                        distanceFromMostRecent % THINNED_LABEL_STRIDE == 0
                    if (showLabel) {
                        val layout = textMeasurer.measure(
                            days[index].format(DAY_LABEL_FORMAT),
                            style = TextStyle(fontSize = DAY_LABEL_FONT_SIZE)
                        )
                        drawText(
                            layout,
                            topLeft = Offset(
                                left + barWidthPx / 2f - layout.size.width / 2f,
                                barAreaHeightPx + (dayLabelAreaHeightPx - layout.size.height) / 2f
                            )
                        )
                    }
                }
            }
        }
    }
}
