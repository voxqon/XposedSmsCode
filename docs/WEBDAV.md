# WebDAV 备份

在「设置 → 其他 → WebDAV 备份」填写 WebDAV HTTPS 地址、用户名及密码
（部分服务需要应用专用密码），点击「保存设置」。地址应指向已有的 WebDAV
根目录或可写目录，例如 `https://example.com/dav/`，不要包含账号、查询参数
或片段。应用将在这个目录下新建 `XposedSmsCode` 文件夹。

- 上传按钮：选择配置、规则、短信记录或数据库，再生成并上传备份。
- 刷新按钮：列出远端 `.scebak` 和 `.zip` 备份，按修改时间从新到旧排列，
  显示文件名、大小及修改时间。尚未创建的目录显示空列表。
- 点击文件：下载到本机临时文件，再使用现有恢复确认与数据选择流程。
- HTTPS 证书必须可信；不支持明文 HTTP、忽略证书错误或自动跟随重定向。
  重定向响应需要用户改填最终 WebDAV 地址。

无定时或自动备份。账号通过 Android Keystore 加密保存在 `noBackupFilesDir`，
与普通设置导出和数据库备份分开。临时文件在操作结束或取消恢复后清理。
备份文件沿用现有格式，包含数据库时为 ZIP；文件本身不额外加密。

## 协议约定

依据 RFC 4918（https://www.rfc-editor.org/rfc/rfc4918）：

| 操作 | 请求 | 成功与失败行为 |
|---|---|---|
| 创建目录 | `MKCOL <base>/XposedSmsCode/` | 201 成功；405 时通过 Depth 0 PROPFIND 确认已有对象是集合；其他状态失败 |
| 上传 | `PUT <folder>/<unique-name>` | 200 / 201 / 204 成功；发送 `If-None-Match: *` 防止覆盖；不自动重试 |
| 文件列表 | `PROPFIND <folder>/`，`Depth: 1` | 207，只读取 DAV 命名空间中成功的 propstat；404 显示空列表 |
| 下载 | `GET <folder>/<name>` | 仅接受 200，流式写入并限制实际读取字节数 |

支持 UTF-8 Basic 认证和无账号访问。XML 请求明确查询 `resourcetype`、
`getcontentlength`、`getlastmodified`。列表只接受同源、当前目录直接子文件，
不使用远端 displayname 作为路径，忽略目录及无效属性。XML 响应限制 4 MiB，
下载限制 256 MiB，恢复继续执行既有 JSON / ZIP 大小与完整性检查。
连接超时 15 秒，读写超时 60 秒，单次请求最长 120 秒。
认证、权限及其他 HTTP 错误会显示失败状态，不回显凭据和服务器响应正文。

## 本地验证

这是通用协议客户端，未指定任何云盘服务，因此没有可查询的服务方 mock。
`WebDavClientTest` 使用本地 HTTP 服务器验证实际客户端请求及边界条件，
HTTP 测试入口仅允许回环地址，生产配置始终要求 HTTPS。
`WebDavSimulationTest` 可连接基于上述协议编写的 Postman stand-in：

```text
postman simulation run postman/simulations/webdav.sim.yaml --port 4019 --no-history --output ndjson
```

设置 `WEBDAV_SIMULATION_URL=http://127.0.0.1:4019/webdav` 后运行
`:runtime:testGithubDebugUnitTest --tests *WebDavSimulationTest --rerun`。
模拟结果只证明客户端对所模拟响应的行为，不能代替真实服务器、Android
Keystore 与真机恢复验证。

2026-10-30 本地验证：12 项客户端测试、2 项 Postman 联调测试通过，
应用相关三个模块共 108 项测试通过，模块边界检查与 Github Debug APK
构建通过。本地 Maven 制品清单校验通过。未使用真实 WebDAV 账号，
未在设备上安装本次构建或验证 Android Keystore 与真机恢复。
