package com.example.parkmate.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// sotto API 29 il permesso non esiste: il foreground basta già
fun hasBackgroundLocationAccess(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

@Composable
fun BackgroundLocationPermissionRequester(
    content: @Composable (request: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showSettingsInvite by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // risultato ignorato: niente da fare in entrambi i casi, come RequestNotificationPermissionOnce
    }

    fun request() {
        when {
            hasBackgroundLocationAccess(context) -> Unit
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
                launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            // qui SDK_INT è già necessariamente >= 30: il primo ramo esclude <29, il secondo esclude 29
            else -> showSettingsInvite = true
        }
    }

    content { request() }

    // ripete il controllo della versione perché Lint non riesce a seguirlo
    if (showSettingsInvite && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val label = context.packageManager.backgroundPermissionOptionLabel
        ConfirmDialog(
            title = "Avvisi anche ad app chiusa",
            text = "Per gli avvisi di ingresso e uscita anche quando ParkMate non è in primo piano, concedi \"$label\" nelle impostazioni dell'app.",
            confirmLabel = "Apri impostazioni",
            onConfirm = {
                showSettingsInvite = false
                context.openAppSettings()
            },
            onDismiss = { showSettingsInvite = false }
        )
    }
}
