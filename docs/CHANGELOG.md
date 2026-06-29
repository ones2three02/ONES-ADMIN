# ONES-ADMIN 版本记录

## v0.0.13 - 2026-06-29

- 新增接口资源 Manifest 接口 `/api/system/api-resources/manifest`，输出稳定 JSON 接口清单。
- Manifest 返回应用版本、资源总数、`SHA-256` 指纹和关键接口元数据，便于 Jenkins 做接口契约留档与版本差异比对。
- Manifest 资源项覆盖 `apiKey`、请求方法、路径、处理器、认证级别、权限点、权限模式、写操作和废弃状态。
- Manifest 沿用 `system:api:list` 权限控制，普通运营用户无法越权读取接口契约清单。
- README 首版接口表新增接口资源 Manifest 入口。

## v0.0.12 - 2026-06-29

- 新增接口资源 CSV 导出接口 `/api/system/api-resources/export`，用于审计、Jenkins 附件和权限巡检留档。
- 导出字段覆盖 `apiKey`、请求方法、路径、处理器、模块、摘要、认证级别、权限点、权限状态、写操作、废弃状态和访问策略。
- CSV 导出沿用 `system:api:list` 权限控制，普通运营用户无法越权下载接口清单。
- 导出内容补充 CSV 公式注入防护，降低导出文件在表格软件中打开时的安全风险。
- README 首版接口表新增接口资源导出入口。

## v0.0.11 - 2026-06-29

- 增强接口资源清单，新增稳定接口标识 `apiKey`、处理器定位 `handler` 和接口废弃状态 `deprecated`。
- 接口资源查询新增按 `handler`、`deprecated` 筛选，便于接口管理页和 Jenkins 巡检定位 Controller 方法。
- 接口资源汇总新增 `deprecatedCount`，接口治理门禁对废弃接口预留 `DEPRECATED_API` 警告规则。
- 后端工程化审计补充 Smart Admin、Cool Admin Java、RuoYi/Yudao 的接口管理借鉴点。
- 修正 README 前端运行环境要求为 Node.js 22.18+ 或 24+、pnpm 11+。

## v0.0.10 - 2026-06-29

- 新增数据库迁移治理规范文档 `docs/architecture/database-migration-governance.md`。
- 增强 Flyway 迁移测试，校验迁移文件命名、版本连续性和旧 `schema.sql` 禁用规则。
- 新增破坏性 SQL 门禁，`drop table`、`truncate table`、`delete from`、`alter table ... drop column` 必须包含审批标记。
- README 增加数据库迁移治理文档入口。

## v0.0.9 - 2026-06-29

- 引入 Flyway 数据库迁移治理，新增 `db/migration/V1__init_schema.sql` 基线迁移脚本。
- 关闭 Spring SQL init，移除旧的 `schema.sql` 和启动 DDL 补偿类 `DatabaseSchemaMigrator`。
- 对已有非空数据库启用 `baseline-on-migrate`，避免接入 Flyway 时重复执行基线脚本。
- 新增数据库迁移治理测试，验证测试环境存在 Flyway 迁移历史表。

## v0.0.8 - 2026-06-29

- 新增认证模块错误码 `AuthErrorCode` 和系统模块错误码 `SystemErrorCode`。
- `CommonErrorCode` 收敛为通用协议与框架级错误码，文件、认证、用户、角色、菜单、部门错误改为模块枚举承载。
- 替换核心业务服务中的裸字符串异常和裸 `404` 异常，统一使用模块错误码。
- 新增错误码治理测试，校验跨模块错误码不重复，并约束认证、系统模块号段。

## v0.0.7 - 2026-06-29

- 新增接口权限码命名规范治理，接口资源返回权限码是否符合标准。
- 接口治理质量门禁新增 `PERMISSION_CODE_INVALID_FORMAT` 错误规则。
- 门禁接口返回当前权限码正则，便于 Jenkins 和后续前端接口管理页展示统一规范。
- 补充接口资源测试，确保权限码规范可被 CI 自动验证。

## v0.0.6 - 2026-06-29

- 新增接口权限注册一致性治理，接口资源返回权限码是否已注册、是否可在角色授权树分配。
- 接口治理质量门禁新增未注册权限码错误、不可授权权限码警告。
- 补齐审计日志、接口资源、文件上传等权限的菜单 button 节点，不新增前端路由页面。
- 补充接口资源测试，验证权限码注册和可授权状态。

## v0.0.5 - 2026-06-29

- 新增接口治理质量门禁接口 `/api/system/api-resources/governance`。
- 门禁接口返回是否通过、接口总数、错误数、警告数和违规明细，便于 Jenkins 后续接入自动巡检。
- 新增系统接口权限缺口、系统写接口无权限、缺少模块标签、缺少接口摘要等治理规则。
- 补充接口治理门禁权限测试，确保仅具备 `system:api:list` 权限的用户可以访问。

## v0.0.4 - 2026-06-29

- 新增接口资源治理汇总接口 `/api/system/api-resources/summary`。
- 汇总接口返回接口总数、写操作数量、权限缺口数量、显式访问策略数量。
- 汇总接口支持按认证级别和模块统计接口分布，便于后续接口管理页和巡检脚本消费。

## v0.0.3 - 2026-06-29

- 新增接口访问策略注解 `@ApiAccessPolicy`，可显式标记登录态接口的设计意图。
- 接口资源清单新增访问策略显式标识和策略说明，降低权限巡检误报。
- 文件上传新增权限点 `system:file:upload`，超级管理员启动时自动补齐授权。
- 当前用户菜单、动态路由、文件访问接口补充登录态访问策略说明。
- 补充文件上传权限测试，验证普通运营账号不能越权上传文件。

## v0.0.2 - 2026-06-29

- 增强接口资源管理能力。
- 接口资源查询支持分页、方法、路径、模块、权限点、认证级别、写操作和权限缺口筛选。
- 新增接口认证级别：`PUBLIC`、`LOGIN`、`PERMISSION`。
- 新增系统接口权限缺口标识，便于后续前端接口管理页和安全巡检使用。

## v0.0.1 - 2026-06-29

- 初始化企业级后台管理系统基座。
- 前端采用 Vue3 + Vben Admin 风格，保留 Vben 代码与视觉体系。
- 后端采用 Spring Boot 3、Sa-Token、MyBatis-Plus。
- 已完成登录认证、RBAC、动态菜单、用户/角色/部门基础管理。
- 已完成登录失败锁定、TraceId、OpenAPI、审计日志、防重复提交、统一分页与接口资源清单。
- 已补充分支模型、Jenkins 建议、后端工程化审计与认证架构文档。
