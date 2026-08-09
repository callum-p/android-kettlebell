package com.kettlebell.app.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.kettlebell.app.BuildConfig
import com.kettlebell.app.debug.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A newer release found on GitHub. */
data class ReleaseInfo(
    val version: String,
    val versionCode: Int,
    val apkUrl: String,
    val notes: String,
)

/**
 * Checks the project's GitHub Releases for a newer version, downloads the APK, and hands it to the
 * system package installer. Updating in place works because every release is signed with the same
 * committed debug keystore as the installed app.
 */
object UpdateChecker {

    private const val LATEST_URL =
        "https://api.github.com/repos/callum-p/android-kettlebell/releases/latest"

    /** Returns a release newer than the installed build, or null (offline, up to date, or error). */
    suspend fun check(currentCode: Int = BuildConfig.VERSION_CODE): ReleaseInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = httpGetText(LATEST_URL) ?: return@runCatching null
                val release = parseLatest(body) ?: return@runCatching null
                if (release.versionCode > currentCode) release else null
            }.onFailure { AppLogger.e("UpdateChecker", "Update check failed", it) }.getOrNull()
        }

    /** Parses the GitHub "latest release" JSON into a [ReleaseInfo]. Pure; unit-tested. */
    fun parseLatest(json: String): ReleaseInfo? {
        val obj = JSONObject(json)
        val version = obj.optString("tag_name").removePrefix("v").trim()
        if (version.isEmpty()) return null
        val assets = obj.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                if (apkUrl != null) break
            }
        }
        val url = apkUrl ?: return null
        return ReleaseInfo(version, versionCodeOf(version), url, obj.optString("body").trim())
    }

    /** Maps a version like "1.4" / "1.4.2" to an integer code (major*10000 + minor*100 + patch). */
    fun versionCodeOf(version: String): Int {
        val parts = version.split(".")
        fun part(i: Int) = parts.getOrNull(i)?.toIntOrNull() ?: 0
        return part(0) * 10000 + part(1) * 100 + part(2)
    }

    /** Downloads the APK to the app cache, reporting 0–100 progress. Returns the file or null. */
    suspend fun download(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Int) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "kettlebell-v${release.version}.apk")

            val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 60_000
                setRequestProperty("User-Agent", "Kettlebell-App")
            }
            connection.inputStream.use { input ->
                val total = connection.contentLengthLong
                out.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            connection.disconnect()
            out
        }.onFailure { AppLogger.e("UpdateChecker", "APK download failed", it) }.getOrNull()
    }

    /** Launches the system installer for a downloaded APK via a FileProvider content URI. */
    fun install(context: Context, apk: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure { AppLogger.e("UpdateChecker", "Failed to launch installer", it) }
    }

    private fun httpGetText(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Kettlebell-App")
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
