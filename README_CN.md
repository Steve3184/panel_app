# Panel Android 客户端

[English](README.md) | **简体中文**

[The Panel](https://github.com/Steve3184/panel) 的 Android 封装与代理客户端。该应用让您能够在 Android 设备上获得接近原生的服务器管理体验

您可以直接从 **Releases** 页面下载已编译好的安装包：
👉 **[下载 APK](https://github.com/Steve3184/panel_app/releases)**

## ✨ 功能特性

- **动态端口分配**：自动寻找并绑定空闲的高位端口，彻底避免端口冲突
- **多实例支持**：允许用户同时安装并打开多个不同包名的该应用，互不干扰
- **本地 Ktor 代理**：内置 Ktor 服务器处理 API 请求和 WebSocket 代理，确保与后端无缝通信
- **多语言支持**：已内置 简体中文、繁体中文、英文、日语

## 🚀 快速开始

### 环境要求

- Android Studio Ladybug 或更高版本
- JDK 17 或更高版本
- 一个已运行的 [The Panel](https://github.com/Steve3184/panel) 后端实例

### 关键步骤：更新前端资源

项目的 `assets` 文件夹默认不包含前端编译产物。在编译 APK 之前，你 **必须** 执行以下命令来下载并更新前端资源：

```bash
./gradlew app:updateFrontend
```

### 构建与运行

1. 克隆本仓库
2. 执行上述 `updateFrontend` 任务
3. 在 Android Studio 中打开项目
4. 构建并安装应用到您的设备
5. 首次启动时，输入您的服务器地址（例如：`http://your-ip:3000`）

## 🛠 技术栈
- **编程语言**：Kotlin
- **服务端**：Ktor (Netty 引擎)
- **HTTP 客户端**：OkHttp 4
- **UI 框架**：XML ViewBinding + Material Design 3

## 📄 许可证
本项目基于 MIT 许可证分发。详情请参阅 [LICENSE](LICENSE) 文件