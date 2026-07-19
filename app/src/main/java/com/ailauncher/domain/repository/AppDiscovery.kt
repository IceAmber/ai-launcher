package com.ailauncher.domain.repository

import com.ailauncher.domain.model.AppInfo

/**
 * Domain interface for discovering installed applications.
 *
 * This interface defines the contract for application discovery functionality
 * without any Android-specific dependencies.
 */
interface AppDiscovery {
    
    /**
     * Discovers all installed applications on the device.
     *
     * @return List of [AppInfo] representing all installed applications
     */
    fun discoverApps(): List<AppInfo>
    
    /**
     * Finds an application by its display name.
     *
     * This method should implement intelligent matching:
     * 1. Exact match (case-insensitive)
     * 2. Contains match (case-insensitive)
     * 3. Package name match (case-insensitive)
     *
     * @param name The application name to search for
     * @return [AppInfo] if found, null otherwise
     */
    fun findAppByName(name: String): AppInfo?
}