package com.pedroeu.ficha.data

import android.content.Context
import android.net.Uri

/**
 * Reading and writing the backup file itself.
 *
 * The document is chosen through the system picker, so the file lands wherever the player
 * keeps things — a cloud folder, Downloads, an SD card — and no storage permission is needed.
 * Somewhere outside the app's own data is the whole point: that is the part an uninstall
 * doesn't take with it.
 */
class BackupFiles(private val context: Context) {

    fun write(uri: Uri, text: String): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw BackupError("Couldn't open that location for writing.")
    }

    fun read(uri: Uri): Result<String> = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: throw BackupError("Couldn't open that file.")
    }
}
