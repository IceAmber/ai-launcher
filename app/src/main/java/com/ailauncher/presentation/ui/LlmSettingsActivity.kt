package com.ailauncher.presentation.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ailauncher.infrastructure.config.WallpaperConfigManager
import com.ailauncher.infrastructure.llm.LlmConfigManager
import com.ailauncher.infrastructure.llm.ModelDownloadManager
import kotlinx.coroutines.launch

/**
 * Settings screen with responsive layout:
 * - Phone portrait: Full-screen pages (list → click → detail)
 * - Tablet/landscape: Two-pane layout (left nav + right detail)
 */
class LlmSettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LlmSettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

/**
 * Settings category definition
 */
data class SettingsCategory(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val subtitle: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSettingsScreen(onBack: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTabletOrLandscape = screenWidthDp >= 600 || configuration.screenWidthDp >= configuration.screenHeightDp
    
    // Shared state
    val context = LocalContext.current
    val configManager = remember { LlmConfigManager(context) }
    val wallpaperManager = remember { WallpaperConfigManager(context) }
    val modelDownloadManager = remember { ModelDownloadManager(context) }
    val scope = rememberCoroutineScope()
    
    // Settings categories
    val categories = listOf(
        SettingsCategory("model", "Model", Icons.Default.SmartToy, "AI model configuration"),
        SettingsCategory("wallpaper", "Wallpaper", Icons.Default.Image, "Background settings"),
        SettingsCategory("display", "Display", Icons.Default.Visibility, "Display preferences"),
        SettingsCategory("sound", "Sound", Icons.Default.VolumeUp, "Audio settings"),
        SettingsCategory("about", "About", Icons.Default.Info, "App information")
    )
    
    var selectedCategory by remember { 
        // Phone mode starts at list, tablet/landscape starts at model
        mutableStateOf(if (isTabletOrLandscape) "model" else "list")
    }
    
    // Model settings state
    var currentProvider by remember { mutableStateOf(configManager.currentProvider.value) }
    var cloudBaseUrl by remember { mutableStateOf(configManager.cloudBaseUrl) }
    var cloudApiKey by remember { mutableStateOf(configManager.cloudApiKey) }
    var isModelDownloaded by remember { mutableStateOf(modelDownloadManager.isModelDownloaded()) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    
    // Wallpaper settings state
    var backgroundType by remember { mutableStateOf(wallpaperManager.backgroundType) }
    var backgroundColor by remember { mutableStateOf(wallpaperManager.getBackgroundColor()) }
    var gradientStartColor by remember { mutableStateOf(wallpaperManager.getGradientStartColor()) }
    var gradientEndColor by remember { mutableStateOf(wallpaperManager.getGradientEndColor()) }
    var selectedPresetIndex by remember { mutableStateOf(wallpaperManager.presetGradientIndex) }
    var imageUri by remember { mutableStateOf(wallpaperManager.imageUri?.let { Uri.parse(it) }) }
    var imageOverlayOpacity by remember { mutableStateOf(wallpaperManager.imageOverlayOpacity) }
    var imageOverlayColor by remember { mutableStateOf(wallpaperManager.getImageOverlayColor()) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            wallpaperManager.imageUri = it.toString()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (isTabletOrLandscape) {
            // Two-pane layout for tablet/landscape
            TwoPaneSettingsLayout(
                modifier = Modifier.padding(paddingValues),
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                currentProvider = currentProvider,
                onProviderChange = { provider ->
                    currentProvider = provider
                    configManager.switchProvider(provider)
                },
                cloudBaseUrl = cloudBaseUrl,
                onCloudBaseUrlChange = { 
                    cloudBaseUrl = it
                    configManager.cloudBaseUrl = it
                },
                cloudApiKey = cloudApiKey,
                onCloudApiKeyChange = {
                    cloudApiKey = it
                    configManager.cloudApiKey = it
                },
                isModelDownloaded = isModelDownloaded,
                downloadProgress = downloadProgress,
                isDownloading = isDownloading,
                onDownloadModel = {
                    if (!isModelDownloaded && !isDownloading) {
                        isDownloading = true
                        scope.launch {
                            modelDownloadManager.downloadModel().fold(
                                onSuccess = {
                                    isModelDownloaded = true
                                    isDownloading = false
                                },
                                onFailure = {
                                    isDownloading = false
                                }
                            )
                        }
                        scope.launch {
                            modelDownloadManager.downloadProgress.collect { progress ->
                                downloadProgress = progress.progressPercent
                                if (progress.state == ModelDownloadManager.DownloadState.COMPLETED) {
                                    isModelDownloaded = true
                                    isDownloading = false
                                }
                            }
                        }
                    }
                },
                backgroundType = backgroundType,
                onBackgroundTypeChange = {
                    backgroundType = it
                    wallpaperManager.backgroundType = it
                },
                backgroundColor = backgroundColor,
                onBackgroundColorChange = {
                    backgroundColor = it
                    wallpaperManager.backgroundColor = it.value.toLong().toInt()
                },
                gradientStartColor = gradientStartColor,
                gradientEndColor = gradientEndColor,
                onGradientStartColorChange = {
                    gradientStartColor = it
                    wallpaperManager.gradientStartColor = it.value.toLong().toInt()
                    selectedPresetIndex = -1
                    wallpaperManager.presetGradientIndex = -1
                },
                onGradientEndColorChange = {
                    gradientEndColor = it
                    wallpaperManager.gradientEndColor = it.value.toLong().toInt()
                    selectedPresetIndex = -1
                    wallpaperManager.presetGradientIndex = -1
                },
                selectedPresetIndex = selectedPresetIndex,
                onPresetSelected = { index ->
                    selectedPresetIndex = index
                    val (start, end) = WallpaperConfigManager.presetGradients[index]
                    gradientStartColor = start
                    gradientEndColor = end
                    wallpaperManager.applyPresetGradient(index)
                },
                imageUri = imageUri,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                imageOverlayOpacity = imageOverlayOpacity,
                onOverlayOpacityChange = {
                    imageOverlayOpacity = it
                    wallpaperManager.imageOverlayOpacity = it
                },
                imageOverlayColor = imageOverlayColor,
                onOverlayColorChange = {
                    imageOverlayColor = it
                    wallpaperManager.imageOverlayColor = it.value.toLong().toInt()
                }
            )
        } else {
            // Full-screen pages for phone portrait
            PhoneSettingsLayout(
                modifier = Modifier.padding(paddingValues),
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                currentProvider = currentProvider,
                onProviderChange = { provider ->
                    currentProvider = provider
                    configManager.switchProvider(provider)
                },
                cloudBaseUrl = cloudBaseUrl,
                onCloudBaseUrlChange = { 
                    cloudBaseUrl = it
                    configManager.cloudBaseUrl = it
                },
                cloudApiKey = cloudApiKey,
                onCloudApiKeyChange = {
                    cloudApiKey = it
                    configManager.cloudApiKey = it
                },
                isModelDownloaded = isModelDownloaded,
                downloadProgress = downloadProgress,
                isDownloading = isDownloading,
                onDownloadModel = {
                    if (!isModelDownloaded && !isDownloading) {
                        isDownloading = true
                        scope.launch {
                            modelDownloadManager.downloadModel().fold(
                                onSuccess = {
                                    isModelDownloaded = true
                                    isDownloading = false
                                },
                                onFailure = {
                                    isDownloading = false
                                }
                            )
                        }
                        scope.launch {
                            modelDownloadManager.downloadProgress.collect { progress ->
                                downloadProgress = progress.progressPercent
                                if (progress.state == ModelDownloadManager.DownloadState.COMPLETED) {
                                    isModelDownloaded = true
                                    isDownloading = false
                                }
                            }
                        }
                    }
                },
                backgroundType = backgroundType,
                onBackgroundTypeChange = {
                    backgroundType = it
                    wallpaperManager.backgroundType = it
                },
                backgroundColor = backgroundColor,
                onBackgroundColorChange = {
                    backgroundColor = it
                    wallpaperManager.backgroundColor = it.value.toLong().toInt()
                },
                gradientStartColor = gradientStartColor,
                gradientEndColor = gradientEndColor,
                onGradientStartColorChange = {
                    gradientStartColor = it
                    wallpaperManager.gradientStartColor = it.value.toLong().toInt()
                    selectedPresetIndex = -1
                    wallpaperManager.presetGradientIndex = -1
                },
                onGradientEndColorChange = {
                    gradientEndColor = it
                    wallpaperManager.gradientEndColor = it.value.toLong().toInt()
                    selectedPresetIndex = -1
                    wallpaperManager.presetGradientIndex = -1
                },
                selectedPresetIndex = selectedPresetIndex,
                onPresetSelected = { index ->
                    selectedPresetIndex = index
                    val (start, end) = WallpaperConfigManager.presetGradients[index]
                    gradientStartColor = start
                    gradientEndColor = end
                    wallpaperManager.applyPresetGradient(index)
                },
                imageUri = imageUri,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                imageOverlayOpacity = imageOverlayOpacity,
                onOverlayOpacityChange = {
                    imageOverlayOpacity = it
                    wallpaperManager.imageOverlayOpacity = it
                },
                imageOverlayColor = imageOverlayColor,
                onOverlayColorChange = {
                    imageOverlayColor = it
                    wallpaperManager.imageOverlayColor = it.value.toLong().toInt()
                }
            )
        }
    }
}

/**
 * Two-pane layout for tablet/landscape
 */
@Composable
fun TwoPaneSettingsLayout(
    modifier: Modifier = Modifier,
    categories: List<SettingsCategory>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    currentProvider: LlmConfigManager.Provider,
    onProviderChange: (LlmConfigManager.Provider) -> Unit,
    cloudBaseUrl: String,
    onCloudBaseUrlChange: (String) -> Unit,
    cloudApiKey: String,
    onCloudApiKeyChange: (String) -> Unit,
    isModelDownloaded: Boolean,
    downloadProgress: Float,
    isDownloading: Boolean,
    onDownloadModel: () -> Unit,
    backgroundType: WallpaperConfigManager.BackgroundType,
    onBackgroundTypeChange: (WallpaperConfigManager.BackgroundType) -> Unit,
    backgroundColor: Color,
    onBackgroundColorChange: (Color) -> Unit,
    gradientStartColor: Color,
    gradientEndColor: Color,
    onGradientStartColorChange: (Color) -> Unit,
    onGradientEndColorChange: (Color) -> Unit,
    selectedPresetIndex: Int,
    onPresetSelected: (Int) -> Unit,
    imageUri: Uri?,
    onPickImage: () -> Unit,
    imageOverlayOpacity: Float,
    onOverlayOpacityChange: (Float) -> Unit,
    imageOverlayColor: Color,
    onOverlayColorChange: (Color) -> Unit
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        // Left pane: Navigation list
        SettingsNavigationPane(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
        
        // Divider
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        )
        
        // Right pane: Detail content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (selectedCategory) {
                "model" -> ModelSettingsPane(
                    currentProvider = currentProvider,
                    onProviderChange = onProviderChange,
                    cloudBaseUrl = cloudBaseUrl,
                    onCloudBaseUrlChange = onCloudBaseUrlChange,
                    cloudApiKey = cloudApiKey,
                    onCloudApiKeyChange = onCloudApiKeyChange,
                    isModelDownloaded = isModelDownloaded,
                    downloadProgress = downloadProgress,
                    isDownloading = isDownloading,
                    onDownloadModel = onDownloadModel
                )
                "wallpaper" -> WallpaperSettingsPane(
                    backgroundType = backgroundType,
                    onBackgroundTypeChange = onBackgroundTypeChange,
                    backgroundColor = backgroundColor,
                    onBackgroundColorChange = onBackgroundColorChange,
                    gradientStartColor = gradientStartColor,
                    gradientEndColor = gradientEndColor,
                    onGradientStartColorChange = onGradientStartColorChange,
                    onGradientEndColorChange = onGradientEndColorChange,
                    selectedPresetIndex = selectedPresetIndex,
                    onPresetSelected = onPresetSelected,
                    imageUri = imageUri,
                    onPickImage = onPickImage,
                    imageOverlayOpacity = imageOverlayOpacity,
                    onOverlayOpacityChange = onOverlayOpacityChange,
                    imageOverlayColor = imageOverlayColor,
                    onOverlayColorChange = onOverlayColorChange
                )
                "display" -> DisplaySettingsPane()
                "sound" -> SoundSettingsPane()
                "about" -> AboutSettingsPane()
            }
        }
    }
}

/**
 * Phone layout with full-screen pages
 */
@Composable
fun PhoneSettingsLayout(
    modifier: Modifier = Modifier,
    categories: List<SettingsCategory>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    currentProvider: LlmConfigManager.Provider,
    onProviderChange: (LlmConfigManager.Provider) -> Unit,
    cloudBaseUrl: String,
    onCloudBaseUrlChange: (String) -> Unit,
    cloudApiKey: String,
    onCloudApiKeyChange: (String) -> Unit,
    isModelDownloaded: Boolean,
    downloadProgress: Float,
    isDownloading: Boolean,
    onDownloadModel: () -> Unit,
    backgroundType: WallpaperConfigManager.BackgroundType,
    onBackgroundTypeChange: (WallpaperConfigManager.BackgroundType) -> Unit,
    backgroundColor: Color,
    onBackgroundColorChange: (Color) -> Unit,
    gradientStartColor: Color,
    gradientEndColor: Color,
    onGradientStartColorChange: (Color) -> Unit,
    onGradientEndColorChange: (Color) -> Unit,
    selectedPresetIndex: Int,
    onPresetSelected: (Int) -> Unit,
    imageUri: Uri?,
    onPickImage: () -> Unit,
    imageOverlayOpacity: Float,
    onOverlayOpacityChange: (Float) -> Unit,
    imageOverlayColor: Color,
    onOverlayColorChange: (Color) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (selectedCategory) {
            "list" -> SettingsListPage(
                categories = categories,
                onCategorySelected = onCategorySelected
            )
            "model" -> ModelSettingsPane(
                currentProvider = currentProvider,
                onProviderChange = onProviderChange,
                cloudBaseUrl = cloudBaseUrl,
                onCloudBaseUrlChange = onCloudBaseUrlChange,
                cloudApiKey = cloudApiKey,
                onCloudApiKeyChange = onCloudApiKeyChange,
                isModelDownloaded = isModelDownloaded,
                downloadProgress = downloadProgress,
                isDownloading = isDownloading,
                onDownloadModel = onDownloadModel,
                showBackButton = true,
                onBack = { onCategorySelected("list") }
            )
            "wallpaper" -> WallpaperSettingsPane(
                backgroundType = backgroundType,
                onBackgroundTypeChange = onBackgroundTypeChange,
                backgroundColor = backgroundColor,
                onBackgroundColorChange = onBackgroundColorChange,
                gradientStartColor = gradientStartColor,
                gradientEndColor = gradientEndColor,
                onGradientStartColorChange = onGradientStartColorChange,
                onGradientEndColorChange = onGradientEndColorChange,
                selectedPresetIndex = selectedPresetIndex,
                onPresetSelected = onPresetSelected,
                imageUri = imageUri,
                onPickImage = onPickImage,
                imageOverlayOpacity = imageOverlayOpacity,
                onOverlayOpacityChange = onOverlayOpacityChange,
                imageOverlayColor = imageOverlayColor,
                onOverlayColorChange = onOverlayColorChange,
                showBackButton = true,
                onBack = { onCategorySelected("list") }
            )
            "display" -> DisplaySettingsPane(
                showBackButton = true,
                onBack = { onCategorySelected("list") }
            )
            "sound" -> SoundSettingsPane(
                showBackButton = true,
                onBack = { onCategorySelected("list") }
            )
            "about" -> AboutSettingsPane(
                showBackButton = true,
                onBack = { onCategorySelected("list") }
            )
        }
    }
}

/**
 * Settings list page (phone mode)
 */
@Composable
fun SettingsListPage(
    categories: List<SettingsCategory>,
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        categories.forEach { category ->
            SettingsListItem(
                category = category,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
fun SettingsListItem(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (category.subtitle.isNotEmpty()) {
                Text(
                    text = category.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Left navigation pane (tablet mode)
 */
@Composable
fun SettingsNavigationPane(
    categories: List<SettingsCategory>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category.id == selectedCategory
            NavigationItem(
                category = category,
                isSelected = isSelected,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationItem(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer 
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface
            )
            if (category.subtitle.isNotEmpty()) {
                Text(
                    text = category.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Right detail pane: Model settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsPane(
    currentProvider: LlmConfigManager.Provider,
    onProviderChange: (LlmConfigManager.Provider) -> Unit,
    cloudBaseUrl: String,
    onCloudBaseUrlChange: (String) -> Unit,
    cloudApiKey: String,
    onCloudApiKeyChange: (String) -> Unit,
    isModelDownloaded: Boolean,
    downloadProgress: Float,
    isDownloading: Boolean,
    onDownloadModel: () -> Unit,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back button for phone mode
        if (showBackButton) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }
        
        // Header
        Text(
            text = "Model Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Configure the AI model used for command processing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Provider selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Model Provider",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentProvider == LlmConfigManager.Provider.CLOUD,
                        onClick = { onProviderChange(LlmConfigManager.Provider.CLOUD) },
                        label = { Text("Cloud") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 0.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                    
                    FilterChip(
                        selected = currentProvider == LlmConfigManager.Provider.MLC,
                        onClick = { onProviderChange(LlmConfigManager.Provider.MLC) },
                        label = { Text("Local") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 0.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                }
                
                Text(
                    text = when (currentProvider) {
                        LlmConfigManager.Provider.CLOUD -> "Use cloud API (Qwen/DeepSeek), requires network"
                        LlmConfigManager.Provider.MLC -> "Built-in local model, download when selected"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Provider-specific settings
        when (currentProvider) {
            LlmConfigManager.Provider.MLC -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE0F7FA)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "MLC LLM - Built-in Model",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Model: ${ModelDownloadManager.DEFAULT_MODEL_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (isModelDownloaded) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Model downloaded and ready",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (isDownloading) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Downloading model...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                @Suppress("DEPRECATION")
                                LinearProgressIndicator(
                                    progress = downloadProgress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                text = "Model not downloaded yet (~1.5GB)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Button(
                            onClick = onDownloadModel,
                            enabled = !isModelDownloaded && !isDownloading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when {
                                    isModelDownloaded -> "Model Ready"
                                    isDownloading -> "Downloading..."
                                    else -> "Download Model"
                                }
                            )
                        }
                    }
                }
            }
            LlmConfigManager.Provider.CLOUD -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Cloud Model Configuration",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        OutlinedTextField(
                            value = cloudBaseUrl,
                            onValueChange = onCloudBaseUrlChange,
                            label = { Text("API Base URL") },
                            placeholder = { Text("https://your-server.com:3000") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = cloudApiKey,
                            onValueChange = onCloudApiKeyChange,
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Cloud model requires network. API key stored locally.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Right detail pane: Wallpaper settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSettingsPane(
    backgroundType: WallpaperConfigManager.BackgroundType,
    onBackgroundTypeChange: (WallpaperConfigManager.BackgroundType) -> Unit,
    backgroundColor: Color,
    onBackgroundColorChange: (Color) -> Unit,
    gradientStartColor: Color,
    gradientEndColor: Color,
    onGradientStartColorChange: (Color) -> Unit,
    onGradientEndColorChange: (Color) -> Unit,
    selectedPresetIndex: Int,
    onPresetSelected: (Int) -> Unit,
    imageUri: Uri?,
    onPickImage: () -> Unit,
    imageOverlayOpacity: Float,
    onOverlayOpacityChange: (Float) -> Unit,
    imageOverlayColor: Color,
    onOverlayColorChange: (Color) -> Unit,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back button for phone mode
        if (showBackButton) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }
        
        // Header
        Text(
            text = "Wallpaper",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Customize the home screen background",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Background type selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3E5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Background Type",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = backgroundType == WallpaperConfigManager.BackgroundType.COLOR,
                        onClick = { onBackgroundTypeChange(WallpaperConfigManager.BackgroundType.COLOR) },
                        label = { Text("Solid") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 0.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                    
                    FilterChip(
                        selected = backgroundType == WallpaperConfigManager.BackgroundType.GRADIENT,
                        onClick = { onBackgroundTypeChange(WallpaperConfigManager.BackgroundType.GRADIENT) },
                        label = { Text("Gradient") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 0.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                    
                    FilterChip(
                        selected = backgroundType == WallpaperConfigManager.BackgroundType.IMAGE,
                        onClick = { onBackgroundTypeChange(WallpaperConfigManager.BackgroundType.IMAGE) },
                        label = { Text("Image") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 0.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                    
                    FilterChip(
                        selected = backgroundType == WallpaperConfigManager.BackgroundType.TECH,
                        onClick = { onBackgroundTypeChange(WallpaperConfigManager.BackgroundType.TECH) },
                        label = { Text("Tech") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 0.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                }
            }
        }
        
        // Type-specific settings
        when (backgroundType) {
            WallpaperConfigManager.BackgroundType.COLOR -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Choose a color",
                            style = MaterialTheme.typography.titleMedium
                        )
                        ColorPalette(
                            selectedColor = backgroundColor,
                            onColorSelected = onBackgroundColorChange
                        )
                    }
                }
            }
            WallpaperConfigManager.BackgroundType.GRADIENT -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Preset Gradients",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        PresetGradientGrid(
                            selectedIndex = selectedPresetIndex,
                            onSelected = onPresetSelected
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(
                            text = "Custom Colors",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        // Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(gradientStartColor, gradientEndColor)
                                    )
                                )
                        )
                        
                        Text(
                            text = "Start color",
                            style = MaterialTheme.typography.labelMedium
                        )
                        ColorPalette(
                            selectedColor = gradientStartColor,
                            onColorSelected = onGradientStartColorChange
                        )
                        
                        Text(
                            text = "End color",
                            style = MaterialTheme.typography.labelMedium
                        )
                        ColorPalette(
                            selectedColor = gradientEndColor,
                            onColorSelected = onGradientEndColorChange
                        )
                    }
                }
            }
            WallpaperConfigManager.BackgroundType.IMAGE -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Wallpaper Image",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        if (imageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Wallpaper preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            imageOverlayColor.copy(alpha = imageOverlayOpacity)
                                        )
                                )
                                TextButton(
                                    onClick = onPickImage,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Text("Change", color = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = onPickImage,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pick Image from Gallery")
                            }
                        }
                        
                        if (imageUri != null) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Text(
                                text = "Overlay Settings",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Add an overlay to improve text readability",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = "Opacity: ${(imageOverlayOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Slider(
                                value = imageOverlayOpacity,
                                onValueChange = onOverlayOpacityChange,
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Text(
                                text = "Overlay color",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val overlayColors = listOf(
                                    Color(0xFF000000),
                                    Color(0xFFFFFFFF),
                                    Color(0xFF1A1A2E),
                                    Color(0xFF2D3436),
                                    Color(0xFF0F2027),
                                )
                                overlayColors.forEach { color ->
                                    val isSelected = imageOverlayColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                } else {
                                                    Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                                }
                                            )
                                            .clickable { onOverlayColorChange(color) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            WallpaperConfigManager.BackgroundType.TECH -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tech Background",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Default sci-fi style background with grid, nodes, and connecting lines.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            TechBackground()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Right detail pane: Display settings (placeholder)
 */
@Composable
fun DisplaySettingsPane(
    showBackButton: Boolean = false,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showBackButton) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }
        
        Text(
            text = "Display",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Display preferences and theme settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• Dark mode toggle\n• Font size adjustment\n• Icon size options\n• Status bar customization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Right detail pane: Sound settings (placeholder)
 */
@Composable
fun SoundSettingsPane(
    showBackButton: Boolean = false,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showBackButton) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }
        
        Text(
            text = "Sound",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Audio and notification sounds",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• Sound effects\n• Voice feedback\n• Notification sounds\n• Volume controls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Right detail pane: About
 */
@Composable
fun AboutSettingsPane(
    showBackButton: Boolean = false,
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showBackButton) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 0.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }
        
        Text(
            text = "About",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "App information and credits",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AI Launcher",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Divider()
                
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• AI-powered command processing\n• Local and cloud model support\n• Customizable wallpaper\n• Voice input\n• App launcher",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Divider()
                
                Text(
                    text = "Built with",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• Kotlin & Jetpack Compose\n• MLC LLM for local inference\n• Material Design 3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Preset gradient grid
 */
@Composable
fun PresetGradientGrid(
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    val gradients = WallpaperConfigManager.presetGradients
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gradients.chunked(4).forEachIndexed { rowIndex, rowGradients ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowGradients.forEachIndexed { colIndex, (startColor, endColor) ->
                    val index = rowIndex * 4 + colIndex
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(startColor, endColor)
                                )
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                } else {
                                    Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                }
                            )
                            .clickable { onSelected(index) }
                    )
                }
            }
        }
    }
}

/**
 * Color palette for wallpaper selection
 */
@Composable
fun ColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        // Light colors
        Color(0xFFF5F5F5),
        Color(0xFFE8EAF6),
        Color(0xFFE3F2FD),
        Color(0xFFE8F5E9),
        Color(0xFFFFF3E0),
        Color(0xFFFCE4EC),
        Color(0xFFF3E5F5),
        Color(0xFFFFF9C4),
        // Dark colors
        Color(0xFF1A1A2E),
        Color(0xFF16213E),
        Color(0xFF0F3460),
        Color(0xFF2D3436),
        Color(0xFF1B1B2F),
        Color(0xFF1A1A1A),
        // Vibrant colors
        Color(0xFF667EEA),
        Color(0xFF764BA2),
        Color(0xFFF093FB),
        Color(0xFF4ECDC4),
        Color(0xFFFF6B6B),
        Color(0xFFFFE66D),
        Color(0xFF95E1D3),
        Color(0xFFA8E6CF)
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.chunked(8).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowColors.forEach { color ->
                    val isSelected = color == selectedColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                }
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }
    }
}
