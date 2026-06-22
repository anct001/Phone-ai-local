package com.phoneai.formchecker.ui

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.phoneai.formchecker.analyzer.AnalyzerEngine
import com.phoneai.formchecker.analyzer.FieldResult
import com.phoneai.formchecker.analyzer.FormAnalysisResult
import com.phoneai.formchecker.analyzer.ModelManager
import com.phoneai.formchecker.databinding.ActivityResultBinding
import com.phoneai.formchecker.databinding.ItemFieldResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath == null) {
            finish()
            return
        }

        val imageFile = File(imagePath)
        showCapturedImage(imageFile)
        analyzeImage(imageFile)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener {
            clearResults()
            analyzeImage(imageFile)
        }
    }

    private fun showCapturedImage(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        binding.ivCapture.setImageBitmap(bitmap)
    }

    private fun analyzeImage(file: File) {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutResults.visibility = View.GONE
        val engineLabel = when (ModelManager.getEngine(this)) {
            AnalyzerEngine.LOCAL_GEMMA -> "AI trên thiết bị (Gemma)"
            AnalyzerEngine.CLAUDE_CLOUD -> "AI đám mây (Claude)"
        }
        binding.tvStatus.text = "$engineLabel đang phân tích..."
        binding.btnRetry.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val analyzer = ModelManager.createAnalyzer(this@ResultActivity)
                    try {
                        analyzer.analyze(file)
                    } finally {
                        analyzer.close()
                    }
                }
                displayResults(result)
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Đã xảy ra lỗi"
                binding.tvStatus.setTextColor(Color.RED)
                binding.btnRetry.isEnabled = true
                Toast.makeText(this@ResultActivity, "Lỗi phân tích: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayResults(result: FormAnalysisResult) {
        binding.progressBar.visibility = View.GONE
        binding.layoutResults.visibility = View.VISIBLE
        binding.btnRetry.isEnabled = true

        binding.tvFormTitle.text = result.formTitle.ifBlank { result.formType }
        binding.tvSummary.text = result.summary

        val (statusColor, statusText) = when (result.overallStatus) {
            "PASS" -> Color.parseColor("#2E7D32") to "✓ Đạt"
            "FAIL" -> Color.parseColor("#C62828") to "✗ Không đạt"
            else   -> Color.parseColor("#E65100") to "⚠ Cần kiểm tra"
        }
        binding.tvStatus.text = statusText
        binding.tvStatus.setTextColor(statusColor)
        binding.cardStatus.setCardBackgroundColor(statusColor)

        binding.llFieldResults.removeAllViews()
        result.fields.forEach { field ->
            addFieldRow(field)
        }

        val okCount = result.fields.count { it.status == "OK" }
        val errCount = result.fields.count { it.status == "ERROR" }
        val warnCount = result.fields.count { it.status == "WARNING" }
        val missingCount = result.fields.count { it.status == "MISSING" }
        binding.tvFieldSummary.text = "Tổng ${result.fields.size} mục: OK $okCount / Lỗi $errCount / Cảnh báo $warnCount / Chưa điền $missingCount"
    }

    private fun addFieldRow(field: FieldResult) {
        val itemBinding = ItemFieldResultBinding.inflate(
            LayoutInflater.from(this), binding.llFieldResults, true
        )

        itemBinding.tvFieldName.text = field.fieldName
        itemBinding.tvFieldValue.text = field.value.ifBlank { "(chưa điền)" }
        itemBinding.tvFieldMessage.text = field.message

        val (bgColor, icon) = when (field.status) {
            "OK"      -> Color.parseColor("#E8F5E9") to "✓"
            "ERROR"   -> Color.parseColor("#FFEBEE") to "✗"
            "WARNING" -> Color.parseColor("#FFF3E0") to "⚠"
            "MISSING" -> Color.parseColor("#F3E5F5") to "？"
            else      -> Color.parseColor("#F5F5F5") to "—"
        }
        itemBinding.root.setBackgroundColor(bgColor)
        itemBinding.tvStatusIcon.text = icon

        val iconColor = when (field.status) {
            "OK"      -> Color.parseColor("#2E7D32")
            "ERROR"   -> Color.parseColor("#C62828")
            "WARNING" -> Color.parseColor("#E65100")
            "MISSING" -> Color.parseColor("#6A1B9A")
            else      -> Color.GRAY
        }
        itemBinding.tvStatusIcon.setTextColor(iconColor)

        if (field.message.isBlank() || field.status == "OK") {
            itemBinding.tvFieldMessage.visibility = View.GONE
        }
    }

    private fun clearResults() {
        binding.layoutResults.visibility = View.GONE
        binding.tvStatus.text = "AI đang phân tích biểu mẫu..."
        binding.tvStatus.setTextColor(Color.BLACK)
        binding.llFieldResults.removeAllViews()
    }
}
