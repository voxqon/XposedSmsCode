# 本地构建与依赖来源

日期：2026-10-29 至 2026-10-30。

## 运行与构建边界

应用在 Android 上本地处理短信，不使用授权服务器、遥测服务器、更新服务器、
在线支付或远程规则服务。APK 仅为用户主动执行的 HTTPS WebDAV 备份、
文件列表和恢复保留 INTERNET 权限，仍移除 ACCESS_NETWORK_STATE、
REQUEST_INSTALL_PACKAGES 和 BILLING 权限。WebDAV 账号加密保存在本机，
不随数据备份导出。Hook 运行于宿主进程，因此也从
Hook 使用的源码中删除了规则下载与日志上传实现，不能只依赖应用权限限制。

Android、Root 与兼容的 Xposed/LSPosed 是运行环境。UI、Room、Kotlin、
协程及 Xposed API 等第三方库仍保留其本地实现，并非全部重新编写：
构建优先使用本地 `third_party/maven`，缺少依赖时从 Google Maven、
Maven Central 和 Gradle Plugin Portal 获取。仅有本地文件，不代表已证明全部第三方代码无漏洞。

## 源码来源

以下目录由原 Git 子模块改为本仓库普通源码，保留迁入前的本地修改及许可证：

| 本地目录 | 原始提交 |
|---|---|
| `build-logic` | `37c362b2011dd626c075a6c60592e10584233bd3` |
| `magisk-ui-kit` | `86e69a4160490c411b8e431c1a02c0ff591ec9cc` |
| `magisk-xposed-kit` | `51c2b538fba215d9ab745968052d8bec94b7ff5a` |
| `smscode/core` | `0153e88335507993d0ff43415bce3a8180df70cf` |
| `smscode/rules` | `51cd2daf5d528cee94300d0796aa5ce842988c51` |

在线更新及支付接口源码已删除。各目录的 LICENSE 保留，原始 Git
指针及迁入时的差异备份留在本机 `.git/vendoring-backup-20261029`。

## 本地二进制依赖

可选的本地缓存 `third_party/maven` 保存 Maven 原始 POM、Gradle module 元数据和 JAR/AAR，
包括编译插件与测试库；保留二进制内的许可证及 NOTICE。
`third_party/artifacts.json` 记录路径、坐标、大小、SHA-256 与 POM 声明的许可证。
这些文件由当前工作站 Gradle 缓存及构建时补齐的公开制品迁入，
清单用于检查本地文件变动，不是上游签名或完整源码审计的替代品。

Gradle 9.9.0-milestone-2 的安装包由 wrapper 从官方发行地址取得并校验：

```text
6d8120ea1012280478bed6c3656a1050c3da745a605bc3a846df561f50313c8f
```

构建不自动下载 SDK 或 JDK。工作站本次下载的 JDK 27、Android SDK 平台
37.2、构建工具 37.0.0 等保存在忽略目录 `.tools/`。
迁移工作区时需要一并保留这些本地工具，或自行提供同版本工具；
缺失时构建会失败，不会回退到远程源。

## 构建

安装 Python 3.11+。设置 JDK 27 的 `JAVA_HOME` 和 Android SDK 的
`ANDROID_HOME`；本机已有 `.tools/` 工具时，以下脚本可自动选择：

```text
python scripts/check_local.py
```

检查包括本地制品哈希、安全策略、规则格式与样例、规则索引、签名策略和打包测试、
核心与应用单元测试、模块边界检查及 Debug APK。生成 APK 后还会检查实际权限、
数字签名、Xposed 入口类、规则文件哈希及内置隐私文档，并限制安装包不超过
15 MiB。规则工具仅使用 Python 标准库，
不需要 pip 包。具备完整本地缓存时，可以传入 `--offline`。
也可在配置好环境后直接运行：

```text
gradlew.bat :app:assembleGithubDebug
```

Windows 默认使用 `python`，其他系统默认使用 `python3`；
可用 `-PpythonExecutable=绝对路径` 指定解释器。
Debug 使用本机 Android 调试签名，不要求原作者私钥。
默认 `minifyDebug=true`，开启 R8 代码优化和资源裁剪，产物不可连接调试器。
图标库只保留实际使用的代码；应用图标通过 Android 系统接口异步加载，
使用 2 MiB 内存缓存。UI 模块与应用共用 Compose BOM，已移除 Coil、
未使用的自适应布局和 LiveData 桥接依赖。

需要断点调试时显式关闭优化，此时包体会明显增大：

```text
gradlew.bat :app:assembleGithubDebug -PminifyDebug=false
```

恢复普通构建命令即可重新生成精简安装包；关闭优化的诊断包不适用
`check_local.py` 的 15 MiB 体积门槛。Release 同样启用代码和资源裁剪。

Release 仍要求自行提供完整签名配置。Debug 签名通常不能直接覆盖
原版签名的安装；卸载原版会删除应用数据，需先自行做好本地备份。

输出位于 `app/build/outputs/apk/github/debug/`。完整库集合包含平台变体，
但不同操作系统或未运行过的构建任务仍须实际离线验证，不能仅凭缓存存在推断成功。

## 安装包体积验证（2026-10-30）

| 检查项 | 优化前 | 优化后 |
|---|---:|---:|
| APK | 82.82 MiB | 6.09 MiB |
| DEX 文件数 | 25 | 1 |
| DEX 总体积 | 81.80 MiB | 5.64 MiB |

安装包缩小约 92.6%。保留短信识别、复制、通知、自动输入、规则导入、
备份恢复和历史记录的现有实现；保留 Xposed 反射入口与 Hook 的混淆保护。
不再打入未使用的图标实现、Coil 和自适应布局代码。

`scripts/check_local.py` 完整通过：核心、应用与 UI 模块共 581 项测试，
签名与构建策略另有 13 项，Python 规则工具另有 15 项；合计 608 项通过，
1 项因 Windows 符号链接权限限制跳过。APK 签名、离线权限、Xposed 入口类、
12 份规则文件及隐私文档验证通过。15 MiB 体积检查也已用原始大包验证拒绝行为。
裁剪构建新增使用的工具制品已补入本地仓库及哈希清单，构建保持离线。

验证记录位于 `.tools/apk-size-validation.log`。当前未连接 Android 设备，
尚未验证真机 / LSPosed 下的短信与自动输入链路。
