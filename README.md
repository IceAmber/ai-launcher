# AI Launcher - Smart Desktop Assistant

<div align="center">

![AI Launcher](https://img.shields.io/badge/AI-Launcher-blue?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-5.0+-green?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?style=for-the-badge&logo=kotlin)
![License](https://img.shields.io/badge/License-Apache%202.0-orange?style=for-the-badge)

**Redefine your desktop experience with AI**

[English](README.md) | [中文](README_zh.md) | [日本語](README_ja.md)

</div>

---

## 🌟 Introduction

AI Launcher is an AI-powered Android launcher that integrates advanced voice recognition and natural language processing. Control your phone with voice commands, launch apps instantly, and enjoy a smarter, more efficient mobile life.

### ✨ Core Philosophy

- 🎯 **Voice First** - Control your phone with natural language, hands-free
- 🚀 **Quick Launch** - Intelligently learns your habits, one-tap access to frequently used apps
- 🎨 **Personalization** - Rich themes, wallpapers, and layout customization options
- 🔒 **Privacy Protection** - All data stored locally, never uploaded to the cloud

---

## 📱 Main Features

### 🎤 Voice Control
- Supports Chinese and English voice recognition
- Natural language understanding, control your phone like chatting
- Launch apps, send messages, set alarms with voice
- Offline voice recognition (requires local model download)

### 📲 Smart App Management
- Automatically learns frequently used apps, displayed at the top
- Shows up to 8 frequently used apps (2 rows × 4 columns)
- Quick search and launch any app
- App usage statistics and sorting

### 🎨 Personalization
- **Tech Background** - Default sci-fi style grid background (can be disabled)
- **Image Wallpaper** - Choose images from gallery as wallpaper
- **Gradient Background** - 16 preset gradients + custom colors
- **Solid Color** - Clean single-color background
- **Dark Mode** - Auto-follow system or manual toggle

### ⚙️ Flexible Configuration
- Supports multiple AI model backends:
  - Cloud models (OpenAI, Qwen, DeepSeek, etc.)
  - Local models (MLC LLM, completely offline)
- Custom API endpoints and keys
- Detailed permission management
- Rich developer options

---

## 🛠️ Tech Stack

### Core Technologies
- **Kotlin** - Primary development language
- **Jetpack Compose** - Modern UI framework
- **Material 3** - Latest Material Design specification
- **Coroutines** - Asynchronous programming
- **ViewModel** - Lifecycle-aware

### Architecture
- **MVVM Pattern** - Clear separation of concerns
- **Clean Architecture** - Layered architecture design
- **Repository Pattern** - Data access abstraction

### Dependencies
- **Retrofit + OkHttp** - Network requests
- **Gson** - JSON parsing
- **Coil** - Image loading
- **MLC LLM** - Local large language model inference

---

## 📥 Download & Install

### Method 1: Download from Releases
Go to [Releases Page](https://github.com/IceAmber/ai-launcher/releases) to download the latest APK

### Method 2: Build from Source
```bash
# Clone the repository
git clone https://github.com/IceAmber/ai-launcher.git
cd ai-launcher

# Build debug version
./gradlew assembleDebug

# Build release version
./gradlew assembleRelease

# APK located at app/build/outputs/apk/
```

### Method 3: App Store
Coming soon to Google Play and other app stores, stay tuned!

---

## 🚀 Quick Start

### 1. First Launch
- Grant necessary permissions (app list, voice recognition, etc.)
- Choose whether to use tech background (enabled by default)
- Configure AI model (optional)

### 2. Configure AI Model

#### Using Cloud Model (Recommended for beginners)
1. Open Settings → Model Configuration
2. Select "Cloud Model"
3. Enter API endpoint and key
   - Supports OpenAI, Qwen, DeepSeek compatible APIs
   - Example: `https://api.openai.com/v1`

#### Using Local Model (Completely Offline)
1. Open Settings → Model Configuration
2. Select "Local Model (MLC)"
3. Download model files (required for first use)
4. Wait for download to complete

### 3. Use Voice Control
- Tap the microphone icon or say wake word
- Speak commands, for example:
  - "Open WeChat"
  - "Call Mom"
  - "Set alarm for 7 AM tomorrow"
  - "What's the weather today"

---

## ⚙️ Configuration

### Model Configuration

#### Cloud Model Parameters
```kotlin
// Supported API formats
Base URL: https://api.openai.com/v1
API Key: sk-xxx
Model: gpt-3.5-turbo / gpt-4

// Qwen
Base URL: https://dashscope.aliyuncs.com/api/v1
API Key: sk-xxx
Model: qwen-turbo / qwen-plus

// DeepSeek
Base URL: https://api.deepseek.com/v1
API Key: sk-xxx
Model: deepseek-chat
```

#### Local Model
- Default model: gemma-2-2b-it
- Model size: ~1.5GB
- Requires Android 5.0+ and 4GB+ RAM
- Completely offline, no network required

### Wallpaper Configuration

#### Tech Background
- Deep blue background (#0A0E27)
- Grid lines (40dp spacing)
- 7 glowing nodes + connection lines
- Customizable colors and transparency

#### Image Wallpaper
- Select from gallery
- Adjustable blur level
- Add overlay for better readability
- Custom overlay color and transparency

#### Gradient Background
- 16 preset gradients (Sunset, Ocean, Forest, etc.)
- Custom start and end colors
- Real-time preview

---

## 🧩 Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| `QUERY_ALL_PACKAGES` | Get app list for display and launch | ✅ Required |
| `RECORD_AUDIO` | Voice recognition | ⭕ Optional |
| `INTERNET` | Access cloud AI model API | ⭕ Optional (not needed for local model) |
| `VIBRATE` | Haptic feedback | ⭕ Optional |

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

---

## 🔒 Privacy Policy

We value your privacy:

- ✅ **Local Storage** - All data saved on device
- ✅ **No Data Collection** - No personal information collected
- ✅ **No Cloud Upload** - Voice recognition done locally (when using local model)
- ✅ **Open Source Transparency** - Code fully open source, auditable

For detailed privacy policy, see [PRIVACY.md](PRIVACY.md)

---

## 📄 License

This project is licensed under the **Apache License 2.0**.

```
Copyright 2026 AI Launcher

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 🙏 Acknowledgments

Thanks to these open source projects:

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit
- [MLC LLM](https://github.com/mlc-ai/mlc-llm) - Local large language model inference
- [Coil](https://coil-kt.github.io/coil/) - Kotlin image loading library
- [Retrofit](https://square.github.io/retrofit/) - Type-safe HTTP client

---

## 📞 Contact

- 📧 Email: support@ailauncher.app
- 🌐 Website: https://ailauncher.app
- 🐛 Issues: [GitHub Issues](https://github.com/IceAmber/ai-launcher/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/IceAmber/ai-launcher/discussions)

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=IceAmber/ai-launcher&type=Date)](https://star-history.com/#IceAmber/ai-launcher)

---

<div align="center">

**If this project helps you, please give it a ⭐ Star!**

Made with ❤️ by AI Launcher Team

</div>
