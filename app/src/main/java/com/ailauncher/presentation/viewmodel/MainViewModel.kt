package com.ailauncher.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ailauncher.application.usecase.LaunchAppUseCase
import com.ailauncher.application.usecase.ProcessCommandUseCase
import com.ailauncher.domain.model.ExecutionResult
import com.ailauncher.domain.model.UserCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the main launcher screen.
 *
 * This ViewModel manages the state and business logic for the main launcher UI,
 * coordinating between use cases and providing observable state to the UI.
 */
class MainViewModel(
    private val processCommandUseCase: ProcessCommandUseCase,
    private val launchAppUseCase: LaunchAppUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState
    
    fun processCommand(commandText: String) {
        if (commandText.isBlank()) return
        
        _uiState.value = MainUiState(isProcessing = true, result = null)
        
        viewModelScope.launch {
            try {
                val userCommand = UserCommand(commandText)
                val result = processCommandUseCase.execute(userCommand)
                
                if (result.success) {
                    // Check if we have appInfo in metadata
                    val appInfo = result.metadata["appInfo"] as? com.ailauncher.domain.model.AppInfo
                    
                    if (appInfo != null) {
                        // Launch the app
                        val launchResult = launchAppUseCase.execute(appInfo)
                        _uiState.value = MainUiState(
                            isProcessing = false,
                            result = launchResult
                        )
                    } else {
                        _uiState.value = MainUiState(
                            isProcessing = false,
                            result = result
                        )
                    }
                } else {
                    _uiState.value = MainUiState(
                        isProcessing = false,
                        result = result
                    )
                }
            } catch (e: Exception) {
                _uiState.value = MainUiState(
                    isProcessing = false,
                    result = ExecutionResult.error("Error processing command: ${e.message}")
                )
            }
        }
    }
    
    fun clearResult() {
        _uiState.value = MainUiState(result = null)
    }
}