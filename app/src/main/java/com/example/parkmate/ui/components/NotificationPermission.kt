package com.example.parkmate.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit

// preferenze indipendenti da quelle di PermissionRequester: è un permesso diverso, con una
// logica diversa (niente motivazione, niente rimando alle impostazioni da qui)
private const val PREFS_NAME = "parkmate_notification_permission"
private const val KEY_ALREADY_ASKED = "already_asked"

/**
 * Chiede POST_NOTIFICATIONS una sola volta, senza insistere. Pensato per essere chiamato
 * all'avvio dell'app, non su un tocco dell'utente.
 *
 * Sotto API 33 il permesso non esiste: non facciamo nulla.
 * Già concesso: non facciamo nulla.
 * Mai chiesto: mostriamo il dialog di sistema.
 * Già chiesto e negato: non facciamo nulla, silenziosamente. La richiesta esplicita,
 * con l'eventuale rimando alle impostazioni, è nella pagina impostazioni.
 */
@Composable
fun RequestNotificationPermissionOnce() {
    // versione dove il permesso non esiste
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // risultato ignorato: in nessun caso insistiamo da qui, negato o concesso che sia
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val alreadyAsked = prefs.getBoolean(KEY_ALREADY_ASKED, false)

        if (!granted && !alreadyAsked) {
            prefs.edit { putBoolean(KEY_ALREADY_ASKED, true) }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
