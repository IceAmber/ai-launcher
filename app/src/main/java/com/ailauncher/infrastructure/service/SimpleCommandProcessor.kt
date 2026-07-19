package com.ailauncher.infrastructure.service

import com.ailauncher.domain.model.AppInfo
import com.ailauncher.domain.model.ExecutionResult
import com.ailauncher.domain.model.UserCommand
import com.ailauncher.domain.repository.AppDiscovery
import com.ailauncher.domain.service.CommandProcessor

/**
 * Simple implementation of [CommandProcessor] that handles basic natural language commands.
 *
 * This processor supports commands like:
 * - "open WeChat"
 * - "launch camera"  
 * - "打开 微信"
 * - "启动 相机"
 */
class SimpleCommandProcessor(
    private val appDiscovery: AppDiscovery
) : CommandProcessor {
    
    override suspend fun process(command: UserCommand): ExecutionResult {
        val commandText = command.text.trim()
        
        if (commandText.isEmpty()) {
            return ExecutionResult.error("Empty command")
        }
        
        // Extract app name from command
        val appName = extractAppName(commandText)
        
        if (appName.isEmpty()) {
            return ExecutionResult.error("Could not extract app name from command")
        }
        
        // Try to find the app
        var appInfo: AppInfo? = null
        
        // First try exact app name match
        appInfo = appDiscovery.findAppByName(appName)
        
        // If not found, try common variations
        if (appInfo == null) {
            appInfo = findAppWithVariations(appName)
        }
        
        return if (appInfo != null) {
            ExecutionResult.success("Found app: ${appInfo.name}", metadata = mapOf("appInfo" to appInfo))
        } else {
            ExecutionResult.error("App not found: $appName")
        }
    }
    
    private fun extractAppName(commandText: String): String {
        // Remove common command prefixes
        var cleanText = commandText.lowercase()
        
        // English prefixes
        cleanText = cleanText.replace(Regex("^(open|launch|start|run)\\s+"), "")
        
        // Chinese prefixes  
        cleanText = cleanText.replace(Regex("^(打开|启动|运行)\\s*"), "")
        
        // Remove any remaining whitespace
        return cleanText.trim()
    }
    
    private fun findAppWithVariations(appName: String): AppInfo? {
        // Try common app name variations
        val variations = listOf(
            appName,
            appName.replace(" ", ""),
            appName.replace("-", ""),
            if (appName.endsWith("app")) appName.dropLast(3) else appName,
            if (appName.endsWith("应用")) appName.dropLast(2) else appName
        )
        
        for (variation in variations) {
            val appInfo = appDiscovery.findAppByName(variation)
            if (appInfo != null) {
                return appInfo
            }
        }
        
        return null
    }
}