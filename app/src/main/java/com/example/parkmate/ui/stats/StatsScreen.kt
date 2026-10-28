package com.example.parkmate.ui.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.ui.components.DropdownField

private enum class StatsView(val label: String) {
    HEAT_MAP("Heat map"),
    COST_CHART("Grafico costi")
}

@Composable
fun StatsScreen(viewModelFactory: ParkMateViewModelFactory) {
    val viewModel: StatsViewModel = viewModel(factory = viewModelFactory)
    val closedSessions by viewModel.closedSessions.observeAsState(emptyList())

    var selectedPeriod by rememberSaveable { mutableStateOf(StatsPeriod.LAST_7_DAYS) }
    var selectedView by rememberSaveable { mutableStateOf(StatsView.HEAT_MAP) }

    // filtra una sola volta qui: la heat map usa solo questo, il grafico riceve anche la lista
    // completa (gli serve per calcolare il primo giorno di "Tutto")
    val sessionsInPeriod = remember(closedSessions, selectedPeriod) {
        closedSessions.filterByPeriod(selectedPeriod)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        DropdownField(
            label = "Periodo",
            selectedLabel = selectedPeriod.label,
            options = StatsPeriod.entries,
            optionLabel = { it.label },
            onSelect = { selectedPeriod = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        DropdownField(
            label = "Visualizzazione",
            selectedLabel = selectedView.label,
            options = StatsView.entries,
            optionLabel = { it.label },
            onSelect = { selectedView = it }
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedView) {
                StatsView.HEAT_MAP -> ParkingHeatMap(sessions = sessionsInPeriod)
                StatsView.COST_CHART -> CostChart(allSessions = closedSessions, period = selectedPeriod)
            }
        }
    }
}
