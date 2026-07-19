package com.ailauncher.domain.model

/**
 * Represents an installed application on the device.
 *
 * This is a pure domain model with no Android dependencies.
 *
 * @property name The display name of the application
 * @property packageName The unique package identifier
 * @property isSystemApp Whether this is a system application
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean = false
)