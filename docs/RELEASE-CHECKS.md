# PICO Vibe Lab 发布前检查

检查日期：2026-11-12。此报告记录本地发布候选，尚未推送 GitHub。

## 发布对象

- 仓库目标：https://github.com/1249186599/puppet-theater.git
- 分支：`main`
- 提交信息：`Initial release for PICO Vibe Lab`
- 实际应用根目录名：`wooden-puppet-demo`；当前工作区 `hello-spatial` 主要保存制作资产和验证记录。
- 原项目已有 Git 初始化、203 个暂存文件，无已有提交、无暂存外修改。原项目及其索引保持不变。
- 发布副本名：`puppet-theater-release`。逐文件复制原项目已暂存的源码和运行资源；只修改忽略规则、README 和音效署名，新增资源说明与检查文档。
- 完整文件清单：[RELEASE-FILES.txt](RELEASE-FILES.txt)。运行源码、模型、图片、音频保持原字节；仅 SOUND-CREDITS 文本内容补充。

## 安全与完整性

- 对发布候选扫描了常见私钥、GitHub/云服务 Token、密码赋值、URL 内凭据、本机路径与邮箱。
- 没有发现上述敏感内容。GLB JSON 和图片文本元数据检查未发现个人路径或邮箱；二进制随机字节产生的邮箱样式命中未出现在可读元数据中。
- 所有 73 个 GLB 均未引用包外 URI；未发现需另行下载的模型纹理或 buffer。
- 最大文件 `chapter5_approved_props.glb` 为 33,170,896 字节。运行资源随源码保留。
- 忽略规则的 18 个代表性路径检查全部通过；包括依赖缓存、构建结果、环境变量文件、密钥、个人配置与历史验证记录。Gradle Wrapper JAR 和运行 GLB 未被误排除。
- `git diff --cached --check` 通过。
- 模式扫描不能证明不存在一切形式的敏感信息；本检查针对发布文件，不公开本地日志、历史快照或个人配置。

## 构建与测试结果

尝试原项目 Gradle 任务：

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline --no-daemon
```

Gradle 在执行任务前因会话权限限制失败：`FileLockContentionHandler` 创建套接字时出现 `java.net.SocketException: Operation not permitted`。已将 Gradle 缓存复制到工作区可写位置，仍存在该套接字限制。因此本轮没有完成 APK 构建、Gradle 测试任务或 Android Lint，不能据此判断 Android 编译是否通过。

替代验证：使用本机缓存的 Kotlin 2.1.20 编译器和 JUnit 4.13.2，以 JVM target 11 编译项目的纯 Kotlin 业务逻辑及全部 18 个现有 JVM 测试类，运行结果为 **75 tests / 0 failures**。
`FinaleConfetti.kt` 与渲染代码同文件的 `RibbonFrame` 和 `FinaleRibbonMotion` 声明逐字提取到临时编译目录；逻辑和测试未改写，未使用渲染替身。该验证不覆盖 Android/PICO SDK 编译、资源打包、真实渲染或设备交互。

本轮没有运行设备仪器测试、重新安装 APK 或进行完整头显通关。既有验证记录不能替代当前提交的完整设备验证。

## 资源和依赖许可

IENBA 的 Door Creak 和 Sadiquecat 的 Applause Edited 1 来源页均标注 CC0；来源链接和加工说明已写入 `SOUND-CREDITS.txt`。
项目所有者后续说明模型和图片主要由 Tripo 与 Codex 生成，其中 Tripo 使用付费积分。已补充该来源信息和平台条款链接；不再将主要资源笼统标记为来源不明。付费积分的具体条款、逐文件对应关系及参考图/模板资源许可尚未完整核实；详情见 [RESOURCE-NOTICES.md](../RESOURCE-NOTICES.md)。未擅自添加整体开源许可证。
第三方 SDK 和 Maven 依赖未随源码提交二进制，使用与发布仍需遵守它们各自的条款。

## 推送前仍需解决

1. 项目所有者确认资源可公开，并明确同意推送本候选。
2. 在允许 Gradle 所需本地通信的环境重新执行上述构建、测试和 Lint；当前测试通过范围仅为独立 JVM 验证。
3. 恢复 GitHub 可访问性并核查远端历史。当前 `git ls-remote` 返回 `Could not resolve host: github.com`，网页核查也未成功，因此无法确认仓库存在性、可见性、写入权限或远端 `main` 内容。
4. 获得推送确认后，先读取远端分支；如果已有历史，必须以保留历史的方式整合并重新核查，不能直接用当前根提交覆盖远端。禁止强制推送。

当前项目未配置 release 签名；需要提交正式安装包时应在本地完成签名，密钥及密码不得提交。
