package com.ailauncher.presentation.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ailauncher.AILauncherApplication
import com.ailauncher.application.usecase.LaunchAppUseCase
import com.ailauncher.application.usecase.ProcessCommandUseCase
import com.ailauncher.domain.model.AppInfo
import com.ailauncher.domain.model.ExecutionResult
import com.ailauncher.domain.model.UserCommand
import com.ailauncher.infrastructure.llm.LlmConfigManager
import com.ailauncher.infrastructure.repository.AndroidAppDiscovery
import kotlinx.coroutines.launch

class EnhancedMainViewModel(
    application: Application,
    private val processCommandUseCase: ProcessCommandUseCase,
    private val launchAppUseCase: LaunchAppUseCase,
    private val appDiscovery: AndroidAppDiscovery
) : AndroidViewModel(application) {
    
    var uiState by mutableStateOf(EnhancedMainUiState())
        private set
    
    private val context = application.applicationContext
    private val app = application as AILauncherApplication
    
    init {
        loadFrequentlyUsedApps()
        checkSpeechPermission()
        detectSystemTheme()
        loadCurrentModelInfo()
    }
    
    /**
     * 加载当前模型信息
     */
    private fun loadCurrentModelInfo() {
        val configManager = app.configManager
        val currentModel = when (configManager.currentProvider.value) {
            LlmConfigManager.Provider.CLOUD -> "Cloud Model"
            LlmConfigManager.Provider.MLC -> "MLC Model"
        }
        uiState = uiState.copy(currentModelInfo = currentModel)
    }
    
    /**
     * 刷新 LLM 客户端（配置变更后调用）
     */
    fun refreshLlmClient() {
        app.refreshLlmClient()
        loadCurrentModelInfo()
    }
    
    fun processCommand(commandText: String) {
        if (commandText.isBlank()) return
        
        // 添加到对话历史
        val userMessage = ChatMessage(
            role = "user",
            content = commandText,
            timestamp = System.currentTimeMillis()
        )
        uiState = uiState.copy(
            isProcessing = true,
            result = null,
            conversationHistory = uiState.conversationHistory + userMessage
        )
        
        viewModelScope.launch {
            try {
                val userCommand = UserCommand(commandText)
                val result = processCommandUseCase.execute(userCommand)
                
                // 添加助手回复到对话历史
                val assistantMessage = ChatMessage(
                    role = "assistant",
                    content = result.message,
                    timestamp = System.currentTimeMillis()
                )
                
                if (result.success) {
                    val appInfo = result.metadata["appInfo"] as? AppInfo
                    
                    if (appInfo != null) {
                        val launchResult = launchAppUseCase.execute(appInfo)
                        uiState = uiState.copy(
                            isProcessing = false,
                            result = launchResult,
                            conversationHistory = uiState.conversationHistory + assistantMessage.copy(
                                content = launchResult.message
                            )
                        )
                    } else {
                        uiState = uiState.copy(
                            isProcessing = false,
                            result = result,
                            conversationHistory = uiState.conversationHistory + assistantMessage
                        )
                    }
                } else {
                    uiState = uiState.copy(
                        isProcessing = false,
                        result = result,
                        conversationHistory = uiState.conversationHistory + assistantMessage
                    )
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    role = "assistant",
                    content = "Error: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )
                uiState = uiState.copy(
                    isProcessing = false,
                    result = ExecutionResult.error("Error processing command: ${e.message}"),
                    conversationHistory = uiState.conversationHistory + errorMessage
                )
            }
        }
    }
    
    fun clearResult() {
        uiState = uiState.copy(result = null)
    }
    
    fun clearConversationHistory() {
        uiState = uiState.copy(conversationHistory = emptyList())
    }
    
    fun launchAppDirectly(appInfo: AppInfo) {
        viewModelScope.launch {
            try {
                launchAppUseCase.execute(appInfo)
                // 不显示结果，启动应用是正常操作
            } catch (e: Exception) {
                // 只在失败时显示错误
                uiState = uiState.copy(
                    result = ExecutionResult.error("Failed to launch ${appInfo.name}: ${e.message}")
                )
            }
        }
    }
    
    fun requestSpeechPermission() {
        uiState = uiState.copy(showSpeechPermissionDialog = true)
    }
    
    fun onSpeechPermissionGranted() {
        uiState = uiState.copy(showSpeechPermissionDialog = false, hasSpeechPermission = true)
    }
    
    fun onSpeechPermissionDenied() {
        uiState = uiState.copy(showSpeechPermissionDialog = false, hasSpeechPermission = false)
    }
    
    fun onSpeechResult(transcript: String) {
        processCommand(transcript)
    }
    
    private fun loadFrequentlyUsedApps() {
        val allApps = appDiscovery.discoverApps()
        android.util.Log.d("EnhancedMainViewModel", "Discovered ${allApps.size} apps")
        // Show up to 8 apps in 2 rows of 4
        val frequentApps = allApps.take(8)
        android.util.Log.d("EnhancedMainViewModel", "Frequent apps: ${frequentApps.map { it.name }}")
        uiState = uiState.copy(frequentlyUsedApps = frequentApps)
    }
    
    private fun checkSpeechPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        uiState = uiState.copy(hasSpeechPermission = hasPermission)
    }
    
    private fun detectSystemTheme() {
        val uiMode = context.resources.configuration.uiMode
        val isDarkMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        uiState = uiState.copy(isDarkMode = isDarkMode)
    }
}

data class EnhancedMainUiState(
    val isProcessing: Boolean = false,
    val result: ExecutionResult? = null,
    val frequentlyUsedApps: List<AppInfo> = emptyList(),
    val hasSpeechPermission: Boolean = false,
    val showSpeechPermissionDialog: Boolean = false,
    val isDarkMode: Boolean = false,
    val conversationHistory: List<ChatMessage> = emptyList(),
    val currentModelInfo: String = "云端模型"
)

data class ChatMessage(
    val role: String,  // "user" or "assistant"
    val content: String,
    val timestamp: Long
)
