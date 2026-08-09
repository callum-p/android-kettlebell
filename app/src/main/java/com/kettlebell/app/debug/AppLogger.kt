package com.kettlebell.app.debug

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A single captured log line. */
data class LogEntry(
    val timeMillis: Long,
    val level: String,
    val tag: String,
    val message: String,
    val stackTrace: String? = null,
) {
    val time: String
        get() = FORMATTER.format(Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()))

    private companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss")
    }
}

/**
 * Lightweight in-app logger that captures exceptions (both caught and uncaught) so they can be
 * inspected from the Settings screen. Entries are mirrored to a file so they survive restarts —
 * important for diagnosing crashes.
 */
object AppLogger {

    private const val TAG = "Kettlebell"
    private const val MAX_ENTRIES = 500
    private const val LOG_FILE = "debug_log.tsv"
    private const val FIELD_SEP = "\u001F" // unit separator
    private const val LINE_SEP = "\u001E" // record separator

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    private var logFile: File? = null

    /** Wire up persistence and install the uncaught-exception handler. Call once from Application. */
    fun init(context: Context) {
        val file = File(context.filesDir, LOG_FILE)
        logFile = file
        _entries.value = readFromDisk(file)
        i(TAG, "Logger initialised (${_entries.value.size} prior entries)")

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("UncaughtException", "Crash on thread '${thread.name}'", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun i(tag: String, message: String) = add(LogEntry(now(), "INFO", tag, message))

    fun w(tag: String, message: String, throwable: Throwable? = null) =
        add(LogEntry(now(), "WARN", tag, message, throwable?.let(::stackTraceOf)))

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        add(LogEntry(now(), "ERROR", tag, message, throwable?.let(::stackTraceOf)))
    }

    fun clear() {
        _entries.value = emptyList()
        logFile?.let { runCatching { it.writeText("") } }
    }

    fun exportText(): String = _entries.value.joinToString("\n") { entry ->
        buildString {
            append("[${entry.time}] ${entry.level}/${entry.tag}: ${entry.message}")
            entry.stackTrace?.let { append("\n").append(it) }
        }
    }

    @Synchronized
    private fun add(entry: LogEntry) {
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        appendToDisk(entry)
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun stackTraceOf(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString().trim()
    }

    private fun sanitize(value: String): String =
        value.replace(FIELD_SEP, " ").replace(LINE_SEP, " ")

    private fun appendToDisk(entry: LogEntry) {
        val file = logFile ?: return
        runCatching {
            val encoded = listOf(
                entry.timeMillis.toString(),
                entry.level,
                entry.tag,
                sanitize(entry.message),
                sanitize(entry.stackTrace ?: ""),
            ).joinToString(FIELD_SEP)
            file.appendText(encoded + LINE_SEP)
        }
    }

    private fun readFromDisk(file: File): List<LogEntry> = runCatching {
        if (!file.exists()) return emptyList()
        file.readText()
            .split(LINE_SEP)
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(FIELD_SEP)
                if (parts.size < 5) return@mapNotNull null
                LogEntry(
                    timeMillis = parts[0].toLongOrNull() ?: return@mapNotNull null,
                    level = parts[1],
                    tag = parts[2],
                    message = parts[3],
                    stackTrace = parts[4].ifBlank { null },
                )
            }
            .takeLast(MAX_ENTRIES)
    }.getOrDefault(emptyList())
}
