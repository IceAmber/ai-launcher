package com.ailauncher.application.usecase

import com.ailauncher.domain.model.AppInfo
import com.ailauncher.domain.model.ExecutionResult
import com.ailauncher.domain.service.AppLauncher

/**
 * Use case for launching applications.
 *
 * This use case encapsulates the business logic for launching apps,
 * providing a clean interface between the presentation layer and domain services.
 */
class LaunchAppUseCase(
    private val appLauncher: AppLauncher
) {
    
    fun execute(appInfo: AppInfo): ExecutionResult {
        return appLauncher.launchApp(appInfo)
    }
}