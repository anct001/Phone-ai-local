package com.phoneai.local.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoneai.local.databinding.ActivityChatBinding
import com.phoneai.local.model.ModelConfig
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupInputBar()
        observeState()

        // Auto-load default model on start if file exists
        val defaultConfig = ModelConfig.DEFAULT
        if (viewModel.inference.isModelDownloaded(defaultConfig)) {
            viewModel.loadModel(defaultConfig)
        } else {
            showModelNotFound(defaultConfig)
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

        binding.btnStop.setOnClickListener {
            viewModel.stopGeneration()
        }

        binding.btnClear.setOnClickListener {
            viewModel.clearChat()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.messages) {
                    if (state.messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(state.messages.size - 1)
                    }
                }

                // Input bar visibility
                val modelReady = state.modelState is ModelState.Loaded
                binding.inputLayout.visibility = if (modelReady) View.VISIBLE else View.GONE
                binding.tvModelStatus.visibility = if (modelReady) View.GONE else View.VISIBLE

                // Send / Stop toggle
                binding.btnSend.visibility = if (state.isGenerating) View.GONE else View.VISIBLE
                binding.btnStop.visibility = if (state.isGenerating) View.VISIBLE else View.GONE

                // Status text
                binding.tvModelStatus.text = when (val ms = state.modelState) {
                    is ModelState.NotLoaded -> "Model chưa được tải"
                    is ModelState.Loading   -> "Đang tải model…"
                    is ModelState.Loaded    -> ms.config.displayName
                    is ModelState.Error     -> "Lỗi: ${ms.msg}"
                }

                // Error toast
                state.errorMessage?.let { err ->
                    Toast.makeText(this@ChatActivity, err, Toast.LENGTH_LONG).show()
                    viewModel.dismissError()
                }
            }
        }
    }

    private fun showModelNotFound(config: ModelConfig) {
        val path = viewModel.inference.modelFilePath(config)
        binding.tvModelStatus.text =
            "Chưa có model.\nSao chép file GGUF vào:\n${path.absolutePath}"
        binding.tvModelStatus.visibility = View.VISIBLE
    }
}
