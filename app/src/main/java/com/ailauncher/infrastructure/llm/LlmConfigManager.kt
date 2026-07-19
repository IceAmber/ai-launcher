package com.ailauncher.infrastructure.llm

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * LLM 模型配置管理
 * 支持云端和本地模型切换
 */
class LlmConfigManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 模型提供商类型
     */
    enum class Provider {
        CLOUD,      // 云端模型（通义千问/DeepSeek）
        MLC         // MLC LLM 本地模型（内置）
    }
    
    // 当前选中的提供商
    private val _currentProvider = MutableStateFlow(getSavedProvider())
    val currentProvider: StateFlow<Provider> = _currentProvider
    
    // 云端配置
    var cloudBaseUrl: String
        get() = prefs.getString(KEY_CLOUD_BASE_URL, DEFAULT_CLOUD_BASE_URL) ?: DEFAULT_CLOUD_BASE_URL
        set(value) = prefs.edit().putString(KEY_CLOUD_BASE_URL, value).apply()
    
    var cloudApiKey: String
        get() = prefs.getString(KEY_CLOUD_API_KEY, DEFAULT_CLOUD_API_KEY) ?: DEFAULT_CLOUD_API_KEY
        set(value) = prefs.edit().putString(KEY_CLOUD_API_KEY, value).apply()
    
    // 本地配置
    var localBaseUrl: String
        get() = prefs.getString(KEY_LOCAL_BASE_URL, DEFAULT_LOCAL_BASE_URL) ?: DEFAULT_LOCAL_BASE_URL
        set(value) = prefs.edit().putString(KEY_LOCAL_BASE_URL, value).apply()
    
    var localModelName: String
        get() = prefs.getString(KEY_LOCAL_MODEL_NAME, DEFAULT_LOCAL_MODEL) ?: DEFAULT_LOCAL_MODEL
        set(value) = prefs.edit().putString(KEY_LOCAL_MODEL_NAME, value).apply()
    
    /**
     * 切换模型提供商
     */
    fun switchProvider(provider: Provider) {
        prefs.edit().putString(KEY_CURRENT_PROVIDER, provider.name).apply()
        _currentProvider.value = provider
    }
    
    /**
     * 获取保存的提供商
     */
    private fun getSavedProvider(): Provider {
        val providerName = prefs.getString(KEY_CURRENT_PROVIDER, Provider.CLOUD.name)
        return try {
            Provider.valueOf(providerName ?: Provider.CLOUD.name)
        } catch (e: Exception) {
            Provider.CLOUD
        }
    }
    
    /**
     * 创建对应的 LLM 客户端
     */
    fun createClient(): LlmClient {
        return when (_currentProvider.value) {
            Provider.CLOUD -> QwenApiClient(
                baseUrl = cloudBaseUrl,
                apiKey = cloudApiKey
            )
            Provider.MLC -> MlcLlmClient(
                context = context,
                modelPath = getModelPath()
            )
        }
    }
    
    /**
     * 获取 MLC 模型路径
     */
    fun getModelPath(): String {
        return File(context.filesDir, "models/${ModelDownloadManager.DEFAULT_MODEL_NAME}").absolutePath
    }
    
    /**
     * 检查 MLC 模型是否已下载
     */
    fun isMlcModelDownloaded(): Boolean {
        return ModelDownloadManager(context).isModelDownloaded()
    }
    
    /**
     * 获取当前配置摘要
     */
    fun getConfigSummary(): String {
        return when (_currentProvider.value) {
            Provider.CLOUD -> "Cloud Model: $cloudBaseUrl"
            Provider.MLC -> "MLC LLM: ${ModelDownloadManager.DEFAULT_MODEL_NAME}"
        }
    }
    
    companion object {
        private const val PREFS_NAME = "llm_config"
        
        private const val KEY_CURRENT_PROVIDER = "current_provider"
        private const val KEY_CLOUD_BASE_URL = "cloud_base_url"
        private const val KEY_CLOUD_API_KEY = "cloud_api_key"
        private const val KEY_LOCAL_BASE_URL = "local_base_url"
        private const val KEY_LOCAL_MODEL_NAME = "local_model_name"
        
        private const val DEFAULT_CLOUD_BASE_URL = ""
        private const val DEFAULT_CLOUD_API_KEY = ""
        private const val DEFAULT_LOCAL_BASE_URL = "http://localhost:11434"
        private const val DEFAULT_LOCAL_MODEL = "qwen2.5:7b"
    }
}
