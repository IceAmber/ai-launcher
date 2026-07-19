package com.ailauncher.domain.model

/**
 * Represents the result of executing a user command.
 *
 * This domain model provides a standardized way to communicate command execution outcomes
 * back to the presentation layer.
 *
 * @property success Whether the command execution was successful
 * @property message Human-readable message describing the result
 * @property action Optional action type for UI feedback (e.g., "LAUNCH_APP", "OPEN_SETTINGS")
 * @property metadata Additional context-specific data as key-value pairs
 */
data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val action: String? = null,
    val metadata: Map<String, Any> = emptyMap()
) {
    
    companion object {
        
        /**
         * Creates a successful execution result.
         *
         * @param message Success message
         * @param action Optional action type
         * @param metadata Additional metadata
         * @return [ExecutionResult] with success=true
         */
        fun success(
            message: String,
            action: String? = null,
            metadata: Map<String, Any> = emptyMap()
        ): ExecutionResult {
            return ExecutionResult(
                success = true,
                message = message,
                action = action,
                metadata = metadata
            )
        }
        
        /**
         * Creates a failed execution result.
         *
         * @param message Error message
         * @param action Optional action type
         * @param metadata Additional metadata
         * @return [ExecutionResult] with success=false
         */
        fun error(
            message: String,
            action: String? = null,
            metadata: Map<String, Any> = emptyMap()
        ): ExecutionResult {
            return ExecutionResult(
                success = false,
                message = message,
                action = action,
                metadata = metadata
            )
        }
    }
}