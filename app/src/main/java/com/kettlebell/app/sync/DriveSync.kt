package com.kettlebell.app.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.kettlebell.app.debug.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Observable state of the Google Drive connection, surfaced to the Settings screen. */
data class DriveStatus(
    val connected: Boolean = false,
    val email: String? = null,
    val syncing: Boolean = false,
    val lastSyncMillis: Long? = null,
    val lastError: String? = null,
)

/**
 * Backs up and restores the app's SQLite database to the user's private Google Drive
 * "appDataFolder". Uses Google Sign-In for authorization and talks to the Drive REST API
 * directly over HTTP to avoid the heavy google-api-client dependency stack.
 *
 * Requires an OAuth client configured in Google Cloud Console for this app's package name and
 * signing certificate; without it, sign-in requesting the Drive scope will fail (handled
 * gracefully — the app keeps working with local-only storage).
 */
class DriveSync(private val context: Context) {

    private val driveScope = Scope(SCOPE_APPDATA)
    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(driveScope)
        .build()
    private val signInClient = GoogleSignIn.getClient(context, signInOptions)
    private val prefs = context.getSharedPreferences("drive_sync", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(DriveStatus())
    val status: StateFlow<DriveStatus> = _status.asStateFlow()

    init {
        refreshStatus()
    }

    fun isConnected(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, driveScope)
    }

    fun signInIntent(): Intent = signInClient.signInIntent

    fun refreshStatus() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val connected = account != null && GoogleSignIn.hasPermissions(account, driveScope)
        _status.value = _status.value.copy(
            connected = connected,
            email = if (connected) account?.email else null,
            lastSyncMillis = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 },
        )
    }

    /** Parse the sign-in result. Returns true if the Drive scope was granted. */
    fun handleSignInResult(data: Intent?): Boolean {
        return runCatching {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(com.google.android.gms.common.api.ApiException::class.java)
            val granted = GoogleSignIn.hasPermissions(account, driveScope)
            refreshStatus()
            granted
        }.getOrElse {
            AppLogger.e("DriveSync", "Sign-in failed", it)
            _status.value = _status.value.copy(lastError = "Sign-in failed")
            false
        }
    }

    fun disconnect() {
        signInClient.signOut()
        prefs.edit().remove(KEY_LAST_SYNC).apply()
        _status.value = DriveStatus()
    }

    private val dbFile: File get() = context.getDatabasePath(DB_NAME)

    /** Upload the local database to Drive, creating or replacing the remote copy. */
    suspend fun backup(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext false
        _status.value = _status.value.copy(syncing = true, lastError = null)
        val ok = runCatching {
            val token = accessToken() ?: error("No access token")
            val file = dbFile
            if (!file.exists()) return@runCatching true
            val bytes = file.readBytes()
            val existingId = findRemoteFileId(token)
            if (existingId == null) {
                createFile(token, bytes)
            } else {
                updateFile(token, existingId, bytes)
            }
            prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
            true
        }.getOrElse {
            AppLogger.e("DriveSync", "Backup failed", it)
            false
        }
        _status.value = _status.value.copy(
            syncing = false,
            lastSyncMillis = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 },
            lastError = if (ok) null else "Backup failed",
        )
        ok
    }

    /**
     * Download the remote database and overwrite the local file. MUST be called before Room opens
     * the database (the app gates DB access on boot until this completes).
     */
    suspend fun restore(): Boolean = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext false
        _status.value = _status.value.copy(syncing = true, lastError = null)
        val ok = runCatching {
            val token = accessToken() ?: error("No access token")
            val remoteId = findRemoteFileId(token) ?: return@runCatching false
            val bytes = downloadFile(token, remoteId)
            val file = dbFile
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            // Discard any stale write-ahead log / shared-memory for the replaced database.
            File(file.path + "-wal").delete()
            File(file.path + "-shm").delete()
            prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
            true
        }.getOrElse {
            AppLogger.e("DriveSync", "Restore failed", it)
            false
        }
        _status.value = _status.value.copy(
            syncing = false,
            lastSyncMillis = prefs.getLong(KEY_LAST_SYNC, 0L).takeIf { it > 0 },
            lastError = if (ok) null else "Restore failed",
        )
        ok
    }

    private fun accessToken(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account ?: return null
        return runCatching {
            GoogleAuthUtil.getToken(context, account, "oauth2:$SCOPE_APPDATA")
        }.getOrElse {
            AppLogger.e("DriveSync", "Could not get access token", it)
            null
        }
    }

    private fun findRemoteFileId(token: String): String? {
        val query = URLEncoder.encode("name = '$DB_NAME'", "UTF-8").replace("+", "%20")
        val url = URL(
            "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$query&fields=files(id)",
        )
        val response = httpGet(url, token)
        val files = JSONObject(response).optJSONArray("files") ?: return null
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun createFile(token: String, bytes: ByteArray) {
        val boundary = "kbsync" + System.nanoTime()
        val metadata = JSONObject()
            .put("name", DB_NAME)
            .put("parents", org.json.JSONArray().put("appDataFolder"))
            .toString()
        val body = buildMultipart(boundary, metadata, bytes)
        val connection = openConnection(
            URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"),
            token,
            "POST",
        )
        connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        writeAndCheck(connection, body, "createFile")
    }

    private fun updateFile(token: String, fileId: String, bytes: ByteArray) {
        val connection = openConnection(
            URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"),
            token,
            "PATCH",
        )
        connection.setRequestProperty("Content-Type", "application/octet-stream")
        writeAndCheck(connection, bytes, "updateFile")
    }

    private fun downloadFile(token: String, fileId: String): ByteArray {
        val connection = openConnection(
            URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media"),
            token,
            "GET",
        )
        connection.connect()
        if (connection.responseCode !in 200..299) {
            error("Download failed: HTTP ${connection.responseCode}")
        }
        return connection.inputStream.use { it.readBytes() }
    }

    private fun buildMultipart(boundary: String, metadataJson: String, media: ByteArray): ByteArray {
        val header = (
            "--$boundary\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                metadataJson + "\r\n" +
                "--$boundary\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n"
            ).toByteArray()
        val footer = "\r\n--$boundary--".toByteArray()
        return header + media + footer
    }

    private fun openConnection(url: URL, token: String, method: String): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        // HttpURLConnection doesn't support PATCH natively; tunnel it through POST + override header.
        if (method == "PATCH") {
            connection.requestMethod = "POST"
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        } else {
            connection.requestMethod = method
        }
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        if (method != "GET") connection.doOutput = true
        return connection
    }

    private fun httpGet(url: URL, token: String): String {
        val connection = openConnection(url, token, "GET")
        connection.connect()
        if (connection.responseCode !in 200..299) {
            error("Request failed: HTTP ${connection.responseCode}")
        }
        return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun writeAndCheck(connection: HttpURLConnection, body: ByteArray, label: String) {
        connection.outputStream.use { it.write(body) }
        if (connection.responseCode !in 200..299) {
            val err = connection.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }
            error("$label failed: HTTP ${connection.responseCode} $err")
        }
    }

    private companion object {
        const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val DB_NAME = "kettlebell.db"
        const val KEY_LAST_SYNC = "last_sync"
    }
}
