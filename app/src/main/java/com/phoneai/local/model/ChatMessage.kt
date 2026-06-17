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
 * Formats conversation history into Gemma 3 chat template:
 * <start_of_turn>user\n...<end_of_turn>\n<start_of_turn>model\n
 */
fun List<ChatMessage>.toGemma3Prompt(systemPrompt: String? = null): String {
    val sb = StringBuilder()

    if (!systemPrompt.isNullOrBlank()) {
        sb.append("<start_of_turn>system\n$systemPrompt<end_of_turn>\n")
    }

    for (msg in this) {
        val role = when (msg.role) {
            ChatMessage.Role.USER      -> "user"
            ChatMessage.Role.ASSISTANT -> "model"
            ChatMessage.Role.SYSTEM    -> "system"
        }
        sb.append("<start_of_turn>$role\n${msg.content}<end_of_turn>\n")
    }

    sb.append("<start_of_turn>model\n")
    return sb.toString()
}
