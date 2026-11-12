# Privacy Policy / 隐私政策

**Effective Date / 生效日期:** 2026-10-30

This policy describes the modified source tree in this repository. Previously released upstream APKs and remote services may behave differently.

本政策描述当前仓库修改后的源码行为，不代表此前发布的上游 APK 或远程服务行为。

[English Version](#privacy-policy-for-xposedsmscode) | [中文版本](#xposedsmscode-隐私政策)

---

## Privacy Policy for XposedSmsCode

### 1. Introduction
Welcome to XposedSmsCode ("we," "our," or "us"). We are committed to protecting your privacy and ensuring you understand how we handle your data. This Privacy Policy explains what information we collect, why we collect it, and how we safeguard it.

### 2. Information We Collect and Use

#### 2.1 Verification Code Processing
Our application's core functionality is to extract verification codes from SMS messages in Xposed environments, then process them locally according to your settings (for example, record, intercept, copy, and auto-input behaviors). Depending on distribution/channel and enabled features, we may request the following permissions:

*   **POST_NOTIFICATIONS:** Used to display verification-code notifications.
*   **QUERY_ALL_PACKAGES / PACKAGE_USAGE_STATS:** Used for app list display and app usage based sorting in settings.

**Usage:**
*   We STRICTLY use these permissions only for the stated features.
*   We DO NOT collect, store, or transmit your personal conversations or contacts to our servers.
*   SMS content parsing and matching are performed locally on your device.

#### 2.2 Local Processing and Network Access
*   All SMS content parsing and matching is performed locally. When you manually back up records or the database to WebDAV, the selected data, including SMS content and verification codes, is uploaded to the server you configure.
*   The Internet permission is used for optional, user-initiated HTTPS WebDAV backup, file listing and restore. There is no scheduled or automatic upload. Remote rule downloads, APK updates, billing and external browser links have been removed. Rules are bundled in the APK or imported locally.
*   This version removes the remote licensing SDK and activation service integration. Code processing follows local settings and does not require a remote license, device registration, or license renewal.
*   LSPosed/Xposed module activation and hook health are checked locally.
*   Both distribution flavors operate locally and do not require Google Play services or purchases. The privacy document is bundled with the APK.

### 3. Technical Diagnostics
Diagnostic and log data is generated and kept locally. Telemetry network transport and installation-identifier creation have been removed from the local logging implementation. Legacy settings cannot enable reporting. Debug logs redact sensitive fields by default; a developer can explicitly build with plaintext logging enabled. Logs or backups leave the device only when you choose to export, share or back up to WebDAV; exported files can contain sensitive information.

### 4. Data Retention
*   **Local Storage:** Extracted records are stored in the app's private database. Record retention follows your configured limits. Local logs follow the logging retention settings.
*   **Backups:** Android automatic backup and device transfer exclude application data. Manual configuration export excludes IPC authentication tokens, legacy remote license state, telemetry settings and internal migration keys. Restore refuses those protected keys. Exported record backups can contain SMS content and verification codes.
*   **WebDAV:** The URL and credentials are encrypted using an Android Keystore key and stored in app-private storage excluded from backups. Remote backup files are stored in the server's `XposedSmsCode` folder. Files retain the existing backup format and are not additionally encrypted by the app; HTTPS protects transport. Server retention and access are controlled by your provider. Delete remote copies separately when no longer needed.
*   **Deletion:** You can delete local records, clear app data, or uninstall the application. Exported files and copies you shared must be deleted separately.
*   **Previous Services:** Removing this integration does not delete records that an upstream licensing or telemetry service may already hold. This repository cannot verify or control those services' retention policies.

### 5. Data Sharing and Disclosure
We **DO NOT** sell, trade, or otherwise transfer your personally identifiable information to outside parties.
*   This version disables automatic telemetry export and removes remote licensing requests.
*   Verification code content is processed locally on device. Your explicit WebDAV backup operation sends the selected backup data to your configured server.
*   User-initiated sharing is handled by the application you select.

### 6. Security
We implement a variety of security measures to maintain the safety of your personal information.
*   The app operates ensuring that sensitive permissions are used solely for the stated core purpose.
*   Cleartext HTTP is disabled by the app's network security configuration.
*   Cross-process auto-input requests require a per-installation authentication token and are addressed to the system or module package. The token is rotated once when upgrading from the older backup behavior.
*   Automatic SMS deletion and read-state changes require the complete sender, body and original timestamp to match. If identity cannot be confirmed, the operation is skipped.

### 7. Children's Privacy
Our Service does not address anyone under the age of 13. We do not knowingly collect personally identifiable information from children under 13.

### 8. Changes to This Privacy Policy
We may update our Privacy Policy from time to time. Thus, you are advised to review this page periodically for any changes. We will notify you of any changes by posting the new Privacy Policy on this page.

### 9. Contact Us
For questions about this version, open an issue in the project's GitHub repository.

---

## XposedSmsCode 隐私政策

### 1. 简介
欢迎使用 XposedSmsCode（以下简称“我们”）。我们致力于保护您的隐私，并确保您了解我们如何处理您的数据。本隐私政策解释了我们收集哪些信息、为什么收集以及如何保护这些信息。

### 2. 我们收集和使用的信息

#### 2.1 验证码处理
我们应用的核心功能是在 Xposed 环境中提取短信验证码，并按您的本地设置进行处理（例如记录、拦截、复制、自动输入）。根据发行渠道与功能启用情况，应用可能申请以下权限：

*   **通知权限 (POST_NOTIFICATIONS):** 用于展示验证码通知。
*   **应用查询/使用情况权限 (QUERY_ALL_PACKAGES / PACKAGE_USAGE_STATS):** 用于设置页应用列表展示和按使用频率排序。

**使用说明：**
*   我们**仅**将这些权限用于声明功能。
*   我们**不会**收集、存储或传输您的个人对话或联系人到我们的服务器。
*   所有针对短信内容的解析与匹配均在您的设备本地进行。

#### 2.2 本地处理与网络访问
*   短信内容的解析与匹配全部在本地完成。当您主动将记录或数据库备份至 WebDAV 时，所选数据（可能包含短信正文和验证码）会上传至您配置的服务器。
*   联网权限仅用于可选的、由您主动执行的 HTTPS WebDAV 备份、文件列表和恢复，无定时或自动上传。已移除远程规则下载、应用内更新、支付接入和外部浏览器链接。规则来自 APK 内置资源或本地导入。
*   当前版本已移除远程授权 SDK 和激活服务接入。验证码处理由本地设置控制，不要求远程授权、设备注册或授权续期。
*   LSPosed/Xposed 模块启用状态和 hook 健康状态在本地检测。
*   两个发行渠道均在本地运行，不要求 Google Play 服务或购买。隐私政策随 APK 内置。

### 3. 技术诊断数据
诊断与日志数据在本地生成和保存。本地日志实现已移除遥测网络传输与安装标识生成，旧设置无法重新启用上报。Debug 日志默认脱敏，开发者可通过显式构建参数启用明文日志。日志或备份仅在您主动导出、分享或备份至 WebDAV 时离开设备，导出的文件可能包含敏感信息。

### 4. 数据保留
*   **本地存储：** 提取的记录存储在应用私有数据库中，按您配置的数量上限保留；本地日志遵循日志保留设置。
*   **备份：** Android 自动备份和设备迁移均排除应用数据。手动配置导出排除 IPC 认证令牌、旧远程授权状态、遥测设置及内部迁移键；恢复时也拒绝这些受保护的键。记录备份可能包含短信正文和验证码。
*   **WebDAV：** 地址和账号密码使用 Android Keystore 密钥加密后保存在应用私有目录，不写入备份。远端备份存放于服务器的 `XposedSmsCode` 文件夹，沿用现有备份格式，应用不另行加密文件内容；传输通过 HTTPS 保护。远端数据的保留与访问由您和服务提供方控制，无需保留时请单独删除远端副本。
*   **删除：** 您可以删除本地记录、清除应用数据或卸载应用。已导出的文件及已分享的副本需要另外删除。
*   **此前的服务：** 移除服务接入不会删除上游授权或遥测服务可能已持有的数据。当前仓库无法核实或控制这些服务的数据保留政策。

### 5. 数据共享与披露
我们**不会**向外界出售、交易或以其他方式转移您的个人身份信息。
*   当前版本关闭自动遥测导出，并移除了远程授权请求。
*   验证码内容在本地处理。您主动执行 WebDAV 备份时，所选备份数据会发送到您配置的服务器。
*   您主动进行的分享由您选择的应用处理。

### 6. 安全性
我们实施了多种安全措施来维护您个人信息的安全。
*   应用程序确保敏感权限仅用于声明的核心目的。
*   应用网络安全配置禁用明文 HTTP。
*   跨进程自动输入请求要求每次安装生成的认证令牌，并仅发送到系统包或模块包。由旧备份行为升级时会轮换一次令牌。
*   自动删除短信和修改已读状态需要完整发件人、正文及原始时间戳匹配；不能确认身份时跳过操作。

### 7. 儿童隐私
我们的服务不针对 13 岁以下的任何人。我们不会有意收集 13 岁以下儿童的个人身份信息。

### 8. 本隐私政策的更改
我们可能会不时更新我们的隐私政策。因此，建议您定期查看本页面以了解任何更改。我们将通过在此页面上发布新的隐私政策来通知您任何更改。

### 9. 联系我们
关于本版本的问题，请在项目的 GitHub 仓库提交 issue。
