package com.ailauncher.infrastructure.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.ailauncher.domain.model.AppInfo
import com.ailauncher.domain.repository.AppDiscovery

/**
 * Android implementation of [AppDiscovery] that uses the Android PackageManager
 * to discover installed applications.
 */
class AndroidAppDiscovery(
    private val context: Context
) : AppDiscovery {
    
    // Store launch intents separately since they're Android-specific
    private val appLaunchIntents = mutableMapOf<String, Intent>()
    
    override fun discoverApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val apps = mutableListOf<AppInfo>()
        appLaunchIntents.clear()
        
        // Get all installed packages
        val packages = packageManager.getInstalledPackages(PackageManager.MATCH_ALL)
        
        for (packageInfo in packages) {
            val packageName = packageInfo.packageName
            
            // Skip system packages that don't have launchable activities
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) continue
            
            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            
            apps.add(
                AppInfo(
                    name = appName,
                    packageName = packageName
                )
            )
            
            // Store the launch intent for later use
            appLaunchIntents[packageName] = launchIntent
        }
        
        return apps
    }
    
    override fun findAppByName(name: String): AppInfo? {
        val allApps = discoverApps()
        return allApps.find { it.name.equals(name, ignoreCase = true) }
    }
    
    /**
     * Gets the launch intent for a package name.
     * This is Android-specific and not part of the domain interface.
     */
    fun getLaunchIntentForPackage(packageName: String): Intent? {
        return appLaunchIntents[packageName]
    }
}