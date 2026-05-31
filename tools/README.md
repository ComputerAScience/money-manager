# Tools

## migrate-backup-v8.js

把旧版 Money Manager 备份 JSON 转换为 schema 8，也就是当前的“机构管理 + 扁平资产”结构。

App 升级时会自动迁移手机本机数据；这个脚本主要用于你想在导入前先检查或手动转换备份文件的场景。

```bash
node tools/migrate-backup-v8.js old-backup.json migrated-backup.json
```

如果省略输出文件，迁移后的 JSON 会输出到终端：

```bash
node tools/migrate-backup-v8.js old-backup.json > migrated-backup.json
```

迁移规则：

- 旧银行存款和银行理财合并为一条银行资产的总金额
- 旧银行负债生成一条单独的负债资产
- 旧券商持仓和券商现金合并为一条投资资产的总金额
- 旧持股备注会追加到资产备注
- 自定义资产类型会写入备份里的 `settings.customCategories`
- `schemaVersion` 和 `version` 会更新为 `8`

运行脚本测试：

```bash
node tools/migrate-backup-v8.test.js
```
