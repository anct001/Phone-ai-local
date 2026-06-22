package com.phoneai.formchecker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.phoneai.formchecker.analyzer.AnalyzerEngine
import com.phoneai.formchecker.analyzer.ModelManager
import com.phoneai.formchecker.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Cần cấp quyền truy cập camera", Toast.LENGTH_SHORT).show()
    }

    private val cameraResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imagePath = result.data?.getStringExtra(CameraActivity.EXTRA_IMAGE_PATH)
            if (imagePath != null) {
                startActivity(Intent(this, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_IMAGE_PATH, imagePath)
                })
            }
        }
    }

    private val pickModelLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importModel(it) } }

    private val galleryLauncher = registerForActivityResult(
        PickVisualMedia()
    ) { uri -> uri?.let { loadGalleryImage(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCapture.setOnClickListener { onCaptureClicked() }
        binding.btnGallery.setOnClickListener { onGalleryClicked() }
        binding.btnModel.setOnClickListener { onManageModelClicked() }
        binding.btnSwitchEngine.setOnClickListener { onSwitchEngineClicked() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val engine = ModelManager.getEngine(this)
        val status = when (engine) {
            AnalyzerEngine.LOCAL_GEMMA -> {
                if (ModelManager.isModelReady(this)) {
                    "✅ AI trên thiết bị (Gemma)\nModel: đã tải (${ModelManager.modelSizeMb(this)} MB) · Hoạt động offline"
                } else {
                    "⚠ AI trên thiết bị (Gemma)\nChưa có model. Vào \"Quản lý model AI\" để tải file .task."
                }
            }
            AnalyzerEngine.CLAUDE_CLOUD -> {
                if (ModelManager.hasCloudKey()) "☁ AI đám mây (Claude) · Trực tuyến"
                else "⚠ AI đám mây (Claude)\nChưa có API key (cần CLAUDE_API_KEY khi build)"
            }
        }
        binding.tvEngineStatus.text = status
    }

    private fun onCaptureClicked() {
        val engine = ModelManager.getEngine(this)
        if (engine == AnalyzerEngine.LOCAL_GEMMA && !ModelManager.isModelReady(this)) {
            AlertDialog.Builder(this)
                .setTitle("Cần model AI")
                .setMessage("Để dùng AI trên thiết bị, cần có file model Gemma 3n (.task).")
                .setPositiveButton("⬇ Tải xuống") { _, _ -> openDownloadScreen() }
                .setNeutralButton("📂 Chọn file") { _, _ -> pickModel() }
                .setNegativeButton("Hủy", null)
                .show()
            return
        }
        if (engine == AnalyzerEngine.CLAUDE_CLOUD && !ModelManager.hasCloudKey()) {
            Toast.makeText(this, "Chưa có API key. Hãy chuyển sang engine local hoặc build kèm CLAUDE_API_KEY.", Toast.LENGTH_LONG).show()
            return
        }
        checkCameraPermission()
    }

    private fun onGalleryClicked() {
        val engine = ModelManager.getEngine(this)
        if (engine == AnalyzerEngine.LOCAL_GEMMA && !ModelManager.isModelReady(this)) {
            AlertDialog.Builder(this)
                .setTitle("Cần model AI")
                .setMessage("Để dùng AI trên thiết bị, cần có file model Gemma 3n (.task).")
                .setPositiveButton("⬇ Tải xuống") { _, _ -> openDownloadScreen() }
                .setNeutralButton("📂 Chọn file") { _, _ -> pickModel() }
                .setNegativeButton("Hủy", null)
                .show()
            return
        }
        if (engine == AnalyzerEngine.CLAUDE_CLOUD && !ModelManager.hasCloudKey()) {
            Toast.makeText(this, "Chưa có API key. Hãy chuyển sang engine local.", Toast.LENGTH_LONG).show()
            return
        }
        galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    }

    private fun loadGalleryImage(uri: Uri) {
        binding.layoutProgress.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false
        binding.btnGallery.isEnabled = false
        binding.tvProgress.text = "Đang tải ảnh..."

        lifecycleScope.launch {
            try {
                val destFile = withContext(Dispatchers.IO) {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val dest = File(externalCacheDir ?: cacheDir, "form_$timestamp.jpg")
                    contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Không mở được ảnh" }
                        dest.outputStream().use { input.copyTo(it) }
                    }
                    dest
                }
                startActivity(Intent(this@MainActivity, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_IMAGE_PATH, destFile.absolutePath)
                })
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Không tải được ảnh: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.layoutProgress.visibility = View.GONE
                binding.btnCapture.isEnabled = true
                binding.btnGallery.isEnabled = true
            }
        }
    }

    private fun onManageModelClicked() {
        val ready = ModelManager.isModelReady(this)
        val msg = if (ready)
            "Đã có model (${ModelManager.modelSizeMb(this)} MB).\nĐể thay thế, hãy tải xuống hoặc chọn file mới."
        else
            "Cần model Gemma 3n (.task).\nNhập URL để tải trong app hoặc chọn file từ thiết bị."

        val builder = AlertDialog.Builder(this)
            .setTitle("Quản lý model AI")
            .setMessage(msg)
            .setPositiveButton("⬇ Tải xuống") { _, _ -> openDownloadScreen() }
            .setNeutralButton("📂 Chọn file") { _, _ -> pickModel() }

        if (ready) {
            builder.setNegativeButton("🗑 Xóa") { _, _ ->
                ModelManager.deleteModel(this)
                refreshStatus()
                Toast.makeText(this, "Đã xóa model", Toast.LENGTH_SHORT).show()
            }
        } else {
            builder.setNegativeButton("Đóng", null)
        }

        builder.show()
    }

    private fun openDownloadScreen() {
        startActivity(Intent(this, ModelDownloadActivity::class.java))
    }

    private fun onSwitchEngineClicked() {
        val current = ModelManager.getEngine(this)
        val options = arrayOf("AI trên thiết bị (Gemma) · Offline", "AI đám mây (Claude) · Trực tuyến")
        val checked = if (current == AnalyzerEngine.LOCAL_GEMMA) 0 else 1
        AlertDialog.Builder(this)
            .setTitle("Chọn engine phân tích")
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val engine = if (which == 0) AnalyzerEngine.LOCAL_GEMMA else AnalyzerEngine.CLAUDE_CLOUD
                ModelManager.setEngine(this, engine)
                refreshStatus()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun pickModel() {
        // .task không có MIME chuẩn -> nhận mọi loại file.
        pickModelLauncher.launch(arrayOf("*/*"))
    }

    private fun importModel(uri: Uri) {
        binding.layoutProgress.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false
        binding.btnModel.isEnabled = false
        binding.tvProgress.text = "Đang import model..."

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ModelManager.importModel(this@MainActivity, uri) { copied ->
                        val mb = copied / (1024 * 1024)
                        runOnUiThread { binding.tvProgress.text = "Đang nhập... $mb MB" }
                    }
                }
                Toast.makeText(this@MainActivity, "Import model hoàn tất", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Import thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.layoutProgress.visibility = View.GONE
                binding.btnCapture.isEnabled = true
                binding.btnModel.isEnabled = true
                refreshStatus()
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) openCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        cameraResultLauncher.launch(Intent(this, CameraActivity::class.java))
    }
}
