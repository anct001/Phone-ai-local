package com.phoneai.local.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoneai.local.databinding.ActivityChatBinding
import com.phoneai.local.model.ModelCatalog
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter

    private val prefs by lazy { getSharedPreferences("phoneai", MODE_PRIVATE) }

    private val modelPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val id = result.data?.getStringExtra(ModelSelectionActivity.RESULT_MODEL_ID)
            val config = id?.let { ModelCatalog.byId(it) }
            if (config != null) {
                prefs.edit().putString(KEY_LAST_MODEL, config.id).apply()
                viewModel.loadModel(config)
            }
        }
    }

    companion object {
        private const val KEY_LAST_MODEL = "last_model"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupInputBar()
        observeState()

        // Restore last-used model if still present; otherwise open the picker.
        val lastId = prefs.getString(KEY_LAST_MODEL, null)
        val config = lastId?.let { ModelCatalog.byId(it) }
        if (config != null && viewModel.inference.isModelDownloaded(config)) {
            viewModel.loadModel(config)
        } else {
            openModelPicker()
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = this@ChatActivity.adapter
        }
    }

    private fun setupInputBar() {
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etInput.text?.clear()
            }
        }
        binding.btnStop.setOnClickListener { viewModel.stopGeneration() }
        binding.btnClear.setOnClickListener { viewModel.clearChat() }
        binding.btnSwitchModel.setOnClickListener { openModelPicker() }
        binding.btnOpenPicker.setOnClickListener { openModelPicker() }
    }

    private fun openModelPicker() {
        modelPicker.launch(Intent(this, ModelSelectionActivity::class.java))
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.messages) {
                    if (state.messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(state.messages.size - 1)
                    }
                }

                val modelReady = state.modelState is ModelState.Loaded

                // Show chat UI only when a model is loaded
                binding.inputLayout.visibility = if (modelReady) View.VISIBLE else View.GONE
                binding.rvMessages.visibility  = if (modelReady) View.VISIBLE else View.GONE
                binding.emptyState.visibility  = if (modelReady) View.GONE else View.VISIBLE

                // Toolbar title reflects loaded model
                binding.toolbar.title = when (val ms = state.modelState) {
                    is ModelState.Loaded -> "${ms.config.displayName} · ${ms.config.quant}"
                    else -> "Phone AI"
                }

                // Send / Stop toggle
                binding.btnSend.visibility = if (state.isGenerating) View.GONE else View.VISIBLE
                binding.btnStop.visibility = if (state.isGenerating) View.VISIBLE else View.GONE

                // Empty-state status text
                binding.tvModelStatus.text = when (val ms = state.modelState) {
                    is ModelState.NotLoaded -> "Chưa có model nào được tải.\nHãy chọn và tải một model để bắt đầu."
                    is ModelState.Loading   -> "Đang tải model vào bộ nhớ…"
                    is ModelState.Loaded    -> ms.config.displayName
                    is ModelState.Error     -> "Lỗi tải model: ${ms.msg}"
                }
                binding.btnOpenPicker.visibility =
                    if (state.modelState is ModelState.Loading) View.GONE else View.VISIBLE

                state.errorMessage?.let { err ->
                    Toast.makeText(this@ChatActivity, err, Toast.LENGTH_LONG).show()
                    viewModel.dismissError()
                }
            }
        }
    }
}
