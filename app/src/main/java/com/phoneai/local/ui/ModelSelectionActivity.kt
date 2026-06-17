package com.phoneai.local.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoneai.local.databinding.ActivityModelSelectionBinding
import kotlinx.coroutines.launch

/**
 * Lets the user browse the model catalog, see how each model fits their device,
 * download/delete models, and pick one to chat with.
 *
 * Returns the chosen model id via [RESULT_MODEL_ID].
 */
class ModelSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelSelectionBinding
    private val viewModel: ModelSelectionViewModel by viewModels()
    private lateinit var adapter: ModelAdapter

    companion object {
        const val RESULT_MODEL_ID = "result_model_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ModelAdapter(
            onDownload = { viewModel.download(it.config) },
            onCancel   = { viewModel.cancelDownload(it.config) },
            onUse      = { selectModel(it) },
            onDelete   = { confirmDelete(it) }
        )
        binding.rvModels.layoutManager = LinearLayoutManager(this)
        binding.rvModels.adapter = adapter

        lifecycleScope.launch {
            viewModel.state.collect { state ->
                adapter.submitList(state.items)
                binding.tvDeviceInfo.text = buildString {
                    append("Thiết bị: RAM ${state.totalRamMb / 1024} GB")
                    append(" · còn trống ~${state.availableRamMb} MB")
                    append("\nĐộ phù hợp của mỗi model được đánh giá theo RAM hiện có.")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()   // recheck downloaded files / free RAM
    }

    private fun selectModel(item: ModelItem) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(RESULT_MODEL_ID, item.config.id)
        })
        finish()
    }

    private fun confirmDelete(item: ModelItem) {
        AlertDialog.Builder(this)
            .setTitle("Xoá model?")
            .setMessage("Xoá ${item.config.displayName} ${item.config.quant} " +
                    "(${"%.1f".format(item.config.sizeGb)} GB) khỏi thiết bị?")
            .setPositiveButton("Xoá") { _, _ -> viewModel.deleteModel(item.config) }
            .setNegativeButton("Huỷ", null)
            .show()
    }
}
