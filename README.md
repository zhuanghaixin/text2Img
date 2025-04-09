# Text2Img - 文本生成图片应用

一个基于Android的文本到图像生成应用，通过调用Coze API实现文本描述到图像的转换。

## 功能特点

- **文本到图像生成**：输入文本描述，生成相应的图像
- **图像预览**：点击生成的图片可全屏查看
- **图像下载**：支持将生成的图片保存到设备相册
- **简洁界面**：直观易用的用户界面
- **适配不同Android版本**：兼容Android 9及以上版本的存储权限管理

## 项目结构

```
Text2Img/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── example/
│   │   │   │           └── text2Img/
│   │   │   │               ├── MainActivity.kt          # 主界面
│   │   │   │               └── FullscreenImageActivity.kt # 全屏图片查看界面
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml                # 主界面布局
│   │   │   │   │   └── activity_fullscreen_image.xml    # 全屏图片查看布局
│   │   │   │   └── ...
│   │   │   └── AndroidManifest.xml                      # 应用配置文件
│   │   └── ...
│   ├── build.gradle                                     # 应用模块构建文件
│   └── ...
├── build.gradle                                         # 项目构建文件
└── ...
```

## 功能详解

### 1. 文本到图像生成

应用通过Coze API将用户输入的文本描述转换为图像。主要流程如下：

1. 用户在输入框中输入描述文本
2. 点击"生成图片"按钮发送请求到Coze API
3. 应用接收API返回的图像URL
4. 使用Picasso库加载并显示图像

### 2. 图像预览

生成的图像支持点击查看全屏预览：

- 点击图片打开全屏查看界面
- 全屏模式下隐藏状态栏和导航栏，提供沉浸式体验
- 再次点击图片可返回主界面

### 3. 图像下载

应用支持将生成的图片下载到设备相册：

- 点击"下载图片"按钮将图片保存至设备
- 自动适配不同Android版本的存储权限：
  - Android 10+: 使用MediaStore API
  - Android 9及以下: 使用传统File API
- 提供权限请求和错误处理机制
- 下载完成后显示提示信息

### 4. 权限使用说明

应用需要以下权限：

- `INTERNET`: 用于API通信和图片下载
- `READ_MEDIA_IMAGES`: Android 13及以上版本的图片保存权限
- `READ_EXTERNAL_STORAGE`: Android 10-12的存储权限
- `WRITE_EXTERNAL_STORAGE`: Android 9及以下版本的存储权限

## 使用方法

1. 在输入框中输入您想要生成的图片描述
2. 点击"生成图片"按钮
3. 等待图片生成并显示
4. 点击图片可全屏查看
5. 点击"下载图片"按钮将图片保存到相册

## 构建APK

### 方法一：使用Android Studio

1. 打开Android Studio并加载项目
2. 点击顶部菜单栏的`Build`
3. 选择`Build Bundle(s) / APK(s)`
4. 选择`Build APK(s)`
5. 等待构建完成，Android Studio会显示通知
6. 点击通知中的`locate`可找到生成的APK文件，通常在`app/build/outputs/apk/debug/`目录下

### 方法二：使用命令行

在项目根目录下执行以下命令：

```bash
# 在macOS/Linux系统
./gradlew assembleDebug    # 构建Debug版本APK
./gradlew assembleRelease  # 构建Release版本APK（需要签名配置）

# 在Windows系统
gradlew.bat assembleDebug    # 构建Debug版本APK
gradlew.bat assembleRelease  # 构建Release版本APK（需要签名配置）
```

构建完成后，APK文件位置：
- Debug版本：`app/build/outputs/apk/debug/app-debug.apk`
- Release版本：`app/build/outputs/apk/release/app-release.apk`

### 构建Release版本APK

要构建发布版本的APK（可发布到应用商店的版本），需要进行签名配置：

1. 在Android Studio中，点击顶部菜单的`Build` -> `Generate Signed Bundle / APK`
2. 选择`APK`
3. 创建或选择已有的密钥库(keystore)文件
4. 填写密钥库密码、密钥别名和密钥密码
5. 选择APK目标文件夹，并选择构建类型（通常选择release）
6. 点击`Finish`开始构建

## 技术栈

- Kotlin: 主要开发语言
- OkHttp: 网络请求
- Picasso: 图片加载和缓存
- Coroutines: 异步任务处理
- AndroidX: 现代Android开发组件

## 开发者笔记

- API密钥和工作流ID在代码中配置，可根据需要修改
- 支持不同Android版本的存储权限适配
- 使用协程进行异步操作，避免阻塞主线程
- 提供详细的日志输出，便于问题排查

---

© 2023 Text2Img。保留所有权利。
