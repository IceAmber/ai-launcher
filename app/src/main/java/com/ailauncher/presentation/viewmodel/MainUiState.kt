package com.ailauncher.presentation.viewmodel

import com.ailauncher.domain.model.ExecutionResult

/**
 * UI state for the main launcher screen.
 *
 * This data class represents the current state of the UI, including
 * processing status and command execution results.
 */
data class MainUiState(
    val isProcessing: Boolean = false,
    val result: ExecutionResult? = null
)