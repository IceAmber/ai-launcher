package com.ailauncher.infrastructure.config

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

/**
 * Wallpaper configuration manager
 * Handles persistence of background settings
 */
class WallpaperConfigManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Background type
     */
    enum class BackgroundType {
        COLOR,      // Solid color
        GRADIENT,   // Gradient
        IMAGE,      // Image from gallery
        TECH        // Tech-style background (default)
    }
    
    // Current background type
    var backgroundType: BackgroundType
        get() = BackgroundType.valueOf(prefs.getString(KEY_BG_TYPE, BackgroundType.TECH.name) ?: BackgroundType.TECH.name)
        set(value) = prefs.edit().putString(KEY_BG_TYPE, value.name).apply()
    
    // Solid color (as ARGB Int)
    var backgroundColor: Int
        get() = prefs.getInt(KEY_BG_COLOR, defaultLightColor)
        set(value) = prefs.edit().putInt(KEY_BG_COLOR, value).apply()
    
    // Gradient start color
    var gradientStartColor: Int
        get() = prefs.getInt(KEY_GRADIENT_START, defaultGradientStart)
        set(value) = prefs.edit().putInt(KEY_GRADIENT_START, value).apply()
    
    // Gradient end color
    var gradientEndColor: Int
        get() = prefs.getInt(KEY_GRADIENT_END, defaultGradientEnd)
        set(value) = prefs.edit().putInt(KEY_GRADIENT_END, value).apply()
    
    // Selected preset gradient index (-1 = custom)
    var presetGradientIndex: Int
        get() = prefs.getInt(KEY_PRESET_GRADIENT, 0)
        set(value) = prefs.edit().putInt(KEY_PRESET_GRADIENT, value).apply()
    
    // Image URI string (persisted across sessions)
    var imageUri: String?
        get() = prefs.getString(KEY_IMAGE_URI, null)
        set(value) = prefs.edit().putString(KEY_IMAGE_URI, value).apply()
    
    // Image blur radius (0 = no blur)
    var imageBlurRadius: Float
        get() = prefs.getFloat(KEY_IMAGE_BLUR, 0f)
        set(value) = prefs.edit().putFloat(KEY_IMAGE_BLUR, value).apply()
    
    // Image overlay opacity (0.0 = fully transparent overlay, 1.0 = fully opaque)
    var imageOverlayOpacity: Float
        get() = prefs.getFloat(KEY_IMAGE_OVERLAY, 0.3f)
        set(value) = prefs.edit().putFloat(KEY_IMAGE_OVERLAY, value).apply()
    
    // Overlay color (as ARGB Int)
    var imageOverlayColor: Int
        get() = prefs.getInt(KEY_IMAGE_OVERLAY_COLOR, defaultOverlayColor)
        set(value) = prefs.edit().putInt(KEY_IMAGE_OVERLAY_COLOR, value).apply()
    
    /**
     * Get background color as Compose Color
     */
    fun getBackgroundColor(): Color = Color(backgroundColor)
    
    /**
     * Get gradient start color as Compose Color
     */
    fun getGradientStartColor(): Color = Color(gradientStartColor)
    
    /**
     * Get gradient end color as Compose Color
     */
    fun getGradientEndColor(): Color = Color(gradientEndColor)
    
    /**
     * Get overlay color as Compose Color
     */
    fun getImageOverlayColor(): Color = Color(imageOverlayColor)
    
    /**
     * Apply a preset gradient (sets start/end colors and index)
     */
    fun applyPresetGradient(index: Int) {
        if (index in presetGradients.indices) {
            val (start, end) = presetGradients[index]
            gradientStartColor = start.value.toLong().toInt()
            gradientEndColor = end.value.toLong().toInt()
            presetGradientIndex = index
        }
    }
    
    companion object {
        private const val PREFS_NAME = "wallpaper_config"
        
        private const val KEY_BG_TYPE = "background_type"
        private const val KEY_BG_COLOR = "background_color"
        private const val KEY_GRADIENT_START = "gradient_start_color"
        private const val KEY_GRADIENT_END = "gradient_end_color"
        private const val KEY_PRESET_GRADIENT = "preset_gradient_index"
        private const val KEY_IMAGE_URI = "image_uri"
        private const val KEY_IMAGE_BLUR = "image_blur"
        private const val KEY_IMAGE_OVERLAY = "image_overlay_opacity"
        private const val KEY_IMAGE_OVERLAY_COLOR = "image_overlay_color"
        
        // Default colors
        val defaultLightColor = Color(0xFFF5F5F5).value.toLong().toInt()
        val defaultDarkColor = Color(0xFF1A1A2E).value.toLong().toInt()
        val defaultGradientStart = Color(0xFF667EEA).value.toLong().toInt()
        val defaultGradientEnd = Color(0xFF764BA2).value.toLong().toInt()
        val defaultOverlayColor = Color(0xFF000000).value.toLong().toInt()
        
        /**
         * Preset gradients: list of (startColor, endColor) as Compose Colors
         */
        val presetGradients: List<Pair<Color, Color>> = listOf(
            // 0: Indigo → Purple (default)
            Pair(Color(0xFF667EEA), Color(0xFF764BA2)),
            // 1: Sunset
            Pair(Color(0xFFFF6B6B), Color(0xFFFFE66D)),
            // 2: Ocean
            Pair(Color(0xFF2193B0), Color(0xFF6DD5ED)),
            // 3: Forest
            Pair(Color(0xFF134E5E), Color(0xFF71B280)),
            // 4: Peach
            Pair(Color(0xFFED4264), Color(0xFFFFE66D)),
            // 5: Midnight
            Pair(Color(0xFF232526), Color(0xFF414345)),
            // 6: Lavender
            Pair(Color(0xFFDA22FF), Color(0xFF9733EE)),
            // 7: Emerald
            Pair(Color(0xFF348F50), Color(0xFF56B4D3)),
            // 8: Rose
            Pair(Color(0xFFFFE0EA), Color(0xFFC6426E)),
            // 9: Arctic
            Pair(Color(0xFF83A4D4), Color(0xFFB6FBFF)),
            // 10: Fire
            Pair(Color(0xFFFF416C), Color(0xFFFF4B2B)),
            // 11: Northern Lights
            Pair(Color(0xFF43E97B), Color(0xFF38F9D7)),
            // 12: Twilight
            Pair(Color(0xFF0F2027), Color(0xFF2C5364)),
            // 13: Candy
            Pair(Color(0xFFFDCBF1), Color(0xFFE6A0C3)),
            // 14: Deep Space
            Pair(Color(0xFF000428), Color(0xFF004E92)),
            // 15: Warm Flame
            Pair(Color(0xFFFF9A9E), Color(0xFFFAD0C4)),
        )
    }
}
