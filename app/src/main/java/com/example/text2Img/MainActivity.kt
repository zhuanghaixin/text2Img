package com.example.text2Img

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var promptInput: EditText
    private lateinit var generateButton: Button
    private lateinit var generatedImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var downloadButton: Button

    private val client = OkHttpClient()
    
    private val TAG = "MainActivity"
    private val COZE_TOKEN = "pat_STXwVi1Oftb1Ds290APrxNCUmjkk1EWAHqalA1LqthfWr0aCvd0D9b9XdVX15CFY"
    private val WORKFLOW_ID = "7490858139598061618"
    private val COZE_API_URL = "https://api.coze.cn/v1/workflow/run"
    
    // 保存当前图片URL
    private var currentImageUrl: String? = null
    
    // 存储权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            downloadImage()
        } else {
            Toast.makeText(this, "需要存储权限才能下载图片", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        promptInput = findViewById(R.id.promptInput)
        generateButton = findViewById(R.id.generateButton)
        generatedImage = findViewById(R.id.generatedImage)
        statusText = findViewById(R.id.statusText)
        downloadButton = findViewById(R.id.downloadButton)

        // 确保下载按钮初始状态为隐藏
        downloadButton.visibility = View.GONE
        Log.d(TAG, "下载按钮初始化: GONE")

        // 设置默认提示文本
        promptInput.setText("一只可爱的小猫咪")

        generateButton.setOnClickListener {
            val prompt = promptInput.text.toString()
            if (prompt.isNotEmpty()) {
                generateImage(prompt)
            } else {
                Toast.makeText(this, "请输入描述文本", Toast.LENGTH_SHORT).show()
            }
        }
        
        downloadButton.setOnClickListener {
            Log.d(TAG, "点击下载按钮")
            checkAndRequestPermissions()
        }
    }
    
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上使用READ_MEDIA_IMAGES权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            } else {
                downloadImage()
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // Android 10及以下需要读写存储权限
            val hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            
            if (!hasWritePermission || !hasReadPermission) {
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ))
            } else {
                downloadImage()
            }
        } else {
            // Android 11-12只需要READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            } else {
                downloadImage()
            }
        }
    }
    
    private fun downloadImage() {
        currentImageUrl?.let { url ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val filename = "IMG_$timestamp.jpg"
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10及以上使用MediaStore API
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Text2Img")
                        }
                        
                        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let { imageUri ->
                            val outputStream = contentResolver.openOutputStream(imageUri)
                            if (saveImageToStream(url, outputStream)) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        // Android 9及以下使用传统File API
                        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Text2Img")
                        if (!directory.exists()) {
                            directory.mkdirs()
                        }
                        
                        val file = File(directory, filename)
                        val outputStream = FileOutputStream(file)
                        if (saveImageToStream(url, outputStream)) {
                            // 通知图库更新
                            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            val contentUri = Uri.fromFile(file)
                            mediaScanIntent.data = contentUri
                            sendBroadcast(mediaScanIntent)
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "下载图片失败", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } ?: run {
            Toast.makeText(this, "没有可下载的图片", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveImageToStream(imageUrl: String, outputStream: OutputStream?): Boolean {
        if (outputStream == null) return false
        
        try {
            val connection = URL(imageUrl).openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            
            val buffer = ByteArray(4096)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "保存图片流失败", e)
            return false
        }
    }

    private fun generateImage(prompt: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // 重置UI状态
                statusText.visibility = View.VISIBLE
                statusText.text = "正在生成图片..."
                generateButton.isEnabled = false
                downloadButton.visibility = View.GONE
                // 清除旧的图片点击事件
                generatedImage.setOnClickListener(null)
                // 清除当前图片URL
                currentImageUrl = null

                Log.d(TAG, "生成图片，提示文本: $prompt")
                
                val imageUrl = withContext(Dispatchers.IO) {
                    callCozeApi(prompt)
                }

                if (imageUrl != null) {
                    Log.d(TAG, "获取到图片URL: $imageUrl")
                    loadImage(imageUrl)
                } else {
                    statusText.text = "生成失败，请重试"
                    Log.e(TAG, "API返回了空的图片URL")
                }
            } catch (e: Exception) {
                statusText.text = "发生错误：${e.message}"
                Log.e(TAG, "生成图片时发生错误", e)
                Toast.makeText(this@MainActivity, "错误: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                generateButton.isEnabled = true
            }
        }
    }

    private fun loadImage(imageUrl: String) {
        // 保存当前图片URL
        currentImageUrl = imageUrl
        
        // 提前重置UI状态
        generatedImage.setOnClickListener(null)
        downloadButton.visibility = View.GONE
        
        Log.d(TAG, "开始加载图片: $imageUrl")
        
        Picasso.get()
            .load(imageUrl)
            .into(generatedImage, object : Callback {
                override fun onSuccess() {
                    statusText.text = "图片生成成功！"
                    Log.d(TAG, "图片加载成功")
                    
                    // 显示下载按钮
                    downloadButton.visibility = View.VISIBLE
                    Log.d(TAG, "设置下载按钮可见")
                    
                    // 添加点击图片查看全屏的功能
                    generatedImage.setOnClickListener {
                        currentImageUrl?.let { url ->
                            val intent = Intent(this@MainActivity, FullscreenImageActivity::class.java).apply {
                                putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, url)
                            }
                            startActivity(intent)
                        }
                    }
                }

                override fun onError(e: Exception) {
                    statusText.text = "图片加载失败"
                    Log.e(TAG, "图片加载失败", e)
                    // 隐藏下载按钮
                    downloadButton.visibility = View.GONE
                    // 清除点击事件
                    generatedImage.setOnClickListener(null)
                }
            })
    }

    private fun callCozeApi(prompt: String): String? {
        try {
            val jsonBody = JSONObject().apply {
                put("workflow_id", WORKFLOW_ID)
                put("parameters", JSONObject().apply {
                    put("input", prompt)
                })
            }

            Log.d(TAG, "API请求: $jsonBody")

            val request = Request.Builder()
                .url(COZE_API_URL)
                .addHeader("Authorization", "Bearer $COZE_TOKEN")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "请求失败: ${response.code}"
                    Log.e(TAG, errorMsg)
                    throw IOException(errorMsg)
                }

                val responseBody = response.body?.string()
                Log.d(TAG, "API响应: $responseBody")
                
                if (responseBody == null) {
                    Log.e(TAG, "响应体为空")
                    return null
                }

                val jsonResponse = JSONObject(responseBody)
                
                // 处理data字段，它可能是字符串或对象
                val dataField = jsonResponse.get("data")
                
                if (dataField is String) {
                    // 如果data是字符串形式的JSON
                    try {
                        val dataJson = JSONObject(dataField)
                        return dataJson.optString("output")
                    } catch (e: Exception) {
                        Log.e(TAG, "解析data字符串失败", e)
                        // 如果解析失败，直接返回data字符串
                        return dataField
                    }
                } else if (dataField is JSONObject) {
                    // 如果data已经是JSON对象
                    return dataField.optString("output")
                } else {
                    Log.e(TAG, "未能识别的data类型: ${dataField?.javaClass?.name}")
                    return null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "API调用失败", e)
            throw e
        }
    }
}