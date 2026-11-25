package id.irgunawan.winanp

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

class CrashReporter(private val ctx: Context, private val app: String, private val version: String) : Thread.UncaughtExceptionHandler {
    private val previous = Thread.getDefaultUncaughtExceptionHandler()

    fun install() { Thread.setDefaultUncaughtExceptionHandler(this) }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
            val did = try { Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) } catch (_: Throwable) { "" }
            val stack = sw.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", "\\n")
            val json = "{\"app\":\"$app\",\"package\":\"${ctx.packageName}\",\"version\":\"$version\"," +
                "\"device\":\"${Build.MODEL}\",\"device_id\":\"$did\",\"android\":\"${Build.VERSION.RELEASE}\",\"stack\":\"$stack\"}"
            val c = URL("https://portal.irgun.web.id/api/crash").openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.connectTimeout = 3000; c.readTimeout = 3000; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val b = json.toByteArray(Charsets.UTF_8); c.setFixedLengthStreamingMode(b.size)
            c.outputStream.use { it.write(b) }
            Log.i("CrashReporter", "HTTP " + c.responseCode); c.disconnect()
        } catch (_: Throwable) {}
        if (previous != null) previous.uncaughtException(t, e) else System.exit(10)
    }
}
