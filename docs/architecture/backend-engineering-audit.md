# ONES-ADMIN 后端工程化审计与优化路线

更新时间：2026-06-29
当前分支：develop

## 1. 调研来源

本轮重点参考了以下开源企业后台项目，调研渠道为 GitHub CLI，访问日期为 2026-06-29。

| 项目 | 后端特征 | 可借鉴重点 |
| --- | --- | --- |
| [YunaiV/ruoyi-vue-pro](https://github.com/YunaiV/ruoyi-vue-pro) | Spring Boot、MyBatis-Plus、RBAC、多租户、工作流、第三方登录 | 权限体系、数据权限、SaaS 多租户、接口治理 |
| [dromara/RuoYi-Vue-Plus](https://github.com/dromara/RuoYi-Vue-Plus) | Spring Boot、Sa-Token、MyBatis-Plus、SpringDoc、OSS | Sa-Token 落地方式、接口文档、文件存储 |
| [jeecgboot/JeecgBoot](https://github.com/jeecgboot/JeecgBoot) | 低代码、Online 表单、权限、报表 | 低代码和企业支撑能力边界 |
| [1024-lab/smart-admin](https://github.com/1024-lab/smart-admin) | Spring Boot 3、Sa-Token、MyBatis-Plus、三级等保、安全体系 | 错误码体系、接口文档、登录安全、操作日志、数据字典、数据变更记录 |
| [cool-team-official/cool-admin-java](https://github.com/cool-team-official/cool-admin-java) | Spring Boot 3、MyBatis-Flex、模块化、插件化、自动初始化 | 模块化、插件化、统一 R/PageResult、文件配置、请求日志、代码生成 |

结论：ONES-ADMIN 当前不宜盲目切换 ORM 或引入插件化/低代码大框架，短期应优先补齐接口治理、安全边界、审计日志、文件治理、数据字典等企业后台底座。

## 2. 当前后端基线

已具备：

- Spring Boot 3.5.x + Java 21。
- Sa-Token 登录认证，`/api/**` 默认登录保护。
- MyBatis-Plus 数据访问。
- 用户、角色、菜单、部门基础 RBAC。
- 登录失败次数、临时锁定、最后登录时间，失败次数与锁定时长可配置。
- 登录日志、系统写操作日志已具备后端记录和查询接口。
- MySQL、Redis、RabbitMQ 配置通过环境变量注入，本地敏感配置不提交 Git。

主要缺口：

- 统一错误码刚起步，需要继续扩展错误码目录。
- 列表接口暂未形成统一分页模型。
- 审计日志已具备基础能力，但前端页面、筛选分页、保留策略仍待补齐。
- 数据字典、系统参数、文件元数据、数据权限、多租户仍待规划。
- 数据库迁移仍以 `schema.sql` 和启动补偿为主，中长期建议迁移到 Flyway/Liquibase。

## 3. 本轮已落地优化

- 新增统一错误码接口 `ErrorCode` 与基础枚举 `CommonErrorCode`。
- 保持现有 `ApiResult<T>` 的 `code/message/data` 响应结构，新增基于错误码的失败响应方法。
- 优化全局异常处理：
  - Sa-Token 未登录、无权限统一错误码。
  - 参数校验返回具体字段信息。
  - JSON 格式错误、上传大小超限有明确响应。
- 新增 `X-Trace-Id` 请求追踪过滤器，并写入日志 MDC。
- 新增 OpenAPI 配置：
  - 文档标题、版本、Bearer 认证方案。
  - 认证接口、系统管理接口分组。
  - 自动把 `@SaCheckPermission` 权限标识写入接口描述。
- 主要 Controller 补充 `@Tag`、`@Operation`，接口管理更清晰。
- 文件上传改为配置化治理：
  - 上传目录、公开访问前缀、最大文件大小、允许后缀均可配置。
  - 阻止空文件、超限文件和不允许的扩展名。
  - 下载时补充响应 Content-Type。
- 新增登录日志：
  - 记录用户名、用户 ID、成功/失败、失败原因、IP、User-Agent、TraceId、时间。
  - 提供 `/api/system/audit/login-logs` 查询接口。
- 新增系统操作日志：
  - 自动记录 `/api/system/**` 下 POST、PUT、PATCH、DELETE 写操作。
  - 记录用户 ID、请求方法、路径、模块、动作、权限点、业务响应码、结果、TraceId、耗时。
  - 提供 `/api/system/audit/operation-logs` 查询接口。
- 补充后端测试，覆盖登录、用户、文件上传、参数校验、TraceId、OpenAPI、登录日志、操作日志。

## 4. 后续优化路线

### 第一优先级：接口治理与安全

- 扩展错误码目录：认证、权限、用户、角色、菜单、文件、系统等模块单独枚举。
- 增加统一分页模型：`PageQuery`、`PageResult<T>`，并规范分页接口命名。
- 为审计日志查询增加分页、筛选、导出、保留周期和清理策略。
- 增加重复提交保护：参考 Smart Admin 的 `RepeatSubmit` 思路，优先用于写接口。

### 第二优先级：企业支撑能力

- 系统参数管理：把可运营配置从配置文件逐步沉淀到数据库。
- 数据字典：统一维护状态、枚举、选项。
- 文件表：记录文件名、大小、类型、存储路径、上传人、上传时间。
- 数据变更记录：关键表变更前后差异可追踪。
- 数据权限：按部门、本人、本部门及子部门等范围控制数据可见性。

### 第三优先级：架构演进

- 按 `auth`、`system`、`support`、`common` 梳理模块边界。
- 引入 Flyway 或 Liquibase 管理数据库迁移。
- 预留 SSO 模块：飞书扫码、飞书 SSO、OAuth2/OIDC 等统一接入。
- 多租户能力仅在业务确认需要后再引入，避免过早复杂化。
- 代码生成、插件化、低代码作为长期增强项，不作为当前主线底座。

## 5. Jenkins 与分支建议

- `develop`：持续集成、测试环境部署、日常开发合入。
- `main`：稳定版本、正式环境部署来源。
- Jenkins 先接 `develop` 做后端测试与构建：
  - `cd server && ./mvnw test`
  - 前端后续可接 `pnpm build`
- 合入 `main` 前必须确保后端测试通过、前端构建通过、数据库迁移说明完整。

更多分支说明见 [BRANCHING_AND_JENKINS.md](../BRANCHING_AND_JENKINS.md)。
