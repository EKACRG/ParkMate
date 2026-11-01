package com.example.parkmate.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkmate.ParkMateViewModelFactory
import com.example.parkmate.background.DEFAULT_EXPIRY_WARNING_MINUTES
import com.example.parkmate.background.DEFAULT_REMINDER_INTERVAL_MINUTES
import com.example.parkmate.background.MIN_REMINDER_INTERVAL_MINUTES

@Composable
fun SettingsScreen(viewModelFactory: ParkMateViewModelFactory) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val context = LocalContext.current

    val expiryWarningMinutes by viewModel.expiryWarningMinutes.observeAsState(DEFAULT_EXPIRY_WARNING_MINUTES)
    val reminderIntervalMinutes by viewModel.reminderIntervalMinutes.observeAsState(DEFAULT_REMINDER_INTERVAL_MINUTES)
    val suppressedWarningCount by viewModel.suppressedWarningCount.observeAsState()

    var expiryWarningText by rememberSaveable(expiryWarningMinutes) { mutableStateOf(expiryWarningMinutes.toString()) }
    var reminderIntervalText by rememberSaveable(reminderIntervalMinutes) { mutableStateOf(reminderIntervalMinutes.toString()) }

    // le notifiche di sistema si attivano/disattivano fuori dall'app: va riletto ogni volta
    // che questa schermata torna in primo piano, non solo quando viene creata
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notifiche", style = MaterialTheme.typography.titleMedium)
        Text(if (notificationsEnabled) "Attive" else "Disattivate per questa app")
        Button(onClick = {
            context.startActivity(
                Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        }) { Text("Impostazioni di sistema") }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Avviso di scadenza ticket", style = MaterialTheme.typography.titleMedium)
        val expiryMinutesValue = expiryWarningText.toIntOrNull()
        OutlinedTextField(
            value = expiryWarningText,
            onValueChange = { expiryWarningText = it.filter(Char::isDigit) },
            label = { Text("Anticipo in minuti") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = { expiryMinutesValue?.let(viewModel::saveExpiryWarningMinutes) },
            // disabilitato quando il campo è invalido o coincide col valore già salvato
            enabled = expiryMinutesValue != null && expiryMinutesValue != expiryWarningMinutes
        ) { Text("Salva") }
        // il salvataggio può lasciare senza avviso anticipato dei ticket già in corso: lo
        // segnaliamo invece di far sembrare che la notifica arrivi comunque
        suppressedWarningCount?.takeIf { it > 0 }?.let { count ->
            Text(
                text = if (count == 1)
                    "1 sosta in corso scade entro $expiryWarningMinutes minuti: per quella l'avviso anticipato non verrà inviato."
                else
                    "$count soste in corso scadono entro $expiryWarningMinutes minuti: per quelle l'avviso anticipato non verrà inviato.",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Promemoria sosta in corso", style = MaterialTheme.typography.titleMedium)
        val reminderMinutesValue = reminderIntervalText.toIntOrNull()
        val reminderValid = reminderMinutesValue != null && reminderMinutesValue >= MIN_REMINDER_INTERVAL_MINUTES
        OutlinedTextField(
            value = reminderIntervalText,
            onValueChange = { reminderIntervalText = it.filter(Char::isDigit) },
            label = { Text("Almeno ogni, in minuti") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (reminderMinutesValue != null && !reminderValid) {
            Text("Minimo $MIN_REMINDER_INTERVAL_MINUTES minuti", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { reminderMinutesValue?.let(viewModel::saveReminderIntervalMinutes) },
            // disabilitato quando il campo è invalido o coincide col valore già salvato
            enabled = reminderValid && reminderMinutesValue != reminderIntervalMinutes
        ) { Text("Salva") }
    }
}
