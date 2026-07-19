# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep the main activity
-keep class com.ailauncher.presentation.ui.EnhancedMainActivity { *; }
-keep class com.ailauncher.presentation.ui.MainActivity { *; }
-keep class com.ailauncher.presentation.ui.LlmSettingsActivity { *; }

# Keep data classes
-keep class com.ailauncher.domain.model.** { *; }
-keep class com.ailauncher.presentation.model.** { *; }

# Keep use case classes
-keep class com.ailauncher.domain.usecase.** { *; }

# Keep repository implementations
-keep class com.ailauncher.data.repository.** { *; }

# Keep infrastructure services
-keep class com.ailauncher.infrastructure.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Compose
-keep class androidx.compose.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Keep all annotations
-keepattributes *Annotation*

# Prevent stripping line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
