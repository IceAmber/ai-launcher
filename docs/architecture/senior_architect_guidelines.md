# AI桌面启动器 - 高级架构师设计规范

## 1. 架构原则

### 1.1 核心设计哲学
- **单一职责**：每个类/模块只负责一个功能领域
- **渐进式复杂度**：从简单开始，逐步增加复杂性
- **防御性编程**：假设所有外部输入都是恶意的
- **可观测性优先**：所有关键路径必须有日志和监控
- **向后兼容**：API和数据格式必须保持向后兼容

### 1.2 SOLID原则实施

#### 单一职责原则 (SRP)
- 每个类应该只有一个改变的理由
- 功能模块按业务边界划分，而非技术层次
- 示例：
  ```kotlin
  // ❌ 错误：AppManager同时处理扫描和启动
  class AppManager {
      fun scanApps(): List<AppInfo>
      fun launchApp(packageName: String)
      fun getPermissions(): List<String>
  }
  
  // ✅ 正确：职责分离
  class AppDiscoveryService { fun discoverApps(): List<AppInfo> }
  class AppLauncher { fun launch(packageName: String): Boolean }
  class PermissionManager { fun requestPermissions(): Boolean }
  ```

#### 开闭原则 (OCP)
- 对扩展开放，对修改关闭
- 使用策略模式、工厂模式实现可扩展性
- 示例：
  ```kotlin
  interface CommandExecutor {
      fun canExecute(command: UserCommand): Boolean
      fun execute(command: UserCommand): ExecutionResult
  }
  
  class AppLaunchExecutor : CommandExecutor { ... }
  class SettingsOpenExecutor : CommandExecutor { ... }
  class DeviceControlExecutor : CommandExecutor { ... }
  ```

#### 里氏替换原则 (LSP)
- 子类必须能够替换父类而不影响程序正确性
- 接口契约必须严格遵守
- 禁止在子类中抛出父类未声明的异常

#### 接口隔离原则 (ISP)
- 客户端不应该依赖它不需要的接口
- 接口按功能垂直拆分
- 示例：
  ```kotlin
  // ❌ 错误：大而全的接口
  interface LauncherService {
      fun launchApp(name: String)
      fun openSettings()
      fun toggleWifi()
      fun getBatteryLevel()
      fun startVoiceRecognition()
  }
  
  // ✅ 正确：按功能拆分
  interface AppManagement {
      fun launchApp(name: String)
  }
  
  interface SystemControl {
      fun openSettings()
      fun toggleWifi()
  }
  
  interface DeviceMonitoring {
      fun getBatteryLevel()
  }
  
  interface VoiceInput {
      fun startVoiceRecognition()
  }
  ```

#### 依赖倒置原则 (DIP)
- 高层模块不应该依赖低层模块，两者都应该依赖抽象
- 抽象不应该依赖细节，细节应该依赖抽象
- 使用依赖注入框架（Hilt/Dagger）
- 示例：
  ```kotlin
  // ❌ 错误：直接依赖具体实现
  class CommandProcessor {
      private val dashScopeClient = DashScopeClient() // 具体实现
  }
  
  // ✅ 正确：依赖抽象
  interface LLMClient {
      suspend fun generate(prompt: String): String
  }
  
  class CommandProcessor @Inject constructor(
      private val llmClient: LLMClient // 抽象接口
  )
  ```

## 2. 分层架构设计

### 2.1 整体架构
```
┌─────────────────────────────────────────────────┐
│                  Presentation Layer             │
│  (UI, Input Handling, User Feedback)           │
└─────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│                   Application Layer             │
│  (Use Cases, Command Orchestration)            │
└─────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│                   Domain Layer                  │
│  (Business Logic, Entities, Value Objects)     │
└─────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│                 Infrastructure Layer            │
│  (External Services, Data Access, Frameworks)  │
└─────────────────────────────────────────────────┘
```

### 2.2 各层详细规范

#### Presentation Layer
- **职责**：处理用户输入、显示输出、管理UI状态
- **约束**：
  - 不包含业务逻辑
  - 不直接访问外部服务
  - 使用ViewModel管理状态
- **技术栈**：Jetpack Compose + ViewModel + LiveData

#### Application Layer
- **职责**：协调用例执行、事务管理、安全控制
- **约束**：
  - 每个用例对应一个Application Service
  - 不包含具体实现细节
  - 处理跨领域协调
- **模式**：Use Case Pattern + Command Query Separation

#### Domain Layer
- **职责**：核心业务逻辑、领域模型、业务规则
- **约束**：
  - 纯Kotlin，无Android依赖
  - 无外部框架依赖
  - 包含实体、值对象、领域服务
- **关键概念**：
  - `UserCommand`：用户输入的领域对象
  - `ExecutionResult`：执行结果的领域对象
  - `CommandValidator`：命令验证的领域服务

#### Infrastructure Layer
- **职责**：外部服务集成、数据持久化、框架适配
- **约束**：
  - 实现Domain层定义的接口
  - 处理技术细节（网络、数据库、权限等）
  - 提供适配器模式封装外部API
- **关键组件**：
  - `AndroidAppDiscoveryAdapter`：Android PackageManager适配器
  - `DashScopeLLMAdapter`：DashScope API适配器
  - `SharedPreferencesStorage`：本地存储适配器

## 3. 关键组件设计

### 3.1 命令处理管道
```kotlin
// Domain Layer
data class UserCommand(val text: String, val context: CommandContext)
data class ExecutionResult(val success: Boolean, val message: String)

interface CommandProcessor {
    suspend fun process(command: UserCommand): ExecutionResult
}

// Application Layer
class NaturalLanguageCommandProcessor @Inject constructor(
    private val commandRouter: CommandRouter,
    private val executionOrchestrator: ExecutionOrchestrator
) : CommandProcessor {
    override suspend fun process(command: UserCommand): ExecutionResult {
        val parsedCommand = commandRouter.route(command)
        return executionOrchestrator.execute(parsedCommand)
    }
}

// Infrastructure Layer
class DashScopeCommandRouter @Inject constructor(
    private val llmClient: LLMClient
) : CommandRouter {
    override suspend fun route(command: UserCommand): ParsedCommand {
        val prompt = buildPrompt(command)
        val response = llmClient.generate(prompt)
        return parseResponse(response)
    }
}
```

### 3.2 应用发现服务
```kotlin
// Domain Layer
data class AppInfo(val name: String, val packageName: String, val isSystemApp: Boolean)
interface AppDiscovery {
    fun discoverApps(): List<AppInfo>
    fun findAppByName(name: String): AppInfo?
}

// Infrastructure Layer
class AndroidAppDiscovery @Inject constructor(
    private val context: Context
) : AppDiscovery {
    private val cache = Cache<List<AppInfo>>()
    
    override fun discoverApps(): List<AppInfo> {
        return cache.getOrPut(300_000) { // 5分钟缓存
            val packageManager = context.packageManager
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { app ->
                    try {
                        val label = app.loadLabel(packageManager).toString()
                        val packageName = app.packageName
                        val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        AppInfo(label, packageName, isSystem)
                    } catch (e: Exception) {
                        null
                    }
                }
        }
    }
    
    override fun findAppByName(name: String): AppInfo? {
        val allApps = discoverApps()
        return allApps.find { it.name.equals(name, ignoreCase = true) }
            ?: allApps.find { it.name.contains(name, ignoreCase = true) }
    }
}
```

### 3.3 权限管理
```kotlin
// Domain Layer
enum class PermissionType { QUERY_PACKAGES, USAGE_STATS, MICROPHONE }
data class PermissionRequest(val type: PermissionType, val rationale: String)

interface PermissionManager {
    fun checkPermission(type: PermissionType): Boolean
    fun requestPermission(type: PermissionType): Flow<PermissionResult>
}

// Infrastructure Layer
class AndroidPermissionManager @Inject constructor(
    private val activity: Activity
) : PermissionManager {
    override fun checkPermission(type: PermissionType): Boolean {
        return when (type) {
            PermissionType.QUERY_PACKAGES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ContextCompat.checkSelfPermission(
                        activity, 
                        Manifest.permission.QUERY_ALL_PACKAGES
                    ) == PackageManager.PERMISSION_GRANTED
                } else true
            }
            // ... 其他权限类型
        }
    }
    
    override fun requestPermission(type: PermissionType): Flow<PermissionResult> {
        return callbackFlow {
            val permission = when (type) {
                PermissionType.QUERY_PACKAGES -> Manifest.permission.QUERY_ALL_PACKAGES
                // ... 其他映射
            }
            
            ActivityCompat.requestPermissions(activity, arrayOf(permission), REQUEST_CODE)
            // ... 处理回调
            awaitClose { /* cleanup */ }
        }
    }
}
```

## 4. 代码质量标准

### 4.1 命名规范
- **类名**：名词，UpperCamelCase (`AppDiscoveryService`)
- **方法名**：动词，lowerCamelCase (`discoverApps()`, `launchApplication()`)
- **变量名**：描述性，避免缩写 (`packageName` 而非 `pkgName`)
- **常量名**：UPPER_SNAKE_CASE (`MAX_CACHE_DURATION_MS`)

### 4.2 注释规范
- **公共API**：必须有KDoc注释
- **复杂逻辑**：必须有行内注释解释"为什么"而非"做什么"
- **TODO注释**：必须包含负责人和截止日期
  ```kotlin
  // TODO(john): Implement local LLM fallback by 2026-08-01
  ```

### 4.3 测试要求
- **单元测试**：Domain层100%覆盖
- **集成测试**：Application层80%覆盖
- **UI测试**：核心用户流程100%覆盖
- **测试命名**：`Given_When_Then`模式
  ```kotlin
  @Test
  fun givenValidAppName_whenLaunchApp_thenAppStartsSuccessfully() {
      // test implementation
  }
  ```

### 4.4 性能约束
- **内存占用**：<50MB RAM
- **启动时间**：<500ms
- **命令响应**：<1s (本地), <2s (云端)
- **电池消耗**：<1% per hour (idle)

### 4.5 安全要求
- **输入验证**：所有外部输入必须验证
- **权限最小化**：仅请求必要权限
- **数据加密**：敏感数据必须加密存储
- **网络安全**：所有网络请求必须使用HTTPS

## 5. 依赖管理

### 5.1 依赖注入
- **框架**：Hilt (Android官方推荐)
- **作用域**：
  - `@Singleton`：应用生命周期
  - `@ActivityScoped`：Activity生命周期
  - `@ViewModelScoped`：ViewModel生命周期

### 5.2 第三方库选择原则
- **稳定性**：选择成熟、维护活跃的库
- **轻量性**：优先选择功能单一的小型库
- **兼容性**：确保与目标Android版本兼容
- **许可证**：必须使用商业友好的开源许可证

### 5.3 版本管理
- **语义化版本**：遵循SemVer规范
- **依赖锁定**：使用gradle dependency lock
- **安全扫描**：定期进行依赖安全扫描

## 6. 监控与可观测性

### 6.1 日志规范
- **级别**：ERROR > WARN > INFO > DEBUG
- **结构化**：使用JSON格式记录关键事件
- **敏感信息**：绝不记录用户隐私数据
- **采样率**：DEBUG日志仅在开发环境启用

### 6.2 指标监控
- **核心指标**：
  - 命令成功率
  - 平均响应时间
  - 内存使用量
  - 电池消耗率
- **错误监控**：集成Crashlytics或类似服务

### 6.3 用户反馈
- **匿名统计**：收集使用模式（可选择退出）
- **错误报告**：提供一键错误报告功能
- **功能请求**：内置反馈渠道

## 7. 文档要求

### 7.1 代码文档
- **README.md**：项目概述、快速开始、架构图
- **ADR**：重要架构决策记录
- **API文档**：公共接口的详细说明

### 7.2 用户文档
- **用户指南**：功能使用说明
- **FAQ**：常见问题解答
- **隐私政策**：数据处理透明说明

### 7.3 开发者文档
- **贡献指南**：如何参与开发
- **代码规范**：详细的编码标准
- **测试指南**：如何编写和运行测试

## 8. 持续集成/持续部署

### 8.1 CI/CD流水线
- **代码检查**：ktlint, detekt, Android Lint
- **单元测试**：运行所有单元测试
- **集成测试**：运行核心集成测试
- **构建验证**：生成Release APK并验证

### 8.2 质量门禁
- **测试覆盖率**：Domain层≥90%
- **代码复杂度**：圈复杂度≤10
- **安全扫描**：无高危漏洞
- **性能基准**：满足性能约束

### 8.3 发布流程
- **语义化发布**：自动版本管理和发布
- **灰度发布**：逐步推送给用户
- **回滚机制**：快速回滚有问题的版本

---

**架构师签名**：此设计规范必须严格遵守，任何偏离都需要架构评审委员会批准。