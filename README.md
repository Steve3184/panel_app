# Panel Android Client

**English** | [简体中文](README.md)

An Android wrapper and proxy client for [The Panel](https://github.com/Steve3184/panel). This application allows you to manage your servers via a native-like experience on Android devices.

You can download the latest pre-compiled APK from the **Releases** page:
👉 **[Download APK](https://github.com/Steve3184/panel_app/releases)**

## ✨ Features

- **Dynamic Port Allocation**: Automatically finds and binds to an available high-range port to avoid conflicts.
- **Multi-Instance Support**: Supports multiple installations (different package names) running simultaneously without port collisions.
- **Local Ktor Proxy**: Built-in Ktor server handles API requests and WebSocket proxying for seamless communication with the backend.
- **Multi-language Support**: Includes English, Simplified Chinese, Traditional Chinese, and Japanese.

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- JDK 17 or higher.
- An active backend instance of [The Panel](https://github.com/Steve3184/panel).

### Mandatory Step: Updating Frontend Assets

The `assets` folder does not include the frontend build by default. You **must** run the following command to download and sync the latest frontend resources before building the APK:

```bash
./gradlew app:updateFrontend
```

### Build and Run
1. Clone this repository.
2. Run the `updateFrontend` task as mentioned above.
3. Open the project in Android Studio.
4. Build and install the app on your device/emulator.
5. Enter your server URL (e.g., `http://your-ip:3000`) on the first launch.

## 🛠 Technical Stack
- **Language**: Kotlin
- **Server**: Ktor (Netty engine)
- **HTTP Client**: OkHttp 4
- **UI**: XML ViewBinding + Material Design 3

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
