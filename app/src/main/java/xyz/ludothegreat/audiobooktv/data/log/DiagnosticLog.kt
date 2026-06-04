package xyz.ludothegreat.audiobooktv.data.log

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.ludothegreat.audiobooktv.data.settings.AppSettings
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticLog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun i(tag: String, message: String) = write("I", tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = write("W", tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = write("E", tag, message, throwable)

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            "I" -> Log.i(tag, message)
            "W" -> Log.w(tag, message, throwable)
            "E" -> Log.e(tag, message, throwable)
        }
        scope.launch {
            if (!appSettings.diagnosticLogEnabledSnapshot()) return@launch
            runCatching {
                val file = currentLogFile() ?: return@runCatching
                if (file.length() > MAX_LOG_BYTES) rotate(file)
                val ts = timestampFormat.format(Date())
                val line = buildString {
                    append('[').append(ts).append("] ").append(level).append('/').append(tag).append(": ").append(message)
                    if (throwable != null) {
                        append('\n').append(Log.getStackTraceString(throwable))
                    }
                    append('\n')
                }
                file.appendText(line)
            }
        }
    }

    private fun currentLogFile(): File? {
        val dir = context.getExternalFilesDir(null) ?: return null
        return File(dir, "audiobooktv.log")
    }

    private fun rotate(current: File) {
        val dir = current.parentFile ?: return
        for (i in MAX_ROTATIONS - 1 downTo 1) {
            val older = File(dir, "audiobooktv.log.$i")
            val newer = File(dir, "audiobooktv.log.${i + 1}")
            if (newer.exists()) newer.delete()
            if (older.exists()) older.renameTo(newer)
        }
        val rotated = File(dir, "audiobooktv.log.1")
        if (rotated.exists()) rotated.delete()
        current.renameTo(rotated)
    }

    companion object {
        private const val MAX_LOG_BYTES = 1_048_576L
        private const val MAX_ROTATIONS = 3
    }
}
