package com.ailauncher.infrastructure.llm

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.X509TrustManager

/**
 * LLM API 客户端（云端）
 * 支持多轮对话和指令缓存
 */
class QwenApiClient(
    private val baseUrl: String = "https://65.49.216.139:3000",
    private val apiKey: String = "private_bBuoEBywjBuT4FQcbPqrC5ej"
) : LlmClient {
    // 对话历史（保留最近 10 轮）
    private val conversationHistory = mutableListOf<ChatMessage>()
    private val historyMutex = Mutex()
    private val maxHistorySize = 10

    // 指令缓存（最近 50 条）
    private val commandCache = mutableMapOf<String, LlmCommandResult>()
    private val cacheMutex = Mutex()
    private val maxCacheSize = 50

    @SuppressLint("CustomX509TrustManager")
    private val trustAllCerts = arrayOf<X509TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    @SuppressLint("TrustAllX509TrustManager")
    private fun createUnsafeSSLSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext.socketFactory
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .sslSocketFactory(createUnsafeSSLSocketFactory(), trustAllCerts[0])
        .hostnameVerifier { _, _ -> true }
        .build()
    
    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()
    
    /**
     * 发送多轮对话请求
     */
    override suspend fun chat(
        userMessage: String,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 检查缓存
            cacheMutex.withLock {
                commandCache[userMessage]?.let {
                    return@withContext Result.success(gson.toJson(it))
                }
            }

            // 构建消息列表
            val messages = buildList {
                add(ChatMessage("system", systemPrompt))
                
                // 添加历史对话
                historyMutex.withLock {
                    addAll(conversationHistory)
                }
                
                // 添加当前用户消息
                add(ChatMessage("user", userMessage))
            }

            val requestBody = mapOf("messages" to messages)
            val jsonBody = gson.toJson(requestBody)
            
            val request = Request.Builder()
                .url("$baseUrl/api/chat")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-API-Key", apiKey)
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    val responseBody = response.body?.string() 
                        ?: return@withContext Result.failure(Exception("Empty response"))
                    
                    val aiResponse = gson.fromJson(responseBody, OpenAiResponse::class.java)
                    val content = aiResponse.choices?.firstOrNull()?.message?.content
                        ?: return@withContext Result.failure(Exception("No content in response"))
                    
                    // 更新对话历史
                    historyMutex.withLock {
                        conversationHistory.add(ChatMessage("user", userMessage))
                        conversationHistory.add(ChatMessage("assistant", content))
                        
                        // 保留最近 N 轮对话
                        while (conversationHistory.size > maxHistorySize * 2) {
                            conversationHistory.removeAt(0)
                        }
                    }
                    
                    // 缓存结果
                    val commandResult = parseCommandResult(content)
                    cacheMutex.withLock {
                        commandCache[userMessage] = commandResult
                        
                        // 清理旧缓存
                        if (commandCache.size > maxCacheSize) {
                            val keysToRemove = commandCache.keys.take(commandCache.size - maxCacheSize)
                            keysToRemove.forEach { commandCache.remove(it) }
                        }
                    }
                    
                    Result.success(content)
                }
                401 -> Result.failure(Exception("认证失败：API key 无效"))
                429 -> Result.failure(Exception("请求过于频繁，请稍后再试"))
                else -> {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解析 LLM 返回的 JSON
     */
    override fun parseCommandResult(content: String): LlmCommandResult {
        return try {
            val jsonStr = extractJson(content)
            val jsonObject = JsonParser.parseString(jsonStr).asJsonObject
            
            // 安全提取字段值，处理 JsonNull
            val actionElement = jsonObject.get("action")
            val action = if (actionElement != null && !actionElement.isJsonNull) {
                actionElement.asString
            } else {
                "UNKNOWN"
            }
            
            val targetElement = jsonObject.get("target")
            val target = if (targetElement != null && !targetElement.isJsonNull) {
                targetElement.asString
            } else {
                null
            }
            
            val confidenceElement = jsonObject.get("confidence")
            val confidence = if (confidenceElement != null && !confidenceElement.isJsonNull) {
                confidenceElement.asFloat
            } else {
                0.5f
            }
            
            val messageElement = jsonObject.get("message")
            val message = if (messageElement != null && !messageElement.isJsonNull) {
                messageElement.asString
            } else {
                ""
            }
            
            LlmCommandResult(
                action = action,
                target = target,
                confidence = confidence,
                message = message
            )
        } catch (e: Exception) {
            LlmCommandResult.unknown("无法解析指令: ${e.message}")
        }
    }
    
    /**
     * 从内容中提取 JSON
     */
    private fun extractJson(content: String): String {
        if (content.trim().startsWith("{")) {
            return content.trim()
        }
        
        val jsonPattern = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val match = jsonPattern.find(content)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1)
        }
        
        return content.trim()
    }

    /**
     * 清空对话历史
     */
    override suspend fun clearHistory() {
        historyMutex.withLock {
            conversationHistory.clear()
        }
    }

    /**
     * 清空缓存
     */
    override suspend fun clearCache() {
        cacheMutex.withLock {
            commandCache.clear()
        }
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()
            val response = client.newCall(request).execute()
            response.code == 200
        } catch (e: Exception) {
            false
        }
    }

    override fun getClientName(): String = "云端模型 (Qwen/DeepSeek)"
}

data class ChatMessage(
    val role: String,
    val content: String
)
