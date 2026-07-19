package com.ailauncher.domain.model

/**
 * Represents a user command input.
 *
 * This domain model encapsulates the raw user input along with contextual information
 * that may be needed for command processing.
 *
 * @property text The raw command text entered by the user
 * @property timestamp The time when the command was issued (Unix timestamp in milliseconds)
 * @property userId Optional identifier for multi-user scenarios
 */
data class UserCommand(
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null
)