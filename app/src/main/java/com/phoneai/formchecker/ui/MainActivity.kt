package com.phoneai.formchecker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "カメラの許可が必要です", Toast.LENGTH_SHORT).show()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCapture.setOnClickListener { onCaptureClicked() }
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
                    "✅ オンデバイスAI (Gemma)\nモデル: 読込済み (${ModelManager.modelSizeMb(this)} MB) ・ オフライン動作"
                } else {
                    "⚠ オンデバイスAI (Gemma)\nモデル未読込。「AIモデルを管理」から .task ファイルを取り込んでください。"
                }
            }
            AnalyzerEngine.CLAUDE_CLOUD -> {
                if (ModelManager.hasCloudKey()) "☁ クラウドAI (Claude) ・ オンライン"
                else "⚠ クラウドAI (Claude)\nAPIキー未設定 (ビルド時に CLAUDE_API_KEY が必要)"
            }
        }
        binding.tvEngineStatus.text = status
    }

    private fun onCaptureClicked() {
        val engine = ModelManager.getEngine(this)
        if (engine == AnalyzerEngine.LOCAL_GEMMA && !ModelManager.isModelReady(this)) {
            AlertDialog.Builder(this)
                .setTitle("モデルが必要です")
                .setMessage("オンデバイスAIを使うには Gemma 3n のモデルファイル(.task)を取り込む必要があります。今すぐ取り込みますか？")
                .setPositiveButton("取り込む") { _, _ -> pickModel() }
                .setNegativeButton("キャンセル", null)
                .show()
            return
        }
        if (engine == AnalyzerEngine.CLAUDE_CLOUD && !ModelManager.hasCloudKey()) {
            Toast.makeText(this, "APIキーが未設定です。ローカルエンジンに切替えるか、APIキー付きでビルドしてください。", Toast.LENGTH_LONG).show()
            return
        }
        checkCameraPermission()
    }

    private fun onManageModelClicked() {
        val ready = ModelManager.isModelReady(this)
        val msg = if (ready)
            "モデル読込済み (${ModelManager.modelSizeMb(this)} MB)。\n別のモデルに差し替える場合は「取り込む」を選んでください。"
        else
            "Gemma 3n の .task モデルファイルを端末にダウンロードしてから「取り込む」を選んでください。\n（推奨: gemma-3n の画像対応 .task モデル）"

        val builder = AlertDialog.Builder(this)
            .setTitle("AIモデルの管理")
            .setMessage(msg)
            .setPositiveButton("取り込む") { _, _ -> pickModel() }
            .setNegativeButton("閉じる", null)
        if (ready) {
            builder.setNeutralButton("削除") { _, _ ->
                ModelManager.deleteModel(this)
                refreshStatus()
                Toast.makeText(this, "モデルを削除しました", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    private fun onSwitchEngineClicked() {
        val current = ModelManager.getEngine(this)
        val options = arrayOf("オンデバイスAI (Gemma) ・ オフライン", "クラウドAI (Claude) ・ オンライン")
        val checked = if (current == AnalyzerEngine.LOCAL_GEMMA) 0 else 1
        AlertDialog.Builder(this)
            .setTitle("解析エンジンを選択")
            .setSingleChoiceItems(options, checked) { dialog, which ->
                val engine = if (which == 0) AnalyzerEngine.LOCAL_GEMMA else AnalyzerEngine.CLAUDE_CLOUD
                ModelManager.setEngine(this, engine)
                refreshStatus()
                dialog.dismiss()
            }
            .setNegativeButton("キャンセル", null)
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
        binding.tvProgress.text = "モデルを取り込み中..."

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ModelManager.importModel(this@MainActivity, uri) { copied ->
                        val mb = copied / (1024 * 1024)
                        runOnUiThread { binding.tvProgress.text = "取り込み中... $mb MB" }
                    }
                }
                Toast.makeText(this@MainActivity, "モデルの取り込みが完了しました", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "取り込み失敗: ${e.message}", Toast.LENGTH_LONG).show()
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
