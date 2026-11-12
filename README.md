# 木偶剧场（Wooden Puppet Demo）

一个面向 PICO Swan 的互动木偶剧场空间体验。玩家使用头显控制器的系统射线点击舞台目标，让木偶在五幕连续的机关冒险中移动、解谜并完成演出。

## 项目概览

- **技术栈**：Kotlin 2.1.20、Android、Compose / PICO SpatialUI、PICO Spatial SDK 6.1.9
- **构建工具**：Gradle Wrapper 8.13、Android Gradle Plugin 8.13.2
- **应用 ID**：`com.example.woodenpuppetdemo`
- **最低/目标 SDK**：Android API 35
- **目标设备**：PICO Swan；也可使用已配置的 PICO Spatial 模拟器进行基础验证
- **主要交互**：控制器系统射线指向舞台地面或机关并点击；木偶自动沿安全路径行走
- **第五幕**：机关供电、道具与灯光准备、提线排练、自动谢幕演出、掌声和彩带
- **内容结构**：五幕连续体验——剧院大门、八音盒剧场、发条之旅、风之工坊、木偶的最后一幕
- **内容资产**：木偶及舞台 GLB/USDZ 资源、程序化机关、环境材质、背景音乐和交互音效

## 目录结构

```text
app/src/main/java/                  Kotlin 应用与关卡逻辑
app/src/main/assets/                木偶、舞台和机关模型及音效署名
app/src/main/res/                   UI 图片、图标和 WAV 音频
app/src/test/                       JVM 单元测试
gradle/libs.versions.toml           Gradle、Kotlin 与 PICO SDK 版本
```

## 环境要求

1. macOS 或 Linux 开发环境。
2. JDK 17 或以上运行 Gradle；项目既有环境使用 JDK 21。源码的 Java/Kotlin 字节码目标为 11，这不代表可以用 JDK 11 运行 Android Gradle Plugin。
3. Android SDK Platform 35 和 Build Tools。
4. PICO Spatial SDK 6.1.9 所需的 Maven 仓库配置与本地工具链。
5. 如需安装到设备或模拟器，安装并配置 PICO CLI。

项目使用 `local.properties` 指向本机 Android SDK 和 PICO 工具路径。该文件不会提交到 Git，请根据本机环境自行配置；不要提交账号、密钥或访问令牌。

## 获取项目与安装依赖

```sh
git clone https://github.com/1249186599/puppet-theater.git
cd puppet-theater
export JAVA_HOME="/path/to/jdk-21"
```

在根目录创建仅供本机使用的 `local.properties`：

```properties
sdk.dir=/path/to/android-sdk
spatial.tools.dir=/path/to/pico-spatial-tools
```

也可用 Android Studio 打开根目录并配置 SDK、Gradle JDK。Gradle 从 `settings.gradle.kts` 中的 Google、Maven Central、Gradle Plugin Portal 和 PICO Maven 仓库解析依赖；首次构建需要网络。无需 Node.js 或 npm。仓库包含运行所需的模型和音频，普通构建不需要重新生成资源。

## 构建与测试

在项目根目录执行：

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

依赖全部缓存后可加 `--offline`。静态检查使用 `./gradlew :app:lintDebug`。
Windows 使用 `gradlew.bat`，并在本机设置 `JAVA_HOME`。

生成的调试 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

连接 PICO 设备或启动模拟器后，可使用 PICO CLI 安装并启动：

```sh
pico-cli app install app/build/outputs/apk/debug/app-debug.apk --replace --device <target>
pico-cli app launch com.example.woodenpuppetdemo --device <target>
```

将 `<target>` 替换为设备序列号。已配置 Android Platform Tools 时，也可使用：

```sh
adb -s <target> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <target> shell am start -n com.example.woodenpuppetdemo/.platform.LaunchActivity
```

应用从第一幕开始；使用系统射线点击机关或可行走地面，按节目单提示推进关卡，可重置当前幕。第五幕完成排练后，点击主舞台开始自动演出。

发布构建命令为 `./gradlew :app:assembleRelease`。当前未配置发布签名，产生的 release APK 不能当作已签名正式安装包；提交安装包时需在本地配置签名，勿将签名文件或密码上传。设备仪器测试使用 `./gradlew :app:connectedDebugAndroidTest`，需要兼容且已连接的设备。

设备验证应至少检查：进入舞台、系统射线点击、木偶移动、各幕机关流程、重置、音频加载和崩溃日志。模拟器结果不能替代真实 PICO 设备兼容性确认。

## 开源与资源说明

运行所需的模型、图片和音频资源保存在 `app/src/main/assets` 与 `app/src/main/res` 中。音频来源和许可信息记录在 [SOUND-CREDITS.txt](app/src/main/assets/SOUND-CREDITS.txt)，其他来源及尚待确认事项见 [RESOURCE-NOTICES.md](RESOURCE-NOTICES.md)。已有来源记录不能替代模型、纹理和图片的公开再分发授权；公开前须由项目所有者确认这些权限。

本仓库尚未授予整体开源许可证；用于审核或公开可见不等于允许任意再分发。第三方依赖适用各自许可证，通过 Maven 获取，不随源码提交 SDK 的 AAR/JAR。Gradle Wrapper JAR 是构建启动器，予以保留。

`tools/` 中的两个 Python 脚本是早期动画加工工具，需要单独提供原始 GLB；它们不完整复现当前 14 动画片段的木偶及最终修正，不能直接覆盖已交付模型。

构建缓存、本机配置、签名文件、测试日志、APK、视频、截图和历史快照不会纳入版本库。

## 发布范围

本仓库用于提交 PICO Vibe Lab 审核。最终提交版本应以 Git 的完整 commit ID 为准，并在 PICO 平台填写与该版本对应的仓库地址和 commit ID。
