package com.ailauncher.domain.service

import com.ailauncher.domain.model.ExecutionResult
import com.ailauncher.domain.model.UserCommand

/**
 * Domain interface for processing user commands.
 *
 * This service coordinates the command processing pipeline, from parsing
 * to execution, while maintaining separation of concerns.
 */
interface CommandProcessor {
    
    /**
     * Processes a user command and returns the execution result.
     *
     * This is the main entry point for command processing in the domain layer.
     * The implementation should handle command routing, validation, and execution.
     *
     * @param command The [UserCommand] to process
     * @return [ExecutionResult] representing the outcome
     */
    suspend fun process(command: UserCommand): ExecutionResult
}