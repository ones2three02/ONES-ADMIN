# ONES-ADMIN 后端工程化审计与优化路线

更新时间：2026-06-29
当前分支：develop

## 1. 调研来源

本轮重点参考了以下开源企业后台项目，调研渠道为 agent-reach GitHub/dev 路由与 GitHub CLI，访问日期为 2026-06-29。

| 项目 | 后端特征 | 可借鉴重点 |
| --- | --- | --- |
| [YunaiV/ruoyi-vue-pro](https://github.com/YunaiV/ruoyi-vue-pro) | Spring Boot、MyBatis-Plus、RBAC、多租户、工作流、第三方登录 | 权限体系、数据权限、SaaS 多租户、接口治理 |
| [dromara/RuoYi-Vue-Plus](https://github.com/dromara/RuoYi-Vue-Plus) | Spring Boot、Sa-Token、MyBatis-Plus、SpringDoc、OSS | Sa-Token 落地方式、接口文档、文件存储 |
| [jeecgboot/JeecgBoot](https://github.com/jeecgboot/JeecgBoot) | 低代码、Online 表单、权限、报表 | 低代码和企业支撑能力边界 |
| [1024-lab/smart-admin](https://github.com/1024-lab/smart-admin) | Spring Boot 3、Sa-Token、MyBatis-Plus、三级等保、安全体系；Java 17 版本采用 `sa-base`、`sa-admin` 多模块 | 错误码体系、接口文档、登录安全、操作日志、数据字典、数据变更记录、基础能力模块化 |
| [cool-team-official/cool-admin-java](https://github.com/cool-team-official/cool-admin-java) | Spring Boot 3、MyBatis-Flex、`core` + `modules`、插件化、自动初始化 | 核心能力和业务模块分离、插件扩展思想、统一返回/分页、文件配置、请求日志、代码生成 |

结论：ONES-ADMIN 当前不宜盲目切换 ORM 或引入插件化/低代码大框架，短期应优先补齐接口治理、安全边界、审计日志、文件治理、数据字典等企业后台底座。

补充取舍：

- Smart Admin 的价值在于“安全与规范”：登录安全、数据脱敏、数据变更记录、统一错误码、操作日志、系统参数、字典等能力适合作为 ONES-ADMIN 的企业级底座路线。
- Cool Admin Java 的价值在于“模块化与扩展”：`core` 放通用底座，`modules` 放业务模块的划分方式值得借鉴；插件化、AI 代码生成、多租户暂不作为当前主线，避免底座阶段复杂度过高。
- ONES-ADMIN 当前保持 Spring Boot 3 + Sa-Token + MyBatis-Plus，不跟随 Cool Admin Java 切换 MyBatis-Flex；后续只吸收它的模块边界和初始化治理思想。

## 2. 当前后端基线

已具备：

- Spring Boot 3.5.x + Java 21。
- Sa-Token 登录认证，`/api/**` 默认登录保护。
- MyBatis-Plus 数据访问。
- 用户、角色、菜单、部门基础 RBAC。
- 登录失败次数、临时锁定、最后登录时间，失败次数与锁定时长可配置。
- 登录日志、系统写操作日志已具备后端记录和查询接口。
- 已建立统一分页模型，审计日志查询支持分页与筛选。
- 已建立运行时接口资源清单，可扫描后端真实 `/api/**` 路由、方法、模块、摘要、权限点和写操作标识。
- 已建立接口治理质量门禁，可输出权限缺口、系统写接口无权限、接口文档元数据缺失等违规项。
- 已建立接口权限注册一致性检查，可识别代码权限点是否写入 `sys_permission`，以及是否挂载到 `sys_menu.auth_code` 供角色授权。
- 已建立权限码命名规范检查，当前统一使用小写冒号分段格式：`^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*){1,3}$`。
- 高风险写接口已接入防重复提交保护，正式环境默认使用 Redis 存储防重票据。
- MySQL、Redis、RabbitMQ 配置通过环境变量注入，本地敏感配置不提交 Git。

主要缺口：

- 统一错误码刚起步，需要继续扩展错误码目录。
- 审计日志已具备基础能力，但前端页面、导出、保留策略仍待补齐。
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
- 新增接口资源管理后端能力：
  - 提供 `/api/system/api-resources` 接口，按运行时 Spring MVC 路由生成 API 资源清单。
  - 返回请求方法、路径、所属模块、接口摘要、权限点、权限码是否规范、权限模式、认证级别、是否需要权限、权限点是否注册、权限点是否可授权、是否写操作。
  - 支持分页和按方法、路径、模块、权限点、认证级别、写操作、权限缺口筛选。
  - 提供 `/api/system/api-resources/summary` 治理汇总接口，返回接口总数、写操作数、权限缺口数、访问策略数、认证级别分布和模块分布。
  - 提供 `/api/system/api-resources/governance` 接口治理质量门禁，返回是否通过、错误数、警告数、权限码正则和违规明细。
  - 自动区分 `PUBLIC`、`LOGIN`、`PERMISSION` 三类接口认证级别。
  - 提供 `@ApiAccessPolicy` 显式标记登录态接口的设计意图，减少安全巡检误报。
  - 对仅登录保护、且没有显式访问策略的系统接口标记权限缺口，便于安全巡检。
  - 对系统写接口无权限点、权限码命名不规范、权限点未注册、权限点不可授权、缺少 OpenAPI 模块标签、缺少接口摘要等问题输出结构化治理结果，便于 Jenkins 自动巡检。
  - 新增权限点 `system:api:list`，超级管理员启动时自动补齐授权。
  - 新增文件上传权限点 `system:file:upload`，文件上传不再只是登录态即可访问。
  - 审计日志、接口资源、文件上传等暂未落前端页面的权限，以菜单 button 节点挂入授权树，避免出现无页面组件的空路由。
  - 暂不新增前端菜单，避免出现无页面组件的空路由；后续按 Vben 风格补接口管理页后再挂菜单。
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
- 新增防重复提交：
  - 提供 `@RepeatSubmit` 注解，拦截短时间内同一用户、同一方法、同一参数的重复请求。
  - 用户、角色、菜单、部门、文件上传等写接口已接入。
  - 正式环境默认使用 Redis，测试环境使用内存票据。
- 新增统一分页模型：
  - 提供 `PageQuery`、`PageResult<T>`，统一 `pageNum`、`pageSize`、`total`、`pages`、`list`、`empty` 字段。
  - 配置 MyBatis-Plus 分页插件，当前按 MySQL 方言运行。
  - `pageSize` 最大限制为 200，超过限制会返回明确参数错误。
  - 登录日志支持按用户名、成功状态、IP、时间范围分页筛选。
  - 操作日志支持按用户、方法、路径、成功状态、响应码、TraceId、时间范围分页筛选。
- 补充后端测试，覆盖登录、用户、文件上传、参数校验、TraceId、OpenAPI、登录日志、操作日志、防重复提交。

## 4. 后续优化路线

### 第一优先级：接口治理与安全

- 扩展错误码目录：认证、权限、用户、角色、菜单、文件、系统等模块单独枚举。
- 将统一分页模型逐步推广到用户、角色、菜单、部门、文件等列表接口，并规范分页接口命名。
- 为接口资源清单增加导出、权限缺口治理建议，并在前端按 Vben 选型风格落地接口管理页。
- 为审计日志查询增加前端页面、导出、保留周期和清理策略。
- 为防重复提交增加后台配置页、按接口覆盖默认间隔、Redis 不可用告警。

### 第二优先级：企业支撑能力

- 系统参数管理：把可运营配置从配置文件逐步沉淀到数据库。
- 数据字典：统一维护状态、枚举、选项。
- 文件表：记录文件名、大小、类型、存储路径、上传人、上传时间。
- 数据变更记录：关键表变更前后差异可追踪。
- 数据权限：按部门、本人、本部门及子部门等范围控制数据可见性。
- 模块边界：参考 Smart Admin 的基础模块拆分与 Cool Admin Java 的 `core/modules` 思路，把公共底座和业务模块逐步拆清。

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
  - 登录测试账号后调用 `/api/system/api-resources/governance`，要求 `passed=true` 且 `errorCount=0`
  - 权限码命名统一遵循 `/api/system/api-resources/governance` 返回的 `permissionCodePattern`
  - 前端后续可接 `pnpm build`
- 合入 `main` 前必须确保后端测试通过、前端构建通过、数据库迁移说明完整。

更多分支说明见 [BRANCHING_AND_JENKINS.md](../BRANCHING_AND_JENKINS.md)。
