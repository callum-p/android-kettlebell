package com.kettlebell.app.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kettlebell.app.data.db.KettlebellDatabase
import com.kettlebell.app.debug.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Exports/imports the SQLite database to a user-chosen file via the Storage Access Framework. */
object DatabaseBackup {

    private const val DB_NAME = "kettlebell.db"
    private const val SQLITE_HEADER = "SQLite format 3"

    private fun databaseFile(context: Context): File = context.getDatabasePath(DB_NAME)

    /** Copy the current database to the document [uri] the user picked. */
    suspend fun copyDatabaseToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = databaseFile(context)
            require(file.exists()) { "No database file to back up yet" }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { it.copyTo(output) }
            } ?: error("Could not open the chosen file for writing")
            true
        }.getOrElse {
            AppLogger.e("DatabaseBackup", "Export failed", it)
            false
        }
    }

    /** Overwrite the local database from the chosen [uri]. Caller must restart the app afterwards. */
    suspend fun importDatabaseFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Could not read the chosen file")
            require(isSqlite(bytes)) { "That file is not a Kettlebell backup" }

            KettlebellDatabase.closeInstance()
            val file = databaseFile(context)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            // Drop any stale write-ahead log / shared-memory for the replaced database.
            File(file.path + "-wal").delete()
            File(file.path + "-shm").delete()
            true
        }.getOrElse {
            AppLogger.e("DatabaseBackup", "Import failed", it)
            false
        }
    }

    /** Relaunch the app from scratch so Room reopens the freshly restored database file. */
    fun restartApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(launch)
        }
        Runtime.getRuntime().exit(0)
    }

    private fun isSqlite(bytes: ByteArray): Boolean {
        if (bytes.size < 16) return false
        return String(bytes.copyOfRange(0, SQLITE_HEADER.length), Charsets.US_ASCII) == SQLITE_HEADER
    }
}
