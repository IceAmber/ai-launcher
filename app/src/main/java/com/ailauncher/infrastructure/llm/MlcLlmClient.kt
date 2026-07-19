package com.ailauncher.infrastructure.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * MLC LLM 本地模型客户端
 * 使用 MLC LLM 框架在设备端运行大语言模型
 * 
 * TODO: 集成实际的 MLC LLM SDK
 * 1. 从 https://github.com/mlc-ai/mlc-llm 获取预编译的 AAR 文件
 * 2. 将 AAR 文件放入 app/libs/ 目录
 * 3. 在 build.gradle.kts 中添加: implementation(files("libs/mlc4j-release.aar"))
 * 4. 取消注释下方的 ChatModule 相关代码
 */
class MlcLlmClient(
    private val context: Context,
    private val modelPath: String? = null
) : LlmClient {
    
    companion object {
        private const val TAG = "MlcLlmClient"
    }
    
    // TODO: 集成 MLC SDK 后取消注释
    // private var chatModule: ChatModule? = null
    private var isInitialized = false
    private var conversationHistory = mutableListOf<Pair<String, String>>()
    
    /**
     * 初始化 MLC LLM 引擎
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing MLC LLM engine...")
            
            // 检查模型文件是否存在
            val modelDir = modelPath?.let { File(it) }
            if (modelDir == null || !modelDir.exists()) {
                Log.e(TAG, "Model directory not found: $modelPath")
                return@withContext Result.failure(
                    Exception("Model not downloaded. Please download the model first.")
                )
            }
            
            // 检查必要的模型文件
            val requiredFiles = listOf("ndarray-cache.json", "mlc-chat-config.json")
            for (file in requiredFiles) {
                val f = File(modelDir, file)
                if (!f.exists()) {
                    Log.e(TAG, "Required model file missing: $file")
                    return@withContext Result.failure(Exception("Model file missing: $file"))
                }
            }
            
            // 检查参数文件（params_shard_*.bin 或 model.params）
            val hasParams = modelDir.listFiles()?.any { 
                it.name.startsWith("params_shard_") || it.name == "model.params" 
            } ?: false
            if (!hasParams) {
                Log.e(TAG, "No parameter files found (params_shard_*.bin or model.params)")
                return@withContext Result.failure(Exception("Model parameters missing"))
            }
            
            // TODO: 集成 MLC SDK 后取消注释
            // chatModule = ChatModule(context)
            // chatModule?.reload(modelPath)
            
            Log.i(TAG, "MLC LLM engine initialized successfully (stub mode)")
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MLC LLM", e)
            Result.failure(e)
        }
    }
    
    override suspend fun chat(userMessage: String, systemPrompt: String): Result<String> = 
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Chat request: $userMessage")
                
                if (!isInitialized) {
                    val initResult = initialize()
                    if (initResult.isFailure) {
                        return@withContext Result.failure(
                            Exception("Failed to initialize MLC LLM: ${initResult.exceptionOrNull()?.message}")
                        )
                    }
                }
                
                // TODO: 集成 MLC SDK 后取消注释
                // val response = chatModule?.generate(userMessage) ?: ""
                
                // 临时实现：返回stub响应
                val response = """{"action":"UNKNOWN","target":null,"confidence":0.5,"message":"MLC LLM integration pending. Message: $userMessage"}"""
                
                // 更新对话历史
                conversationHistory.add("user" to userMessage)
                conversationHistory.add("assistant" to response)
                
                // 保留最近 10 轮对话
                if (conversationHistory.size > 20) {
                    conversationHistory = conversationHistory.takeLast(20).toMutableList()
                }
                
                Log.d(TAG, "Generated response: $response")
                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Chat failed", e)
                Result.failure(e)
            }
        }
    
    override fun parseCommandResult(content: String): LlmCommandResult {
        return try {
            // 尝试从内容中提取 JSON
            val jsonMatch = Regex("\\{[^}]*\\}").find(content)
            val jsonStr = jsonMatch?.value ?: content
            
            // 使用 Gson 解析
            val gson = com.google.gson.Gson()
            val commandResult = gson.fromJson(jsonStr, LlmCommandResult::class.java)
            commandResult
        } catch (e: Exception) {
            LlmCommandResult(
                action = "UNKNOWN",
                target = null,
                confidence = 0.0f,
                message = "Failed to parse command: ${e.message}"
            )
        }
    }
    
    override suspend fun clearHistory() {
        conversationHistory.clear()
    }
    
    override suspend fun clearCache() {
        // MLC LLM 内部管理缓存
    }
    
    override suspend fun isAvailable(): Boolean {
        return isInitialized && modelPath != null && File(modelPath).exists()
    }
    
    override fun getClientName(): String {
        return "MLC LLM (Local)"
    }
    
    /**
     * 释放资源
     */
    fun release() {
        // TODO: 集成 MLC SDK 后取消注释
        // chatModule?.unload()
        // chatModule = null
        isInitialized = false
    }
}
