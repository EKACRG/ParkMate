package com.example.parkmate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.parkmate.util.decodePhotoThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_MAX_DIMENSION_PX = 300

// miniatura della foto di una sosta, letta da path. Usata nella scheda della sosta attiva e,
// in sola lettura, nello storico
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
            modifier = modifier.size(120.dp)
        )
    }
}
