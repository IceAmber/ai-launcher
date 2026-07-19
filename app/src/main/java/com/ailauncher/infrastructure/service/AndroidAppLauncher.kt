package com.ailauncher.infrastructure.service

import android.content.Context
import android.content.Intent
import com.ailauncher.domain.model.AppInfo
import com.ailauncher.domain.model.ExecutionResult
import com.ailauncher.domain.service.AppLauncher
import com.ailauncher.infrastructure.repository.AndroidAppDiscovery

/**
 * Android implementation of [AppLauncher] that uses Android Context to launch apps.
 */
class AndroidAppLauncher(
    private val context: Context,
    private val appDiscovery: AndroidAppDiscovery
) : AppLauncher {
    
    override fun launchApp(appInfo: AppInfo): ExecutionResult {
        return try {
            val launchIntent = appDiscovery.getLaunchIntentForPackage(appInfo.packageName)
            
            if (launchIntent == null) {
                return ExecutionResult.error("No launch intent found for ${appInfo.name}")
            }
            
            // Create a new task to ensure the app opens properly as a launcher
            val intent = Intent(launchIntent).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
            context.startActivity(intent)
            ExecutionResult.success("Launched ${appInfo.name}")
        } catch (e: Exception) {
            ExecutionResult.error("Failed to launch ${appInfo.name}: ${e.message}")
        }
    }
}