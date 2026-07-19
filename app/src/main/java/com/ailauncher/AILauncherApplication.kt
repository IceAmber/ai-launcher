package com.ailauncher

import android.app.Application
import com.ailauncher.infrastructure.llm.LlmClient
import com.ailauncher.infrastructure.llm.LlmConfigManager
import com.ailauncher.infrastructure.repository.AndroidAppDiscovery
import com.ailauncher.infrastructure.service.LLMCommandProcessor

class AILauncherApplication : Application() {
    
    companion object {
        lateinit var instance: AILauncherApplication
            private set
    }
    
    lateinit var appDiscovery: AndroidAppDiscovery
        private set
    
    lateinit var configManager: LlmConfigManager
        private set
    
    private var currentLlmClient: LlmClient? = null
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        appDiscovery = AndroidAppDiscovery(this)
        configManager = LlmConfigManager(this)
    }
    
    /**
     * 获取当前 LLM 客户端（根据配置动态创建）
     */
    fun getLlmClient(): LlmClient {
        // 每次获取时检查配置是否变化，如果变化则重新创建
        val client = currentLlmClient
        if (client == null || !isClientMatchingConfig(client)) {
            currentLlmClient = configManager.createClient()
        }
        return currentLlmClient!!
    }
    
    /**
     * 刷新 LLM 客户端（配置变更后调用）
     */
    fun refreshLlmClient() {
        currentLlmClient = null
    }
    
    private fun isClientMatchingConfig(client: LlmClient): Boolean {
        return when (configManager.currentProvider.value) {
            LlmConfigManager.Provider.CLOUD -> client is com.ailauncher.infrastructure.llm.QwenApiClient
            LlmConfigManager.Provider.MLC -> client is com.ailauncher.infrastructure.llm.MlcLlmClient
        }
    }
    
    fun provideProcessCommandUseCase() = 
        com.ailauncher.application.usecase.ProcessCommandUseCase(
            LLMCommandProcessor(appDiscovery, getLlmClient(), instance)
        )
    
    fun provideLaunchAppUseCase() = 
        com.ailauncher.application.usecase.LaunchAppUseCase(
            com.ailauncher.infrastructure.service.AndroidAppLauncher(this, appDiscovery)
        )
}