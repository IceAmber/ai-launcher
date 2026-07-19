package com.ailauncher.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ailauncher.AILauncherApplication
import com.ailauncher.presentation.viewmodel.MainUiState
import com.ailauncher.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AILauncherApp()
        }
    }
}

@Composable
fun AILauncherApp(
    modifier: Modifier = Modifier
) {
    val application = AILauncherApplication.instance
    val viewModel = MainViewModel(
        processCommandUseCase = application.provideProcessCommandUseCase(),
        launchAppUseCase = application.provideLaunchAppUseCase()
    )
    val uiState = viewModel.uiState
    
    MaterialTheme {
        Scaffold(
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            MainScreen(
                uiState = uiState.value,
                onCommandSubmit = { command ->
                    viewModel.processCommand(command)
                },
                onClearResult = {
                    viewModel.clearResult()
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onCommandSubmit: (String) -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandText by remember { mutableStateOf("") }
    val focusRequester = FocusRequester()
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TextField(
            value = commandText,
            onValueChange = { commandText = it },
            enabled = !uiState.isProcessing,
            label = { Text("Enter command (e.g., 'open WeChat')") },
            placeholder = { Text("open WeChat") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (commandText.isNotBlank()) {
                        onCommandSubmit(commandText)
                        commandText = ""
                    }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .focusRequester(focusRequester)
                .padding(16.dp)
        )
        
        if (uiState.isProcessing) {
            Text(
                text = "Processing...",
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        } else if (uiState.result != null) {
            val resultText = if (uiState.result.success) {
                "✅ ${uiState.result.message}"
            } else {
                "❌ ${uiState.result.message}"
            }
            
            Text(
                text = resultText,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                color = if (uiState.result.success) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}