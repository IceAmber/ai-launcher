package com.ailauncher.infrastructure.llm

/**
 * LLM 请求模型
 */
data class LlmRequest(
    val prompt: String,
    val systemPrompt: String? = null
)

/**
 * OpenAI 兼容格式的响应模型
 */
data class OpenAiResponse(
    val id: String?,
    val choices: List<Choice>?,
    val usage: Usage?
)

data class Choice(
    val index: Int?,
    val message: Message?,
    val finish_reason: String?
)

data class Message(
    val role: String?,
    val content: String?
)

data class Usage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)

/**
 * LLM 解析后的命令结果
 */
data class LlmCommandResult(
    val action: String,  // LAUNCH_APP, SET_ALARM, SEND_MESSAGE, QUERY_WEATHER, UNKNOWN
    val target: String?,
    val confidence: Float,
    val message: String
) {
    companion object {
        fun unknown(message: String) = LlmCommandResult(
            action = "UNKNOWN",
            target = null,
            confidence = 0f,
            message = message
        )
    }
}
