package com.phoneai.local.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phoneai.local.llm.InferenceManager
import com.phoneai.local.model.ChatMessage
import com.phoneai.local.model.ModelCatalog
import com.phoneai.local.model.ModelConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage>  = emptyList(),
    val isGenerating: Boolean        = false,
    val modelState: ModelState       = ModelState.NotLoaded,
    val errorMessage: String?        = null
)

sealed class ModelState {
    object NotLoaded   : ModelState()
    object Loading     : ModelState()
    data class Loaded(val config: ModelConfig) : ModelState()
    data class Error(val msg: String)          : ModelState()
}

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    val inference = InferenceManager(app)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    // ── Model loading ─────────────────────────────────────────────────────────

    fun loadModel(config: ModelConfig = ModelCatalog.default) {
        viewModelScope.launch {
            _uiState.update { it.copy(modelState = ModelState.Loading) }
            inference.loadModel(config)
                .onSuccess {
                    _uiState.update { it.copy(modelState = ModelState.Loaded(config)) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(modelState = ModelState.Error(e.message ?: "Unknown error"))
                    }
                }
        }
    }

    // ── Sending a message ─────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isGenerating) return

        val userMsg = ChatMessage(role = ChatMessage.Role.USER, content = text.trim())
        val streamingMsg = ChatMessage(
            role = ChatMessage.Role.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                messages     = it.messages + userMsg + streamingMsg,
                isGenerating = true,
                errorMessage = null
            )
        }

        generateJob = viewModelScope.launch {
            val history = _uiState.value.messages.dropLast(1) // exclude placeholder

            inference.chat(history).collect { token ->
                _uiState.update { state ->
                    val updated = state.messages.toMutableList()
                    val last    = updated.last()
                    updated[updated.lastIndex] = last.copy(content = last.content + token)
                    state.copy(messages = updated)
                }
            }

            // Mark streaming done
            _uiState.update { state ->
                val updated = state.messages.toMutableList()
                updated[updated.lastIndex] = updated.last().copy(isStreaming = false)
                state.copy(messages = updated, isGenerating = false)
            }
        }
    }

    fun stopGeneration() {
        inference.stopGeneration()
        generateJob?.cancel()
        _uiState.update { state ->
            val updated = state.messages.toMutableList()
            if (updated.isNotEmpty()) {
                updated[updated.lastIndex] = updated.last().copy(isStreaming = false)
            }
            state.copy(messages = updated, isGenerating = false)
        }
    }

    fun clearChat() {
        stopGeneration()
        _uiState.update { it.copy(messages = emptyList()) }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    override fun onCleared() {
        super.onCleared()
        inference.unloadModel()
    }
}
