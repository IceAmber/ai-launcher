package com.ailauncher.domain.service

import com.ailauncher.domain.model.AppInfo
import com.ailauncher.domain.model.ExecutionResult

/**
 * Domain interface for launching applications.
 *
 * This interface defines the contract for application launching functionality
 * without any Android-specific dependencies.
 */
interface AppLauncher {
    
    /**
     * Launches an application.
     *
     * @param appInfo The [AppInfo] representing the application to launch
     * @return [ExecutionResult] indicating success or failure
     */
    fun launchApp(appInfo: AppInfo): ExecutionResult
}