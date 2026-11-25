package id.irgunawan.winanp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var otaUpdater: GithubOtaUpdater
    private lateinit var audioEngine: AudioEngineController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter(this, "Winanp", "1.0").install()

        otaUpdater = GithubOtaUpdater(this)
        audioEngine = AudioEngineController(this)

        requestAudioPermissionsIfNeeded()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(WinanpNativeBridge(), "WinanpAndroid")
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/winanp_classic.html")
    }

    private fun requestAudioPermissionsIfNeeded() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    inner class WinanpNativeBridge {
        @JavascriptInterface
        fun triggerNativeOtaCheck(owner: String, repo: String) {
            lifecycleScope.launch {
                val res = otaUpdater.checkLatestRelease(owner, repo, BuildConfig.VERSION_NAME)
                res.onSuccess { info ->
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Winanp OTA Updater (${info.tagName})")
                        .setMessage(
                            if (info.isNewer)
                                "Versi baru ${info.versionName} ditemukan di GitHub!\n\nCatatan Rilis:\n${info.changelog}"
                            else
                                "Versi Anda (${BuildConfig.VERSION_NAME}) sudah paling mutakhir. Tetap unduh ulang ${info.tagName}?"
                        )
                        .setPositiveButton("Unduh & Pasang APK") { _, _ ->
                            otaUpdater.downloadAndInstallApk(info.apkDownloadUrl, info.tagName) { msg ->
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                }.onFailure { err ->
                    Toast.makeText(this@MainActivity, "Gagal cek OTA: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        audioEngine.release()
        super.onDestroy()
    }
}
