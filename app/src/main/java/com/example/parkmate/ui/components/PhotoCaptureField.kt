package com.example.parkmate.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.parkmate.util.createPhotoFile
import com.example.parkmate.util.deletePhotoFile
import com.example.parkmate.util.photoUriFor

// scatto/riscatto della foto di una sosta, condiviso tra il dialog di avvio (foto non ancora
// legata a nessuna sessione) e la scheda della sosta attiva (foto già salvata nel database).
// Chi lo usa decide solo quando persistere il nuovo path tramite onPhotoChanged, e quanto
// spazio dargli tramite modifier; la fotocamera, la pulizia dei file e il cambio foto sono qui
@Composable
fun PhotoCaptureField(
    photoPath: String?,
    onPhotoChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    // eseguito appena prima di aprire la fotocamera: usato da SessionDetailsDialog per salvare
    // la nota prima di lasciare l'app, non serve a chi non ha nulla da salvare in anticipo
    onBeforeLaunch: () -> Unit = {}
) {
    val context = LocalContext.current

    // percorso del file che la fotocamera sta scrivendo: rememberSaveable, non remember, perché
    // deve sopravvivere se l'Activity viene ricreata mentre l'app fotocamera è in primo piano
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (path == null) return@rememberLauncherForActivityResult

        if (success) {
            val previousPath = photoPath
            onPhotoChanged(path)
            if (previousPath != null && previousPath != path) deletePhotoFile(previousPath)
        } else {
            deletePhotoFile(path)
        }
    }

    fun launchCamera() {
        onBeforeLaunch()
        val file = createPhotoFile(context)
        pendingPhotoPath = file.absolutePath
        cameraLauncher.launch(photoUriFor(context, file))
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (photoPath != null) {
            // riempie tutto il riquadro che il chiamante ha assegnato (fillMaxSize, non solo la
            // larghezza): Crop in PhotoThumbnail ritaglia l'eccedenza invece di lasciare bordi vuoti
            PhotoThumbnail(path = photoPath, modifier = Modifier.fillMaxSize())
            TextButton(onClick = { launchCamera() }, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text("Rifai foto")
            }
        } else {
            Button(onClick = { launchCamera() }) { Text("Scatta foto") }
        }
    }
}
