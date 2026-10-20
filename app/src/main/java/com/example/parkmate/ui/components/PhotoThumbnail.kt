package com.example.parkmate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.parkmate.util.decodePhotoThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// più dello spazio minimo di prima (piccolo quadrato): ora il riquadro può essere grande quanto
// tutto lo spazio verticale di un dialog, una risoluzione bassa risulterebbe sgranata
private const val THUMBNAIL_MAX_DIMENSION_PX = 1024

// miniatura della foto di una sosta, letta da path. La dimensione la decide il chiamante col
// modifier; Crop riempie il riquadro ritagliando l'eccedenza, senza deformare l'immagine
@Composable
fun PhotoThumbnail(path: String, modifier: Modifier = Modifier) {
    var bitmap by remember(path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(path) {
        val decoded = withContext(Dispatchers.IO) {
            decodePhotoThumbnail(path, THUMBNAIL_MAX_DIMENSION_PX)
        }
        bitmap = decoded?.asImageBitmap()
    }

    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Foto della sosta",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
