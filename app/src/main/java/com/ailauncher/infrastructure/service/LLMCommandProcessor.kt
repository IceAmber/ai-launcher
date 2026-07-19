package com.ailauncher.infrastructure.service

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.ailauncher.domain.model.AppInfo
import com.ailauncher.domain.model.ExecutionResult
import com.ailauncher.domain.model.UserCommand
import com.ailauncher.domain.repository.AppDiscovery
import com.ailauncher.domain.service.CommandProcessor
import com.ailauncher.infrastructure.llm.LlmClient
import com.ailauncher.infrastructure.llm.QwenApiClient
import com.ailauncher.infrastructure.location.LocationHelper

/**
 * 基于 LLM 的命令处理器
 * 支持多轮对话、精确 app 匹配、多种操作类型
 * 支持云端和本地模型切换
 */
class LLMCommandProcessor(
    private val appDiscovery: AppDiscovery,
    private val llmClient: LlmClient = QwenApiClient(),
    private val context: Context? = null
) : CommandProcessor {

    override suspend fun process(command: UserCommand): ExecutionResult {
        val commandText = command.text.trim()

        if (commandText.isEmpty()) {
            return ExecutionResult.error("Command is empty")
        }

        // 获取已安装应用列表
        val installedApps = appDiscovery.discoverApps()
        val appList = installedApps.joinToString("\n") { "- ${it.name}" }

        // 构建系统提示
        val systemPrompt = buildSystemPrompt(appList)

        // 调用 LLM（支持多轮对话）
        val result = llmClient.chat(commandText, systemPrompt)

        return result.fold(
            onSuccess = { content ->
                val commandResult = llmClient.parseCommandResult(content)
                handleCommandResult(commandResult, installedApps)
            },
            onFailure = { error ->
                ExecutionResult.error("AI service call failed: ${error.message}")
            }
        )
    }

    private fun buildSystemPrompt(appList: String): String {
        return """
你是一个智能桌面助手，需要理解用户的自然语言指令并执行相应操作。

## 重要规则（必须严格遵守）
1. **语言一致性（最高优先级）**：回复语言必须与用户输入语言完全一致
   - 用户输入中文 → message 字段必须用中文
   - 用户输入英文 → message 字段必须用英文
   - 用户输入日文 → message 字段必须用日文
   - 以此类推，严格匹配用户输入的语言
2. **精确匹配应用名**：打开应用时，target 必须是已安装应用列表中的精确名称
3. **只返回 JSON**：不要包含任何其他文字、解释或 markdown 标记

## 已安装的应用列表
$appList

## 返回格式（严格 JSON）
{
  "action": "操作类型",
  "target": "目标参数",
  "confidence": 0.0到1.0的置信度,
  "message": "给用户的回复（必须使用与用户输入相同的语言）"
}

## 支持的操作类型

### LAUNCH_APP - 打开应用
- target: 已安装应用列表中的精确应用名
- 示例：用户说"打开微信"，target 应该是 "微信"（不是"WeChat"）

### SET_ALARM - 设置闹钟
- target: 时间描述（如"早上7点"、"明天下午3点"）
- 示例：用户说"设置早上7点的闹钟"，target 应该是 "早上7点"

### SEND_MESSAGE - 发送消息
- target: 联系人名称或消息内容
- 示例：用户说"给妈妈发消息说我回来了"，target 应该是 "妈妈"

### QUERY_WEATHER - 查询天气
- target: 地点名称，如果用户没指定具体地点则传 "当前位置"
- 示例：用户说"今天天气怎么样"，target 应该是 "当前位置"
- 示例：用户说"北京天气"，target 应该是 "北京"

### SEARCH_WEB - 搜索网页
- target: 搜索关键词
- 示例：用户说"搜索AI最新进展"，target 应该是 "AI最新进展"

### SET_CALENDAR - 设置日程
- target: 日程描述
- 示例：用户说"明天下午开会"，target 应该是 "明天下午开会"

### UNKNOWN - 无法理解或执行
- 当指令无法理解或不在支持范围内时使用

## 注意事项
1. 如果用户要打开的应用不在已安装列表中，返回 UNKNOWN 并说明
2. confidence 低于 0.7 时，在 message 中询问用户确认
3. 回复语言必须与用户输入语言完全一致
4. 只返回 JSON，不要有其他内容
""".trimIndent()
    }

    private suspend fun handleCommandResult(
        result: com.ailauncher.infrastructure.llm.LlmCommandResult,
        installedApps: List<AppInfo>
    ): ExecutionResult {
        // 置信度过低
        if (result.confidence < 0.5f) {
            return ExecutionResult.error(result.message.ifEmpty { "Sorry, I didn't understand your command" })
        }

        return when (result.action) {
            "LAUNCH_APP" -> handleLaunchApp(result, installedApps)
            "SET_ALARM" -> handleSetAlarm(result)
            "SEND_MESSAGE" -> handleSendMessage(result)
            "QUERY_WEATHER" -> handleQueryWeather(result)
            "SEARCH_WEB" -> handleSearchWeb(result)
            "SET_CALENDAR" -> handleSetCalendar(result)
            else -> ExecutionResult.error(result.message.ifEmpty { "Sorry, I cannot process this command" })
        }
    }

    private fun handleLaunchApp(
        result: com.ailauncher.infrastructure.llm.LlmCommandResult,
        installedApps: List<AppInfo>
    ): ExecutionResult {
        val targetApp = findApp(result.target, installedApps)
        return if (targetApp != null) {
            ExecutionResult.success(
                message = result.message.ifEmpty { "Opening ${targetApp.name}" },
                action = "LAUNCH_APP",
                metadata = mapOf("appInfo" to targetApp)
            )
        } else {
            ExecutionResult.error(result.message.ifEmpty { "App not found: ${result.target}" })
        }
    }

    private fun handleSetAlarm(result: com.ailauncher.infrastructure.llm.LlmCommandResult): ExecutionResult {
        if (context == null) {
            return ExecutionResult.success(
                message = result.message.ifEmpty { "Setting alarm requires Context" },
                action = "SET_ALARM"
            )
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AlarmClock.EXTRA_MESSAGE, result.target ?: "闹钟")
            }
            context.startActivity(intent)
            ExecutionResult.success(
                message = result.message.ifEmpty { "Setting alarm: ${result.target}" },
                action = "SET_ALARM"
            )
        } catch (e: Exception) {
            ExecutionResult.error("Failed to set alarm: ${e.message}")
        }
    }

    private fun handleSendMessage(result: com.ailauncher.infrastructure.llm.LlmCommandResult): ExecutionResult {
        if (context == null) {
            return ExecutionResult.success(
                message = result.message.ifEmpty { "Sending message requires Context" },
                action = "SEND_MESSAGE"
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = android.net.Uri.parse("smsto:${result.target}")
            }
            context.startActivity(intent)
            ExecutionResult.success(
                message = result.message.ifEmpty { "Opening messaging app" },
                action = "SEND_MESSAGE"
            )
        } catch (e: Exception) {
            ExecutionResult.error("Failed to open messages: ${e.message}")
        }
    }

    private suspend fun handleQueryWeather(result: com.ailauncher.infrastructure.llm.LlmCommandResult): ExecutionResult {
        if (context == null) {
            return ExecutionResult.error("Weather query requires Context")
        }

        val locationHelper = LocationHelper(context)

        // 检查位置权限
        if (!locationHelper.hasLocationPermission()) {
            return ExecutionResult(
                success = false,
                message = "Location permission required for weather query",
                action = "QUERY_WEATHER",
                metadata = mapOf(
                    "requiresPermission" to true,
                    "permissionType" to "LOCATION"
                )
            )
        }

        // 获取位置
        val location = locationHelper.getCurrentLocation()
        if (location == null) {
            return ExecutionResult.error("Unable to get location, please try again")
        }

        val locationName = locationHelper.getLocationName(location)
        val queryLocation = result.target ?: locationName ?: "当前位置"

        // 查询天气（简化处理，实际应该调用天气 API）
        return ExecutionResult.success(
            message = "Querying weather for $queryLocation",
            action = "QUERY_WEATHER",
            metadata = mapOf("location" to queryLocation)
        )
    }

    private fun handleSearchWeb(result: com.ailauncher.infrastructure.llm.LlmCommandResult): ExecutionResult {
        if (context == null) {
            return ExecutionResult.success(
                message = result.message.ifEmpty { "Web search requires Context" },
                action = "SEARCH_WEB"
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(android.app.SearchManager.QUERY, result.target)
            }
            context.startActivity(intent)
            ExecutionResult.success(
                message = result.message.ifEmpty { "Searching: ${result.target}" },
                action = "SEARCH_WEB"
            )
        } catch (e: Exception) {
            ExecutionResult.error("Search failed: ${e.message}")
        }
    }

    private fun handleSetCalendar(result: com.ailauncher.infrastructure.llm.LlmCommandResult): ExecutionResult {
        if (context == null) {
            return ExecutionResult.success(
                message = result.message.ifEmpty { "Setting calendar event requires Context" },
                action = "SET_CALENDAR"
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, result.target)
            }
            context.startActivity(intent)
            ExecutionResult.success(
                message = result.message.ifEmpty { "Opening calendar app" },
                action = "SET_CALENDAR"
            )
        } catch (e: Exception) {
            ExecutionResult.error("Failed to open calendar: ${e.message}")
        }
    }

    private fun findApp(target: String?, apps: List<AppInfo>): AppInfo? {
        if (target.isNullOrEmpty()) return null

        // 1. 精确匹配
        apps.find { it.name.equals(target, ignoreCase = true) }?.let { return it }

        // 2. 包含匹配
        apps.find { it.name.contains(target, ignoreCase = true) }?.let { return it }

        // 3. 模糊匹配（去除空格后比较）
        val normalizedTarget = target.replace(" ", "").lowercase()
        apps.find { it.name.replace(" ", "").lowercase().contains(normalizedTarget) }?.let { return it }

        return null
    }
}
