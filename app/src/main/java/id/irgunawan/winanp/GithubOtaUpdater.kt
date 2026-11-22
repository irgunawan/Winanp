package id.irgunawan.winanp

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class OtaReleaseInfo(
    val tagName: String,
    val versionName: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val isNewer: Boolean
)

class GithubOtaUpdater(private val context: Context) {

    suspend fun checkLatestRelease(
        owner: String = BuildConfig.GITHUB_OWNER,
        repo: String = BuildConfig.GITHUB_REPO,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): Result<OtaReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Winanp-Android-RedmiNote11")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("GitHub API merespons kode ${conn.responseCode}. Pastikan repo $owner/$repo memiliki Release publik.")
                )
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "v2.91.0")
            val cleanRemoteVersion = tagName.removePrefix("v").removePrefix("V")
            val cleanLocalVersion = currentVersionName.removePrefix("v").removePrefix("V")
            val changelog = json.optString("body", "Pembaruan OTA Winanp Classic.")

            var apkUrl = json.optString("html_url", "")
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", apkUrl)
                        break
                    }
                }
            }

            val isNewer = isRemoteVersionNewer(cleanRemoteVersion, cleanLocalVersion)
            Result.success(
                OtaReleaseInfo(
                    tagName = tagName,
                    versionName = cleanRemoteVersion,
                    changelog = changelog,
                    apkDownloadUrl = apkUrl,
                    isNewer = isNewer
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun downloadAndInstallApk(apkUrl: String, versionTag: String, onStatus: (String) -> Unit) {
        val fileName = "Winanp-${versionTag.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}.apk"
        val destFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Winanp OTA Update ($versionTag)")
            .setDescription("Mengunduh pembaruan aplikasi Winanp untuk Redmi Note 11...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)
        onStatus("Mengunduh $fileName via DownloadManager...")

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    onStatus("Unduhan selesai. Membuka pemasang paket Android...")
                    promptInstallApk(destFile)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    private fun promptInstallApk(apkFile: File) {
        if (!apkFile.exists()) return
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    private fun isRemoteVersionNewer(remote: String, local: String): Boolean {
        val rParts = remote.split(".", "-").mapNotNull { it.toIntOrNull() }
        val lParts = local.split(".", "-").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
