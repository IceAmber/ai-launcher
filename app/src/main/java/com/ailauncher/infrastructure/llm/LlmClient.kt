package com.ailauncher.infrastructure.llm

/**
 * LLM 客户端通用接口
 * 支持云端和本地模型实现
 */
interface LlmClient {
    /**
     * 发送对话请求
     * @param userMessage 用户消息
     * @param systemPrompt 系统提示
     * @return 包含 AI 回复的 Result
     */
    suspend fun chat(userMessage: String, systemPrompt: String): Result<String>
    
    /**
     * 解析 LLM 返回的 JSON 为命令结果
     */
    fun parseCommandResult(content: String): LlmCommandResult
    
    /**
     * 清空对话历史
     */
    suspend fun clearHistory()
    
    /**
     * 清空缓存
     */
    suspend fun clearCache()
    
    /**
     * 检查连接状态
     * @return true 如果客户端可用
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * 获取客户端名称（用于 UI 显示）
     */
    fun getClientName(): String
}
