package com.phoneai.formchecker.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.phoneai.formchecker.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        private const val TAG = "CameraActivity"
    }

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        binding.btnShutter.setOnClickListener { takePhoto() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                Toast.makeText(this, "カメラの起動に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val outputDir = externalCacheDir ?: cacheDir
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(outputDir, "form_$timestamp.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        binding.btnShutter.isEnabled = false
        binding.tvHint.text = "保存中..."

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val result = Intent().apply {
                        putExtra(EXTRA_IMAGE_PATH, outputFile.absolutePath)
                    }
                    setResult(RESULT_OK, result)
                    finish()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exc)
                    Toast.makeText(
                        this@CameraActivity, "撮影に失敗しました: ${exc.message}", Toast.LENGTH_SHORT
                    ).show()
                    binding.btnShutter.isEnabled = true
                    binding.tvHint.text = "シャッターを押して撮影"
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
