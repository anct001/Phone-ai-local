package com.phoneai.formchecker.ui

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.phoneai.formchecker.api.ClaudeApiService
import com.phoneai.formchecker.api.FieldResult
import com.phoneai.formchecker.api.FormAnalysisResult
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
    private val apiService = ClaudeApiService()

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
        binding.tvStatus.text = "AIが帳票を解析中..."
        binding.btnRetry.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    apiService.analyzeForm(file)
                }
                displayResults(result)
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "エラーが発生しました"
                binding.tvStatus.setTextColor(Color.RED)
                binding.btnRetry.isEnabled = true
                Toast.makeText(this@ResultActivity, "解析エラー: ${e.message}", Toast.LENGTH_LONG).show()
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
            "PASS" -> Color.parseColor("#2E7D32") to "✓ 合格"
            "FAIL" -> Color.parseColor("#C62828") to "✗ 不合格"
            else   -> Color.parseColor("#E65100") to "⚠ 要確認"
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
        binding.tvFieldSummary.text = "合計 ${result.fields.size} 項目：OK $okCount / エラー $errCount / 警告 $warnCount / 未記入 $missingCount"
    }

    private fun addFieldRow(field: FieldResult) {
        val itemBinding = ItemFieldResultBinding.inflate(
            LayoutInflater.from(this), binding.llFieldResults, true
        )

        itemBinding.tvFieldName.text = field.fieldName
        itemBinding.tvFieldValue.text = field.value.ifBlank { "（未記入）" }
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
        binding.tvStatus.text = "AIが帳票を解析中..."
        binding.tvStatus.setTextColor(Color.BLACK)
        binding.llFieldResults.removeAllViews()
    }
}
