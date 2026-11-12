# smscode-rules

`smscode-rules` 是一个独立维护的验证码识别规则仓库，面向 `XposedSmsCode`、`xinyi-relay` 及其共享运行时。

目标：
- 将易变的验证码识别规则从应用版本中解耦
- 允许按机构/产品持续补规则，而不必等待整包发版
- 为社区贡献规则提供统一格式、样例和校验入口

## 仓库结构

- `rules/`：按国家 / 领域分类的规则文件
- `schemas/`：规则 Schema
- `tools/`：校验与索引生成脚本
- `_meta/rules-index.json`：生成后的规则索引

## 规则文件格式

每个规则文件描述一个规则集，推荐按**国家 / 领域 / 场景族**归并，而不是按“单条短信模板”拆文件。

推荐实践：
- 同一品牌 / 产品的多条模板放在同一个规则文件里
- 同一类场景（例如统一门户登录、银行登录、运营商活动）可以按领域目录分组
- 只有当规则所有权、优先级策略或维护边界明显不同，才拆成新文件

当前规则模型兼顾：
- **现有应用兼容字段**：`company`、`codeKeyword`、`codeRegex`
- **未来扩展字段**：`senderRegex`、`packageNameHint`、`priority`、`examples`

示例：

```json
{
  "specVersion": 1,
  "id": "cn.carrier.china-mobile-cloud-drive",
  "name": "中国移动云盘",
  "locale": "zh-CN",
  "updatedAt": "2026-05-01T00:00:00+08:00",
  "rules": [
    {
      "id": "reward-code",
      "company": "中国移动云盘",
      "codeKeyword": "领奖验证码",
      "codeRegex": "(?<=领奖验证码[:：])\\d{4,8}",
      "priority": 100,
      "examples": [
        {
          "body": "【验证密码】领奖验证码:1234，5分钟有效……【中国移动云盘】",
          "expectedCode": "1234"
        }
      ]
    }
  ]
}
```

## 设计约束

- 规则文件使用 `UTF-8` 编码，统一为 `JSON`
- 每条规则至少带 1 个可复现样例
- `codeRegex` 必须是可编译正则
- 样例短信必须能命中 `expectedCode`
- 若配置了 `company` / `codeKeyword`，样例正文必须包含对应文本

## 当前兼容策略

现阶段客户端可以直接读取下列字段映射到现有 `SmsCodeRuleSpec`：

- `company`
- `codeKeyword`
- `codeRegex`

其余字段可供后续版本扩展：

- `senderRegex`
- `packageNameHint`
- `priority`
- `notes`
- `examples`

## 工具

工具仅使用 Python 3.11+ 标准库，无需安装 pip 包。校验器覆盖本地 Schema
使用的约束；新增 Schema 约束时须同步实现并测试，否则校验器会报错。

校验全部规则：

```bash
python3 tools/validate_rules.py
```

生成索引：

```bash
python3 tools/build_index.py
```

校验索引是否已同步：

```bash
python3 tools/build_index.py --check
```

`build_index.py` 在规则内容未变化时不会改写 `_meta/rules-index.json`，方便本地重复执行和 CI 校验。

## 贡献建议

- 规则文件建议放在 `rules/<country>/<domain>/<rule-set-id>.json`
- 规则文件名使用 `<rule-set-id>.json`，并与文件内顶层 `id` 保持一致
- 优先为“同一机构 / 同一产品 / 同一场景族”归并一份规则文件，避免碎片化拆分
- 不要为单条短信模板单独创建文件；优先往现有规则集里补规则
- 优先写高特异性规则，再用较低 `priority` 补兜底规则
- 样例短信请使用可公开的脱敏文本，不要提交真实用户数据
- 提交前至少运行一次校验与索引检查

更细的提交约束见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 初版规则

当前已包含：
- 中国移动云盘活动领奖验证码
- 汇丰银行中国验证码
- 统一门户登录验证码

后续建议继续补充：
- 银行
- 运营商
- 电商
- 出行 / 生活服务
- 海外短信模板
