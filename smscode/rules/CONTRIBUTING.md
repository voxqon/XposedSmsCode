# Contributing

感谢为 `smscode-rules` 补充验证码识别规则。

这个仓库只维护规则数据、Schema 和校验工具，不承载 Gradle/Kotlin 构建逻辑。贡献时请尽量保持内容仓边界清晰。

## 提交规则前

1. 为同一家机构、同一产品或同一场景族优先复用已有规则文件；确实需要新增时，优先放到类似 `rules/<country>/<domain>/<rule-set-id>.json` 的目录下。
2. 文件内顶层 `id` 必须与文件名一致，例如 `cn.bank.hsbc-china.json` 对应 `"id": "cn.bank.hsbc-china"`。
3. 每条规则至少提供 1 个可复现样例，并保证 `expectedCode` 能被 `codeRegex` 命中。
4. 示例短信必须使用脱敏、可公开文本，不提交真实用户短信、手机号、卡号或订单号。
5. 先写高特异性规则；只有在确有必要时才添加低优先级兜底规则。

## 建议的规则编写习惯

- 优先按领域和场景归并，例如 `rules/cn/bank/`、`rules/cn/carrier/`、`rules/cn/portal/`，不要把仓库演化成“一条短信一个文件”。
- `company`、`codeKeyword`、`notes` 等字符串字段不要写空字符串；没有值时请直接省略或使用 `null`。
- `packageNameHint` 仅在确有消费端价值时填写，并保持标准 Android 包名格式。
- `priority` 越高，越适合放在更具体、更可靠的规则上。
- 如果同一模板有多个可能命中的数字片段，优先把 `codeRegex` 收窄到只命中验证码本身。

## 本地检查

工具仅使用 Python 3.11+ 标准库，无需安装依赖。

校验规则与样例：

```bash
python3 tools/validate_rules.py
```

更新索引：

```bash
python3 tools/build_index.py
```

检查索引是否已经同步：

```bash
python3 tools/build_index.py --check
```

## Pull Request Checklist

- [ ] 若已有同类规则集，优先合并到现有文件而不是新建碎片文件
- [ ] 规则文件命名与顶层 `id` 一致
- [ ] 样例短信已脱敏且可公开
- [ ] `python3 tools/validate_rules.py` 通过
- [ ] `python3 tools/build_index.py --check` 通过
- [ ] 如规则内容有变化，已同步更新 `_meta/rules-index.json`
