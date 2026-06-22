package com.phoneai.formchecker.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.StatFs
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.phoneai.formchecker.analyzer.ModelManager
import com.phoneai.formchecker.databinding.ActivityModelDownloadBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class ModelDownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelDownloadBinding
    private var downloadJob: Job? = null
    private var activeCall: Call? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "AIモデルをダウンロード"
        }

        refreshStorageInfo()
        refreshNetworkWarning()

        binding.btnDownload.setOnClickListener { startDownload() }
        binding.btnCancelDownload.setOnClickListener { cancelDownload() }
    }

    override fun onResume() {
        super.onResume()
        refreshNetworkWarning()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        activeCall?.cancel()
    }

    private fun refreshStorageInfo() {
        val availableMb = availableStorageMb()
        val color = if (availableMb < 1500) getColor(android.R.color.holo_red_dark)
                    else getColor(android.R.color.darker_gray)
        binding.tvStorageInfo.text = "利用可能ストレージ: ${availableMb} MB（推奨: 2000 MB 以上）"
        binding.tvStorageInfo.setTextColor(color)
    }

    private fun refreshNetworkWarning() {
        binding.tvWifiWarning.visibility = if (isOnWifi()) View.GONE else View.VISIBLE
    }

    private fun availableStorageMb(): Long {
        val stat = StatFs(filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong / (1024L * 1024L)
    }

    private fun isOnWifi(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun startDownload() {
        val url = binding.etDownloadUrl.text?.toString()?.trim().orEmpty()
        if (url.isEmpty()) {
            Toast.makeText(this, "URLを入力してください", Toast.LENGTH_SHORT).show()
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "有効なURL (https://...) を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        setUiDownloading(true)
        binding.progressDownload.isIndeterminate = true
        binding.tvDownloadStatus.text = "接続中..."

        downloadJob = lifecycleScope.launch {
            val tmp = File(ModelManager.modelDir(this@ModelDownloadActivity), "${ModelManager.MODEL_FILE_NAME}.tmp")
            try {
                withContext(Dispatchers.IO) { downloadFile(url, tmp) }

                val dest = ModelManager.modelFile(this@ModelDownloadActivity)
                if (dest.exists()) dest.delete()
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }

                binding.progressDownload.isIndeterminate = false
                binding.progressDownload.progress = 100
                binding.tvDownloadStatus.text = "✅ ダウンロード完了！モデルが使用可能になりました。"
                setUiDownloading(false)
                Toast.makeText(this@ModelDownloadActivity, "ダウンロード完了", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                tmp.delete()
                binding.progressDownload.isIndeterminate = false
                binding.tvDownloadStatus.text = "⛔ キャンセルしました"
                setUiDownloading(false)
                throw e
            } catch (e: Exception) {
                tmp.delete()
                val msg = if (activeCall?.isCanceled() == true) "⛔ キャンセルしました"
                          else "❌ エラー: ${e.message}"
                binding.progressDownload.isIndeterminate = false
                binding.tvDownloadStatus.text = msg
                setUiDownloading(false)
                if (activeCall?.isCanceled() != true) {
                    Toast.makeText(this@ModelDownloadActivity, "ダウンロード失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun downloadFile(url: String, dest: File) {
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        activeCall = call

        val response = call.execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${response.message}")

        val body = response.body ?: throw Exception("レスポンスボディが空です")
        val totalBytes = body.contentLength()
        val startMs = System.currentTimeMillis()
        var downloaded = 0L

        body.byteStream().use { input ->
            dest.outputStream().use { output ->
                val buf = ByteArray(1 shl 20) // 1 MB chunks
                while (true) {
                    coroutineContext.ensureActive()
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    downloaded += n

                    val elapsedSec = (System.currentTimeMillis() - startMs) / 1000.0
                    val speedMbps = if (elapsedSec > 0) downloaded / (1024.0 * 1024.0 * elapsedSec) else 0.0
                    val dlMb = downloaded / (1024 * 1024)
                    val totalMb = if (totalBytes > 0) totalBytes / (1024 * 1024) else 0L
                    val pct = if (totalBytes > 0) (downloaded * 100 / totalBytes).toInt() else -1

                    val statusText = buildString {
                        append("ダウンロード中... ${dlMb} MB")
                        if (totalMb > 0) append(" / ${totalMb} MB")
                        if (pct >= 0) append(" (${pct}%)")
                        append("\n速度: ${"%.1f".format(speedMbps)} MB/s")
                    }

                    runOnUiThread {
                        binding.tvDownloadStatus.text = statusText
                        if (pct >= 0) {
                            binding.progressDownload.isIndeterminate = false
                            binding.progressDownload.progress = pct
                        }
                    }
                }
            }
        }
    }

    private fun cancelDownload() {
        activeCall?.cancel()
        downloadJob?.cancel()
    }

    private fun setUiDownloading(downloading: Boolean) {
        binding.etDownloadUrl.isEnabled = !downloading
        binding.tilUrl.isEnabled = !downloading
        binding.btnDownload.visibility = if (downloading) View.GONE else View.VISIBLE
        binding.btnCancelDownload.visibility = if (downloading) View.VISIBLE else View.GONE
        binding.progressDownload.visibility = View.VISIBLE
        binding.tvDownloadStatus.visibility = View.VISIBLE
    }
}
