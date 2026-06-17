package com.phoneai.local.model

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: Role,
    val content: String,
    val isStreaming: Boolean = false
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
}

/**
 * Builds the correct chat prompt string for each model family. Using the wrong
 * template hurts quality badly, so every supported family has its own format.
 */
object PromptTemplates {

    fun build(family: String, history: List<ChatMessage>, systemPrompt: String?): String =
        when {
            family.startsWith("Gemma")  -> gemma(history, systemPrompt)
            family.startsWith("Qwen")   -> chatML(history, systemPrompt)
            family.startsWith("Llama")  -> llama3(history, systemPrompt)
            family.startsWith("Phi")    -> phi3(history, systemPrompt)
            else                        -> gemma(history, systemPrompt)
        }

    // Gemma 3 — note: Gemma has no separate system role, so we prepend it to
    // the first user turn.
    private fun gemma(history: List<ChatMessage>, sys: String?): String {
        val sb = StringBuilder()
        var sysInjected = sys.isNullOrBlank()
        for (msg in history) {
            val role = if (msg.role == ChatMessage.Role.ASSISTANT) "model" else "user"
            val content = if (!sysInjected && msg.role == ChatMessage.Role.USER) {
                sysInjected = true
                "$sys\n\n${msg.content}"
            } else msg.content
            sb.append("<start_of_turn>$role\n$content<end_of_turn>\n")
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    // ChatML — Qwen2.5
    private fun chatML(history: List<ChatMessage>, sys: String?): String {
        val sb = StringBuilder()
        if (!sys.isNullOrBlank()) sb.append("<|im_start|>system\n$sys<|im_end|>\n")
        for (msg in history) {
            val role = if (msg.role == ChatMessage.Role.ASSISTANT) "assistant" else "user"
            sb.append("<|im_start|>$role\n${msg.content}<|im_end|>\n")
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    // Llama 3.x
    private fun llama3(history: List<ChatMessage>, sys: String?): String {
        val sb = StringBuilder("<|begin_of_text|>")
        if (!sys.isNullOrBlank()) {
            sb.append("<|start_header_id|>system<|end_header_id|>\n\n$sys<|eot_id|>")
        }
        for (msg in history) {
            val role = if (msg.role == ChatMessage.Role.ASSISTANT) "assistant" else "user"
            sb.append("<|start_header_id|>$role<|end_header_id|>\n\n${msg.content}<|eot_id|>")
        }
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }

    // Phi-3 / Phi-3.5
    private fun phi3(history: List<ChatMessage>, sys: String?): String {
        val sb = StringBuilder()
        if (!sys.isNullOrBlank()) sb.append("<|system|>\n$sys<|end|>\n")
        for (msg in history) {
            val role = if (msg.role == ChatMessage.Role.ASSISTANT) "assistant" else "user"
            sb.append("<|$role|>\n${msg.content}<|end|>\n")
        }
        sb.append("<|assistant|>\n")
        return sb.toString()
    }
}
