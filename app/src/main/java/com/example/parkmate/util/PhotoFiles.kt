package com.example.parkmate.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

// crea (e se serve crea la cartella di) un nuovo file vuoto in filesDir/photos/, pronto per
// essere passato alla fotocamera come destinazione dello scatto
fun createPhotoFile(context: Context, sessionId: Long): File {
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    return File(dir, "session_${sessionId}_${System.currentTimeMillis()}.jpg")
}

// URI content:// che la fotocamera di sistema può scrivere senza accesso diretto al file
fun photoUriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_AUTHORITY_SUFFIX, file)

// cancella il file della foto, se esiste. Non fa nulla se path è null o il file è già assente
fun deletePhotoFile(path: String?) {
    path?.let { File(it).delete() }
}

// decodifica la foto già ridotta a maxDimension, per non caricare l'immagine a piena
// risoluzione solo per mostrarne una miniatura. Restituisce null se il file non esiste o non è leggibile
fun decodePhotoThumbnail(path: String, maxDimension: Int): Bitmap? {
    val file = File(path)
    if (!file.exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= maxDimension && bounds.outHeight / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeFile(path, options)
}
