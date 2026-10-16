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

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// apre la schermata di sistema dei permessi di questa app, unico modo per concedere un permesso negato in modo permanente
fun Context.openAppSettings() {
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

    // stato dei dialog
    var showRationale by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // true solo dopo un rifiuto non permanente: l'API non distingue "mai chiesto" da "negato per
    // sempre" (in entrambi i casi dà false), quindi non si può usarla per decidere IN ANTICIPO se
    // mostrare le impostazioni. Lo sappiamo per certo solo subito DOPO un tentativo rifiutato
    fun canShowSystemDialogAgain(): Boolean =
        activity?.let { act -> permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(act, it) } } ?: true

    // il launcher mostra il dialog di sistema e ci restituisce l'esito quando l'utente risponde
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        onResult(results)
        // tutto negato e il sistema non lo richiederebbe più: ora è certo, non prima
        if (results.values.none { it } && !canShowSystemDialogAgain()) {
            showSettings = true
        }
    }

    fun launchSystemDialog() {
        launcher.launch(permissions.toTypedArray())
    }

    // chiamata ogni volta che si preme il pulsante: legge lo stato in quel momento, non prima
    fun request() {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        when {
            allGranted -> onResult(permissions.associateWith { true })
            canShowSystemDialogAgain() -> showRationale = true
            // non è la prima volta e il sistema potrebbe non chiedere più: proviamo comunque,
            // è sicuro anche se è bloccato per sempre (il sistema risponde negato all'istante,
            // senza mostrare nulla), e il launcher sopra rileva il caso davvero permanente
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
