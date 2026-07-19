package com.ailauncher.infrastructure.llm

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 模型下载管理器
 * 负责下载和管理本地 LLM 模型文件
 */
class ModelDownloadManager(private val context: Context) {
    
    enum class DownloadState {
        NOT_STARTED,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        FAILED
    }
    
    data class DownloadProgress(
        val state: DownloadState,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val errorMessage: String? = null
    ) {
        val progressPercent: Float
            get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    }
    
    private val _downloadProgress = MutableStateFlow(DownloadProgress(DownloadState.NOT_STARTED, 0, 0))
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress
    
    // 默认模型配置
    companion object {
        private const val TAG = "ModelDownloadManager"
        const val DEFAULT_MODEL_NAME = "gemma-2-2b-it-q4f16_1-MLC"
        const val DEFAULT_MODEL_URL = "https://hf-mirror.com/mlc-ai/gemma-2-2b-it-q4f16_1-MLC/resolve/main"
        const val FALLBACK_MODEL_URL = "https://huggingface.co/mlc-ai/gemma-2-2b-it-q4f16_1-MLC/resolve/main"
        
        // 核心配置文件（必须下载）
        val CORE_FILES = listOf(
            "ndarray-cache.json",
            "mlc-chat-config.json",
            "tokenizer.json",
            "tokenizer_config.json"
        )
    }
    
    /**
     * 检查模型是否已下载
     */
    fun isModelDownloaded(modelName: String = DEFAULT_MODEL_NAME): Boolean {
        val modelDir = File(context.filesDir, "models/$modelName")
        if (!modelDir.exists()) return false
        
        // 检查核心文件
        for (file in CORE_FILES) {
            if (!File(modelDir, file).exists()) return false
        }
        
        // 检查是否有参数文件（params_shard_*.bin 或 model.params）
        val hasParams = modelDir.listFiles()?.any { 
            it.name.startsWith("params_shard_") || it.name == "model.params" 
        } ?: false
        
        return hasParams
    }
    
    /**
     * 获取模型目录路径
     */
    fun getModelPath(modelName: String = DEFAULT_MODEL_NAME): String {
        return File(context.filesDir, "models/$modelName").absolutePath
    }
    
    /**
     * 下载模型
     */
    suspend fun downloadModel(
        modelName: String = DEFAULT_MODEL_NAME,
        baseUrl: String = DEFAULT_MODEL_URL
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting model download: $modelName from $baseUrl")
            val modelDir = File(context.filesDir, "models/$modelName")
            modelDir.mkdirs()
            
            _downloadProgress.value = DownloadProgress(DownloadState.DOWNLOADING, 0, 0)
            
            var totalDownloaded = 0L
            var totalSize = 0L
            
            // 获取文件列表
            Log.d(TAG, "Fetching file list from API...")
            val fileList = fetchFileList(modelName)
            if (fileList.isEmpty()) {
                throw Exception("Failed to fetch file list")
            }
            
            Log.i(TAG, "Found ${fileList.size} files to download")
            
            // 计算总大小
            for (file in fileList) {
                totalSize += file.size
            }
            Log.i(TAG, "Total model size: $totalSize bytes")
            
            // 下载每个文件
            for (file in fileList) {
                val outputFile = File(modelDir, file.name)
                
                // 如果文件已存在且大小正确，跳过
                if (outputFile.exists() && outputFile.length() == file.size) {
                    totalDownloaded += file.size
                    Log.d(TAG, "File ${file.name} already exists, skipping (${file.size} bytes)")
                    _downloadProgress.value = DownloadProgress(
                        DownloadState.DOWNLOADING,
                        totalDownloaded,
                        totalSize
                    )
                    continue
                }
                
                Log.i(TAG, "Downloading ${file.name}...")
                downloadFile("$baseUrl/${file.name}", outputFile) { bytesDownloaded ->
                    _downloadProgress.value = DownloadProgress(
                        DownloadState.DOWNLOADING,
                        totalDownloaded + bytesDownloaded,
                        totalSize
                    )
                }
                
                totalDownloaded += outputFile.length()
                Log.i(TAG, "Downloaded ${file.name}: ${outputFile.length()} bytes")
            }
            
            _downloadProgress.value = DownloadProgress(DownloadState.COMPLETED, totalSize, totalSize)
            Log.i(TAG, "Model download completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            _downloadProgress.value = DownloadProgress(
                DownloadState.FAILED,
                0,
                0,
                e.message
            )
            Result.failure(e)
        }
    }
    
    /**
     * 从 HuggingFace API 获取文件列表
     */
    private suspend fun fetchFileList(modelName: String): List<ModelFile> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://hf-mirror.com/api/models/mlc-ai/$modelName/tree/main"
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()
            
            if (connection.responseCode != 200) {
                Log.w(TAG, "API request failed: ${connection.responseCode}")
                connection.disconnect()
                return@withContext emptyList()
            }
            
            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            
            // 解析 JSON 响应
            val files = mutableListOf<ModelFile>()
            val jsonArray = org.json.JSONArray(response)
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val fileName = obj.getString("path")
                val fileSize = obj.getLong("size")
                
                // 只下载必要的文件
                if (fileName.endsWith(".bin") || fileName.endsWith(".json") || 
                    fileName == "tokenizer.model") {
                    files.add(ModelFile(fileName, fileSize))
                }
            }
            
            files
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch file list", e)
            emptyList()
        }
    }
    
    /**
     * 模型文件信息
     */
    data class ModelFile(val name: String, val size: Long)
    
    /**
     * 下载单个文件
     */
    private suspend fun downloadFile(
        urlStr: String,
        outputFile: File,
        onProgress: (Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        // 尝试主 URL，失败则尝试 fallback
        val urls = listOf(urlStr, urlStr.replace("hf-mirror.com", "huggingface.co"))
        var lastException: Exception? = null
        
        for (url in urls) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.connect()
                
                if (connection.responseCode != 200) {
                    connection.disconnect()
                    throw Exception("Failed to download $url: HTTP ${connection.responseCode}")
                }
                
                val totalSize = connection.contentLengthLong
                var bytesDownloaded = 0L
                
                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead
                            onProgress(bytesDownloaded)
                        }
                    }
                }
                
                connection.disconnect()
                return@withContext // 成功下载，返回
            } catch (e: Exception) {
                lastException = e
                // 继续尝试下一个 URL
            }
        }
        
        throw lastException ?: Exception("All download attempts failed")
    }
    
    /**
     * 删除已下载的模型
     */
    fun deleteModel(modelName: String = DEFAULT_MODEL_NAME) {
        val modelDir = File(context.filesDir, "models/$modelName")
        if (modelDir.exists()) {
            modelDir.deleteRecursively()
        }
    }
    
    /**
     * 获取已下载模型的大小
     */
    fun getDownloadedModelSize(modelName: String = DEFAULT_MODEL_NAME): Long {
        val modelDir = File(context.filesDir, "models/$modelName")
        return if (modelDir.exists()) {
            modelDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }
    }
}
