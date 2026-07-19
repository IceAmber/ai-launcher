# AI Launcher - 智能桌面助手

<div align="center">

![AI Launcher](https://img.shields.io/badge/AI-Launcher-blue?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-5.0+-green?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?style=for-the-badge&logo=kotlin)
![License](https://img.shields.io/badge/License-Apache%202.0-orange?style=for-the-badge)

**让 AI 重新定义你的桌面体验**

[English](#english) | [中文](#中文)

</div>

---

## 🌟 项目简介

AI Launcher 是一款基于人工智能的 Android 桌面启动器，集成了先进的语音识别和自然语言处理技术，让你可以通过语音指令控制手机、快速启动应用，享受更智能、更高效的移动生活。

### ✨ 核心理念

- 🎯 **语音优先** - 用自然语言控制手机，解放双手
- 🚀 **快速启动** - 智能学习你的使用习惯，一键直达常用应用
- 🎨 **个性化** - 丰富的主题、壁纸、布局自定义选项
- 🔒 **隐私保护** - 所有数据本地存储，不上传云端

---

## 📱 主要功能

### 🎤 语音控制
- 支持中文和英文语音识别
- 自然语言理解，像聊天一样控制手机
- 语音启动应用、发送消息、设置闹钟等
- 离线语音识别（需下载本地模型）

### 📲 智能应用管理
- 自动学习常用应用，显示在桌面顶部
- 最多显示 8 个常用应用（2 行 × 4 列）
- 快速搜索和启动任意应用
- 应用使用统计和排序

### 🎨 个性化定制
- **科技背景** - 默认科幻风格网格背景（可关闭）
- **图片壁纸** - 从相册选择图片作为壁纸
- **渐变背景** - 16 种预设渐变 + 自定义颜色
- **纯色背景** - 简洁的单色背景
- **深色模式** - 自动跟随系统或手动切换

### ⚙️ 灵活配置
- 支持多种 AI 模型后端：
  - 云端模型（OpenAI、通义千问、DeepSeek 等）
  - 本地模型（MLC LLM，完全离线）
- 自定义 API 地址和密钥
- 详细的权限管理
- 丰富的开发者选项

---

## 🖼️ 截图展示

<div align="center">
  <img src="screenshots/main.png" width="250" alt="主界面"/>
  <img src="screenshots/settings.png" width="250" alt="设置界面"/>
  <img src="screenshots/wallpaper.png" width="250" alt="壁纸设置"/>
</div>

---

## 🛠️ 技术栈

### 核心技术
- **Kotlin** - 主要开发语言
- **Jetpack Compose** - 现代化 UI 框架
- **Material 3** - 最新 Material Design 规范
- **Coroutines** - 异步编程
- **ViewModel** - 生命周期感知

### 架构设计
- **MVVM 架构** - 清晰的职责分离
- **Clean Architecture** - 分层架构设计
- **Repository Pattern** - 数据访问抽象

### 依赖库
- **Retrofit + OkHttp** - 网络请求
- **Gson** - JSON 解析
- **Coil** - 图片加载
- **MLC LLM** - 本地大语言模型推理

---

## 📥 下载安装

### 方式一：从 releases 下载
前往 [Releases 页面](https://github.com/yourusername/ai-launcher/releases) 下载最新 APK

### 方式二：从源码编译
```bash
# 克隆仓库
git clone https://github.com/yourusername/ai-launcher.git
cd ai-launcher

# 编译 debug 版本
./gradlew assembleDebug

# 编译 release 版本
./gradlew assembleRelease

# APK 位于 app/build/outputs/apk/
```

### 方式三：应用商店
即将上架 Google Play 和国内应用商店，敬请期待！

---

## 🚀 快速开始

### 1. 首次启动
- 授予必要权限（应用列表、语音识别等）
- 选择是否使用科技背景（默认开启）
- 配置 AI 模型（可选）

### 2. 配置 AI 模型

#### 使用云端模型（推荐新手）
1. 打开设置 → 模型配置
2. 选择"云端模型"
3. 填写 API 地址和密钥
   - 支持 OpenAI、通义千问、DeepSeek 等兼容接口
   - 示例：`https://api.openai.com/v1`

#### 使用本地模型（完全离线）
1. 打开设置 → 模型配置
2. 选择"本地模型 (MLC)"
3. 下载模型文件（首次使用需要）
4. 等待下载完成即可使用

### 3. 使用语音控制
- 点击麦克风图标或说出唤醒词
- 说出指令，例如：
  - "打开微信"
  - "给妈妈打电话"
  - "设置明天早上 7 点的闹钟"
  - "今天天气怎么样"

---

## ⚙️ 配置说明

### 模型配置

#### 云端模型参数
```kotlin
// 支持的 API 格式
Base URL: https://api.openai.com/v1
API Key: sk-xxx
Model: gpt-3.5-turbo / gpt-4

// 通义千问
Base URL: https://dashscope.aliyuncs.com/api/v1
API Key: sk-xxx
Model: qwen-turbo / qwen-plus

// DeepSeek
Base URL: https://api.deepseek.com/v1
API Key: sk-xxx
Model: deepseek-chat
```

#### 本地模型
- 默认模型：gemma-2-2b-it
- 模型大小：约 1.5GB
- 需要 Android 5.0+ 和 4GB+ 内存
- 完全离线运行，无需网络

### 壁纸配置

#### 科技背景
- 深蓝色背景 (#0A0E27)
- 网格线（40dp 间距）
- 7 个发光节点 + 连接线
- 可自定义颜色和透明度

#### 图片壁纸
- 支持从相册选择
- 可调整模糊程度
- 可添加遮罩层提高可读性
- 支持遮罩颜色和透明度

#### 渐变背景
- 16 种预设渐变（日落、海洋、森林等）
- 自定义起始和结束颜色
- 实时预览效果

---

## 🧩 权限说明

| 权限 | 用途 | 是否必需 |
|------|------|----------|
| `QUERY_ALL_PACKAGES` | 获取应用列表，用于显示和启动应用 | ✅ 必需 |
| `RECORD_AUDIO` | 语音识别 | ⭕ 可选 |
| `INTERNET` | 访问云端 AI 模型 API | ⭕ 可选（本地模型不需要） |
| `VIBRATE` | 触觉反馈 | ⭕ 可选 |

---

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出建议！

### 开发环境要求
- Android Studio Hedgehog 或更高版本
- JDK 17+
- Android SDK 34
- Kotlin 1.9+

### 开发步骤
1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/amazing-feature`
3. 提交更改：`git commit -m 'Add amazing feature'`
4. 推送分支：`git push origin feature/amazing-feature`
5. 提交 Pull Request

### 代码规范
- 遵循 Kotlin 官方代码风格
- 使用有意义的提交信息
- 添加必要的单元测试
- 保持代码简洁和可读性

### 翻译贡献
目前支持中文和英文，欢迎贡献其他语言翻译！

---

## 📋 路线图

### v1.1.0 (计划中)
- [ ] 小组件支持（天气、日历、音乐等）
- [ ] 手势控制（双击、滑动等）
- [ ] 应用分组和文件夹
- [ ] 更多主题和图标包

### v1.2.0 (计划中)
- [ ] AI 对话历史管理
- [ ] 语音唤醒词自定义
- [ ] 多语言支持（日语、韩语等）
- [ ] 性能优化和内存管理

### v2.0.0 (未来)
- [ ] 插件系统
- [ ] 跨设备同步
- [ ] AI 助手高级功能（日程管理、邮件处理等）
- [ ] 平板和折叠屏优化

---

## 🔒 隐私政策

我们重视你的隐私：

- ✅ **本地存储** - 所有数据保存在设备本地
- ✅ **不收集数据** - 不收集任何个人信息
- ✅ **不上传云端** - 语音识别在本地完成（使用本地模型时）
- ✅ **开源透明** - 代码完全开源，可审计

详细隐私政策请查看 [PRIVACY.md](PRIVACY.md)

---

## 📄 许可证

本项目采用 **Apache License 2.0** 开源协议。

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

## 🙏 致谢

感谢以下开源项目：

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代化 UI 工具包
- [MLC LLM](https://github.com/mlc-ai/mlc-llm) - 本地大语言模型推理
- [Coil](https://coil-kt.github.io/coil/) - Kotlin 图片加载库
- [Retrofit](https://square.github.io/retrofit/) - 类型安全的 HTTP 客户端

---

## 📞 联系方式

- 📧 Email: support@ailauncher.app
- 🌐 Website: https://ailauncher.app
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/ai-launcher/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/yourusername/ai-launcher/discussions)

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/ai-launcher&type=Date)](https://star-history.com/#yourusername/ai-launcher)

---

<div align="center">

**如果这个项目对你有帮助，请给一个 ⭐ Star 支持！**

Made with ❤️ by AI Launcher Team

</div>
