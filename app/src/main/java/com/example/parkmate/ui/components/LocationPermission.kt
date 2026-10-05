package com.example.parkmate.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

// preferenze private dell'app: ricordano, per ogni gruppo di permessi, se li abbiamo già chiesti
private const val PREFS_NAME = "parkmate_permissions"

// chiave unica per un gruppo di permessi: i nomi ordinati e uniti con "|", così l'ordine della
// lista non cambia la chiave e ogni gruppo ha il suo stato
private fun alreadyAskedKey(permissions: List<String>): String =
    "already_asked:" + permissions.sorted().joinToString("|")

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// apre la schermata di sistema dei permessi di questa app, unico modo per concedere un permesso negato in modo permanente
private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    startActivity(intent)
}

// Il "content" riceve la funzione "request" da collegare al pulsante. Il componente disegna da sé
// i dialog di motivazione e di impostazioni, nel punto in cui viene chiamato
@Composable
fun PermissionRequester(
    permissions: List<String>,
    onResult: (Map<String, Boolean>) -> Unit,
    rationaleTitle: String,
    rationaleText: String,
    settingsTitle: String,
    settingsText: String,
    content: @Composable (request: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    // ricalcolata solo se cambia la lista dei permessi
    val askedKey = remember(permissions) { alreadyAskedKey(permissions) }

    // stato dei dialog
    var showRationale by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // il launcher mostra il dialog di sistema e ci restituisce l'esito quando l'utente risponde
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        onResult(results)
    }

    fun launchSystemDialog() {
        // segniamo che abbiamo chiesto, così al prossimo rifiuto permanente sappiamo distinguerlo
        prefs.edit { putBoolean(askedKey, true) }
        launcher.launch(permissions.toTypedArray())
    }

    // chiamata ogni volta che si preme il pulsante: legge lo stato in quel momento, non prima
    fun request() {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        // vero solo dopo un rifiuto non ancora permanente
        val shouldShowRationale = activity?.let { act ->
            permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(act, it) }
        } ?: false
        val alreadyAsked = prefs.getBoolean(askedKey, false)

        when {
            allGranted -> onResult(permissions.associateWith { true })
            shouldShowRationale -> showRationale = true
            // già chiesto e nessuna motivazione: il sistema non mostrerà più il dialog
            alreadyAsked -> showSettings = true
            else -> launchSystemDialog()
        }
    }

    content { request() }

    if (showRationale) {
        ConfirmDialog(
            title = rationaleTitle,
            text = rationaleText,
            confirmLabel = "Continua",
            onConfirm = {
                // conferma della motivazione: parte la richiesta di sistema
                showRationale = false
                launchSystemDialog()
            },
            onDismiss = { showRationale = false }
        )
    }

    if (showSettings) {
        ConfirmDialog(
            title = settingsTitle,
            text = settingsText,
            confirmLabel = "Apri impostazioni",
            onConfirm = {
                // conferma delle impostazioni: parte l'Intent verso la schermata dell'app
                showSettings = false
                context.openAppSettings()
            },
            onDismiss = { showSettings = false }
        )
    }
}
