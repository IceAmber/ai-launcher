package com.ailauncher.application.usecase

import com.ailauncher.domain.model.ExecutionResult
import com.ailauncher.domain.model.UserCommand
import com.ailauncher.domain.service.CommandProcessor

/**
 * Use case for processing natural language commands.
 *
 * This use case encapsulates the business logic for parsing and processing
 * user commands, providing a clean interface between the presentation layer and domain services.
 */
class ProcessCommandUseCase(
    private val commandProcessor: CommandProcessor
) {
    
    suspend fun execute(command: UserCommand): ExecutionResult {
        return commandProcessor.process(command)
    }
}