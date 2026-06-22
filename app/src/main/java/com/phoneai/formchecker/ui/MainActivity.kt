package com.phoneai.formchecker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.phoneai.formchecker.BuildConfig
import com.phoneai.formchecker.databinding.ActivityMainBinding

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
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_IMAGE_PATH, imagePath)
                }
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (BuildConfig.CLAUDE_API_KEY.isEmpty()) {
            binding.tvApiWarning.visibility = android.view.View.VISIBLE
        }

        binding.btnCapture.setOnClickListener {
            if (BuildConfig.CLAUDE_API_KEY.isEmpty()) {
                Toast.makeText(this, "APIキーが設定されていません。local.propertiesを確認してください。", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            checkCameraPermission()
        }

        binding.btnHistory.setOnClickListener {
            Toast.makeText(this, "履歴機能は準備中です", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> openCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        cameraResultLauncher.launch(Intent(this, CameraActivity::class.java))
    }
}
