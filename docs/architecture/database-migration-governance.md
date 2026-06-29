# ONES-ADMIN 数据库迁移治理规范

更新时间：2026-06-29

## 1. 目标

ONES-ADMIN 后端数据库结构统一由 Flyway 管理，禁止再通过 `schema.sql` 或应用启动时隐式 DDL 补偿修改表结构。

核心目标：

- 每次表结构变更都有版本、脚本、审计记录。
- Jenkins 可以在测试阶段发现迁移脚本命名、版本和危险 SQL 问题。
- 交付说明必须包含数据影响和回滚方式。

## 2. 脚本位置与命名

迁移脚本统一放在：

```text
server/src/main/resources/db/migration
```

命名格式：

```text
V版本号__小写下划线说明.sql
```

示例：

```text
V1__init_schema.sql
V2__add_file_metadata.sql
V3__add_system_parameter.sql
```

要求：

- 版本号从 1 开始连续递增，不允许跳号。
- 说明只使用小写字母、数字和下划线。
- 不允许恢复 `server/src/main/resources/schema.sql`。

## 3. 破坏性 SQL

以下 SQL 默认视为高风险：

- `drop table`
- `alter table ... drop column`
- `truncate table`
- `delete from`

确需执行时，迁移脚本中必须包含审批标记：

```sql
-- ONES-MIGRATION-APPROVED-DESTRUCTIVE: 说明审批结论、影响范围和回滚方案
```

没有该标记的破坏性 SQL 会被测试阻断。

## 4. 交付要求

涉及数据库变更的提交必须说明：

- 影响表和字段。
- 历史数据处理方式。
- 是否可回滚。
- 回滚 SQL 或人工处理步骤。
- 是否需要停机或维护窗口。

## 5. 验证

后端测试会检查：

- `flyway_schema_history` 存在迁移记录。
- 迁移文件命名符合规范。
- 迁移版本连续且不重复。
- 不存在旧的 `schema.sql`。
- 破坏性 SQL 必须带审批标记。
