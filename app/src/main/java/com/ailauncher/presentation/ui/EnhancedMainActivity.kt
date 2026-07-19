package com.ailauncher.presentation.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.ailauncher.AILauncherApplication
import com.ailauncher.domain.model.AppInfo
import com.ailauncher.infrastructure.config.WallpaperConfigManager
import com.ailauncher.presentation.viewmodel.ChatMessage
import com.ailauncher.presentation.viewmodel.EnhancedMainUiState
import com.ailauncher.presentation.viewmodel.EnhancedMainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EnhancedMainActivity : ComponentActivity() {
    
    private val speechPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onSpeechPermissionGranted()
        } else {
            viewModel.onSpeechPermissionDenied()
        }
    }
    
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val transcript = data?.getStringArrayListExtra("android.speech.extra.RESULTS")?.firstOrNull()
            if (transcript != null) {
                viewModel.onSpeechResult(transcript)
            }
        }
    }
    
    private lateinit var viewModel: EnhancedMainViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val application = AILauncherApplication.instance
        viewModel = EnhancedMainViewModel(
            application = application,
            processCommandUseCase = application.provideProcessCommandUseCase(),
            launchAppUseCase = application.provideLaunchAppUseCase(),
            appDiscovery = application.appDiscovery
        )
        
        setContent {
            EnhancedAILauncherApp(
                viewModel = viewModel,
                onGrantSpeechPermission = {
                    speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onLaunchSpeechRecognition = {
                    startSpeechRecognition(speechRecognitionLauncher)
                }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        val hasPermission = ContextCompat.checkSelfPermission(
            this, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.onSpeechPermissionGranted()
        }
    }
    
    private fun startSpeechRecognition(launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val intent = Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
            putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form")
            putExtra("android.speech.extra.PROMPT", "说出您的命令...")
            putExtra("android.speech.extra.LANGUAGE", "zh-CN")
        }
        launcher.launch(intent)
    }
    
    override fun onBackPressed() {
        android.util.Log.d("EnhancedMainActivity", "onBackPressed called - staying on home")
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        android.util.Log.d("EnhancedMainActivity", "onKeyDown: keyCode=$keyCode")
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                android.util.Log.d("EnhancedMainActivity", "KEYCODE_BACK intercepted")
                true
            }
            KeyEvent.KEYCODE_HOME -> {
                super.onKeyDown(keyCode, event)
            }
            187 -> {
                super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@Composable
fun EnhancedAILauncherApp(
    viewModel: EnhancedMainViewModel,
    onGrantSpeechPermission: () -> Unit,
    onLaunchSpeechRecognition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel::uiState
    val context = androidx.compose.ui.platform.LocalContext.current
    val wallpaperManager = remember { WallpaperConfigManager(context) }
    
    // Read wallpaper configuration
    val backgroundType = wallpaperManager.backgroundType
    val backgroundColor = wallpaperManager.getBackgroundColor()
    val gradientStartColor = wallpaperManager.getGradientStartColor()
    val gradientEndColor = wallpaperManager.getGradientEndColor()
    val imageUri = wallpaperManager.imageUri
    val imageOverlayColor = wallpaperManager.getImageOverlayColor()
    val imageOverlayOpacity = wallpaperManager.imageOverlayOpacity
    
    if (uiState.showSpeechPermissionDialog) {
        SpeechPermissionDialog(
            onDismiss = { viewModel.onSpeechPermissionDenied() },
            onGrant = onGrantSpeechPermission
        )
    }
    
    MaterialTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            // Background layer
            when (backgroundType) {
                WallpaperConfigManager.BackgroundType.COLOR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                    )
                }
                WallpaperConfigManager.BackgroundType.GRADIENT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(gradientStartColor, gradientEndColor)
                                )
                            )
                    )
                }
                WallpaperConfigManager.BackgroundType.IMAGE -> {
                    if (imageUri != null) {
                        // Image background
                        Image(
                            painter = rememberAsyncImagePainter(model = imageUri),
                            contentDescription = "Wallpaper",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    imageOverlayColor.copy(alpha = imageOverlayOpacity)
                                )
                        )
                    } else {
                        // Fallback if no image set
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(backgroundColor)
                        )
                    }
                }
                WallpaperConfigManager.BackgroundType.TECH -> {
                    TechBackground()
                }
                WallpaperConfigManager.BackgroundType.TECH -> {
                    // Tech-style background with grid and nodes
                    TechBackground()
                }
            }
            MainContent(
                uiState = uiState,
                onCommandSubmit = { command -> viewModel.processCommand(command) },
                onClearResult = { viewModel.clearResult() },
                onClearHistory = { viewModel.clearConversationHistory() },
                onAppClick = { appInfo -> viewModel.launchAppDirectly(appInfo) },
                onMicClick = {
                    if (uiState.hasSpeechPermission) {
                        onLaunchSpeechRecognition()
                    } else {
                        viewModel.requestSpeechPermission()
                    }
                },
                onSettingsClick = {
                    val intent = Intent(context, LlmSettingsActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxSize(),
                isDarkMode = uiState.isDarkMode
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    uiState: EnhancedMainUiState,
    onCommandSubmit: (String) -> Unit,
    onClearResult: () -> Unit,
    onClearHistory: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onMicClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    var commandText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Auto-focus and show keyboard on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.DarkGray
    val bubbleUserBg = if (isDarkMode) Color(0xFF2563EB) else Color(0xFF3B82F6)
    val bubbleAssistantBg = if (isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
    val inputBg = if (isDarkMode) Color(0xFF1F2937) else Color.White
    
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Header with Frequent Apps
        if (uiState.frequentlyUsedApps.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequent Apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Row {
                    Text(
                        text = uiState.currentModelInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryTextColor,
                        modifier = Modifier.alignByBaseline()
                    )
                    if (uiState.conversationHistory.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Chat",
                                tint = secondaryTextColor
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Model Settings",
                            tint = secondaryTextColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show apps in 2 rows of 4
            val row1 = uiState.frequentlyUsedApps.take(4)
            val row2 = uiState.frequentlyUsedApps.drop(4).take(4)
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(row1) { appInfo ->
                        AppIconItem(
                            appInfo = appInfo,
                            onClick = { onAppClick(appInfo) },
                            isDarkMode = isDarkMode
                        )
                    }
                }
                
                if (row2.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(row2) { appInfo ->
                            AppIconItem(
                                appInfo = appInfo,
                                onClick = { onAppClick(appInfo) },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Input row - at bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                enabled = !uiState.isProcessing,
                placeholder = { Text("Type a command...", color = secondaryTextColor) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = {
                        if (commandText.isNotBlank()) {
                            onCommandSubmit(commandText)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onCommandSubmit(commandText)
                    }
                },
                enabled = commandText.isNotBlank() && !uiState.isProcessing
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (commandText.isNotBlank() && !uiState.isProcessing)
                        MaterialTheme.colorScheme.primary
                    else
                        secondaryTextColor
                )
            }
            
            IconButton(onClick = onMicClick) {
                Icon(
                    imageVector = if (uiState.hasSpeechPermission) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (uiState.hasSpeechPermission) "Voice Input" else "Mic Permission Required",
                    tint = if (uiState.hasSpeechPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
        
        // Result display (above input)
        if (uiState.result != null) {
            val result = uiState.result
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (result.success) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFF44336).copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = if (result.success) "✓ Success" else "✗ Failed",
                        color = if (result.success) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.message,
                        color = textColor,
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        // Loading animation (above input)
        if (uiState.isProcessing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Thinking...",
                    color = secondaryTextColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isUser: Boolean,
    bubbleColor: Color,
    textColor: Color
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                fontSize = 14.sp
            )
        }
        
        Text(
            text = timeFormat.format(Date(message.timestamp)),
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun LoadingBubble(isDarkMode: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    val bgColor = if (isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .alpha(alpha),
                strokeWidth = 2.dp,
                color = if (isDarkMode) Color.LightGray else Color.DarkGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thinking...",
                fontSize = 14.sp,
                color = if (isDarkMode) Color.LightGray else Color.DarkGray,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

@Composable
fun AppIconItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    android.util.Log.d("AppIconItem", "Rendering app: ${appInfo.name}")
    
    Column(
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }
            .width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val icon = try {
            packageManager.getApplicationIcon(appInfo.packageName)
        } catch (e: Exception) {
            null
        }
        
        if (icon != null) {
            val bitmap = drawableToBitmap(icon, 48, 48)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = appInfo.name,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                FallbackIcon(appInfo, isDarkMode)
            }
        } else {
            FallbackIcon(appInfo, isDarkMode)
        }
        
        Text(
            text = appInfo.name.take(6),
            fontSize = 10.sp,
            color = if (isDarkMode) Color.LightGray else Color.DarkGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FallbackIcon(appInfo: AppInfo, isDarkMode: Boolean) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                if (isDarkMode) Color.Gray else Color.LightGray,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = appInfo.name.first().toString(),
            color = if (isDarkMode) Color.White else Color.Black
        )
    }
}

private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap? {
    return try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SpeechPermissionDialog(
    onDismiss: () -> Unit,
    onGrant: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Microphone Permission Required") },
        text = { Text("AI Launcher needs microphone access for voice commands.") },
        confirmButton = {
            Button(onClick = onGrant) { Text("Grant") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Tech-style background with grid, nodes, and connecting lines
 */
@Composable
fun TechBackground() {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gridSizePx = with(density) { 40.dp.toPx() }
    val nodeRadiusOuter = with(density) { 12.dp.toPx() }
    val nodeRadiusMid = with(density) { 6.dp.toPx() }
    val nodeRadiusCore = with(density) { 3.dp.toPx() }
    
    val backgroundColor = Color(0xFF0A0E27)
    val gridColor = Color(0xFF1E3A5F).copy(alpha = 0.3f)
    val nodeColor = Color(0xFF00D9FF)
    val lineColor = Color(0xFF00D9FF).copy(alpha = 0.2f)
    
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Draw background
        drawRect(color = backgroundColor)
        
        // Draw grid lines
        var x = 0f
        while (x < w) {
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, h),
                strokeWidth = 1f
            )
            x += gridSizePx
        }
        var y = 0f
        while (y < h) {
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(w, y),
                strokeWidth = 1f
            )
            y += gridSizePx
        }
        
        // Node positions
        val nodes = listOf(
            androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.25f),
            androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.15f),
            androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.55f),
            androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.70f),
            androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.45f),
            androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.80f),
            androidx.compose.ui.geometry.Offset(w * 0.10f, h * 0.50f)
        )
        
        // Draw connections between nearby nodes
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val dx = nodes[j].x - nodes[i].x
                val dy = nodes[j].y - nodes[i].y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < w * 0.45f) {
                    drawLine(
                        color = lineColor,
                        start = nodes[i],
                        end = nodes[j],
                        strokeWidth = 2f
                    )
                }
            }
        }
        
        // Draw nodes with glow effect
        nodes.forEach { node ->
            drawCircle(
                color = nodeColor.copy(alpha = 0.15f),
                radius = nodeRadiusOuter,
                center = node
            )
            drawCircle(
                color = nodeColor.copy(alpha = 0.4f),
                radius = nodeRadiusMid,
                center = node
            )
            drawCircle(
                color = nodeColor,
                radius = nodeRadiusCore,
                center = node
            )
        }
    }
}
