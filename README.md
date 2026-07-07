# ONES-ADMIN

ONES-ADMIN 是一个企业级后台管理系统起步工程，当前采用 **Vue3 + Spring Boot + Sa-Token + MyBatis-Plus** 完成前后端登录、权限菜单和用户管理闭环。

当前产品版本：`v0.0.128`

## 当前能力

- 后端：Spring Boot 3.5.9、Java 21、Maven Enforcer、Sa-Token 1.45.0、MyBatis-Plus 3.5.16、Flyway、MySQL、Redis、RabbitMQ、MinIO、统一响应、统一异常、TraceId 链路追踪、登录认证、角色权限、角色数据范围、动态菜单、用户 CRUD、系统数据字典类型/字典项管理、系统文件元数据、文件上传与 MinIO/本地存储切换、文件软删除与业务引用保护、文件资产分页查询、文件下载业务归属授权、文件下载操作审计、操作日志模块/操作/权限码/异常筛选与导出、上传人临时文件访问、HRMS 岗位/职级/员工基础接口、HRMS 员工基础信息编辑、HRMS 员工调岗/转正/离职生命周期、HRMS 员工生命周期时间线查询、HRMS 员工任职记录/岗位历史查询、HRMS 员工组织关系上下文查询、HRMS 员工资料附件绑定/解绑/查询、HRMS 员工资料类型与有效期治理、HRMS 员工资料到期预警与 CSV 导出、HRMS 员工合同列表/新增/编辑/终止/到期查询与 CSV 导出接口、HRMS 合同附件引用校验、HRMS 花名册模板下载/批量导入/批次与错误行查询接口、HRMS 员工花名册 CSV 导出、HRMS 人力概览统计接口、HRMS 员工主数据读接口数据范围强约束、HRMS 员工敏感字段脱敏、系统/认证/时区/HRMS 写操作审计、接口资源治理、接口治理聚合报告、接口治理评分维度、接口治理发布就绪摘要、接口治理负责人待办摘要、接口管理参考基准、接口治理推荐动作、接口治理动作项、HRMS 一期企业级设计、接口版本元数据治理、接口生命周期治理、接口受众治理、高风险写接口策略治理、写接口操作审计覆盖治理、公开接口访问策略治理、公开接口开放原因治理、接口治理修复建议、Actuator、Swagger UI。
- 前端：Vue3、Vite、TypeScript、Vben / Ant Design Vue 组件体系、Pinia、Vue Router、Axios 请求拦截、错误提示 TraceId 展示、Vben 风格企业级安全登录页、ONES/1S 核心视觉与白天/夜间品牌资源、飞书优先的紧凑企业登录入口、管理布局、工作台、用户管理页、接口管理页、审计日志页、操作日志模块/操作/权限码/异常操作筛选、系统数据字典页、系统文件管理页、受控文件 Blob 预览、根级与 Playground 包级 Vitest 单元测试入口、HRMS 员工/合同/花名册/概览字典选项接入、HRMS 字典兜底项中英文语言资源、HRMS API 响应与表单提交类型治理、HRMS 员工/合同/生命周期加载失败中英文错误提示、HRMS 前端错误归一化共享工具、HRMS 前端文件展示与下载共享工具、HRMS CSV 导出下载入口统一、HRMS Blob 下载反馈共享工具、HRMS 上传反馈共享工具、HRMS Upload 文件解析共享工具、HRMS Upload 共享工具 Vitest 单元测试、HRMS 状态切换执行反馈共享工具、HRMS 上传与附件预览错误提示入口治理、HRMS 受控附件打开入口、提示模式与加载反馈统一、合同附件上传/查看/移除闭环、合同到期预警指标、风险状态、窗口筛选与导出入口、合同管理中英文文案资源、HRMS 预警窗口/风险状态/到期指标前端共用工具、员工资料附件上传/查看/移除分栏、HRMS 员工资料附件删除反馈闭环、员工资料类型与到期状态展示、员工资料到期概览指标、员工资料到期预警页与导出入口、接口调用方筛选与展示、Manifest 基线快照、发布门禁、发布就绪、负责人待办和治理规则视图、HRMS 人力概览页、HRMS 人力概览中英文文案资源、HRMS 员工档案抽屉、HRMS 员工档案抽屉中英文文案资源、HRMS 员工列表与生命周期操作中英文文案资源、HRMS 任职记录分栏、HRMS 组织关系分栏、HRMS 员工列表导出入口、HRMS 花名册导入页、HRMS 花名册导入中英文文案资源、HRMS 岗位/职级状态切换中英文文案资源、失败反馈与共享确认工具、Playwright 登录冒烟用例。
- 认证：`Authorization: Bearer <token>`，由 Sa-Token 签发与校验。
- 演示账号：`admin / admin123`

## 项目结构

```text
ONES-ADMIN
├── server/                         # Spring Boot + Sa-Token 后端
├── web/                            # Vue3 + Vite 前端
├── docs/architecture/              # 架构与选型文档
├── docs/CHANGELOG.md               # 产品版本记录
├── VERSION                         # 当前产品版本号
├── server/mvnw / server/mvnw.cmd   # Maven Wrapper
└── README.md
```

## 本地运行

### 环境要求

- JDK 21
- Node.js 22.18+ 或 24+
- pnpm 11+
- MySQL、Redis，可选 RabbitMQ、MinIO

### 启动后端

后端默认从环境变量读取 MySQL、Redis、RabbitMQ、MinIO 配置，并会额外加载本地忽略文件：

```text
server/config/application-local.yml
```

本地中间件连接信息记录在 `docs/local/middleware-credentials.md`，该目录已加入 `.gitignore`，不会提交到 Git。

默认 `ones.security.session.storage=redis`、`ones.security.repeat-submit.storage=redis`，正式环境依赖 Redis 保存登录态和防重票据；本地 E2E 或临时 H2 验证可显式设置 `ONES_SECURITY_SESSION_STORAGE=memory`、`ONES_REPEAT_SUBMIT_STORAGE=memory`，避免为了纯页面验证强依赖 Redis。默认 `ones.events.broker=none`、`ones.file.storage-type=local`，本地和测试环境不会强依赖 RabbitMQ 或 MinIO。需要启用开发环境中间件时，在 `server/config/application-local.yml` 或环境变量中改为 `ONES_EVENTS_BROKER=rabbitmq`、`ONES_FILE_STORAGE_TYPE=minio`，并补齐 RabbitMQ、MinIO 等连接信息；RabbitMQ 用户名和密码默认不提供兜底值，避免默认账号进入提交。MinIO 客户端默认对内网对象存储直连，只有明确需要走代理时才设置 `ONES_MINIO_PROXY_ENABLED=true`。

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
cd server
./mvnw spring-boot:run
```

后端默认端口：`8080`

常用地址：

- 健康检查：`http://localhost:8080/api/health`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- Actuator：`http://localhost:8080/actuator/health`

### 启动前端

```bash
cd web
pnpm install
pnpm dev
```

前端默认端口：`5173`

访问：`http://localhost:5173`

### 飞书与企业 SSO 登录配置

登录页按飞书优先展示企业登录入口。前端只负责发起授权跳转，飞书回调、授权码换取用户信息、账号绑定和 Sa-Token 签发必须由后端完成，避免 App Secret 暴露到浏览器。

可在 `web/playground/.env.development` 或生产注入配置中设置：

```text
VITE_GLOB_AUTH_FEISHU_APP_ID=
VITE_GLOB_AUTH_FEISHU_REDIRECT_URI=
VITE_GLOB_AUTH_FEISHU_AUTH_URL=
VITE_GLOB_AUTH_SSO_NAME=
VITE_GLOB_AUTH_SSO_URL=
```

优先级：如果配置了 `VITE_GLOB_AUTH_FEISHU_AUTH_URL`，前端直接使用该授权地址；否则在同时配置 `VITE_GLOB_AUTH_FEISHU_APP_ID` 和 `VITE_GLOB_AUTH_FEISHU_REDIRECT_URI` 时，按飞书开放平台 OAuth 授权入口生成跳转地址。企业 SSO 只有配置 `VITE_GLOB_AUTH_SSO_URL` 后才展示入口。

## 验证命令

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
cd server
./mvnw test

cd web
pnpm test:unit
pnpm -F @vben/playground run test:unit
pnpm build
```

## 首版接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | 登录并返回 Sa-Token |
| `GET` | `/api/auth/me` | 获取当前用户 |
| `POST` | `/api/auth/logout` | 退出登录 |
| `GET` | `/api/system/menus` | 获取当前用户菜单 |
| `GET` | `/api/system/users` | 查询用户列表 |
| `POST` | `/api/system/users` | 新增用户 |
| `PUT` | `/api/system/users/{id}` | 编辑用户 |
| `DELETE` | `/api/system/users/{id}` | 删除用户 |
| `GET` | `/api/system/roles` | 查询角色列表 |
| `GET` | `/api/system/dicts/types` | 查询系统字典类型 |
| `POST` | `/api/system/dicts/types` | 新增系统字典类型 |
| `PUT` | `/api/system/dicts/types/{id}` | 编辑系统字典类型 |
| `DELETE` | `/api/system/dicts/types/{id}` | 删除无字典项的系统字典类型 |
| `GET` | `/api/system/dicts/items` | 查询系统字典项 |
| `POST` | `/api/system/dicts/items` | 新增系统字典项 |
| `PUT` | `/api/system/dicts/items/{id}` | 编辑系统字典项 |
| `DELETE` | `/api/system/dicts/items/{id}` | 删除系统字典项 |
| `GET` | `/api/system/dicts/{dictCode}/options` | 查询启用字典选项，供业务表单复用 |
| `GET` | `/api/system/api-resources/governance/rules` | 查询接口治理规则目录 |
| `GET` | `/api/system/api-resources/governance/report` | 生成接口治理聚合报告，包含发布就绪、参考基准、推荐动作与可跟踪动作项，供 Jenkins 和接口管理页消费 |
| `GET` | `/api/system/api-resources/export` | 导出接口资源 CSV 清单 |
| `GET` | `/api/system/api-resources/manifest` | 生成接口资源 Manifest |
| `GET` | `/api/system/api-resources/manifest/snapshots` | 查询接口资源 Manifest 发布快照 |
| `GET` | `/api/system/api-resources/manifest/snapshots/latest` | 查询最新接口资源 Manifest 发布快照 |
| `POST` | `/api/system/api-resources/manifest/snapshots` | 发布接口资源 Manifest 快照 |
| `POST` | `/api/system/api-resources/manifest/diff` | 对比接口资源 Manifest |
| `POST` | `/api/system/api-resources/manifest/gate` | 校验接口资源 Manifest 发布门禁，返回机器可读检查项 |
| `POST` | `/api/system/api-resources/manifest/gate/latest` | 基于最新发布快照校验接口资源 Manifest 发布门禁，返回机器可读检查项 |
| `GET` | `/api/hr/positions` | 查询 HRMS 岗位列表 |
| `POST` | `/api/hr/positions` | 新增 HRMS 岗位 |
| `PUT` | `/api/hr/positions/{id}` | 编辑 HRMS 岗位 |
| `GET` | `/api/hr/job-grades` | 查询 HRMS 职级列表 |
| `POST` | `/api/hr/job-grades` | 新增 HRMS 职级 |
| `PUT` | `/api/hr/job-grades/{id}` | 编辑 HRMS 职级 |
| `GET` | `/api/hr/employees` | 查询 HRMS 员工列表 |
| `GET` | `/api/hr/employees/{id}` | 查询 HRMS 员工详情 |
| `POST` | `/api/hr/employees` | 新增 HRMS 员工，并写入任职记录和生命周期事件 |
| `PUT` | `/api/hr/employees/{id}` | 编辑 HRMS 员工基础信息，不绕过调岗/转正/离职流程 |
| `GET` | `/api/hr/employees/{id}/documents` | 查询 HRMS 员工资料附件 |
| `GET` | `/api/hr/employees/documents/expiring` | 查询 HRMS 即将到期员工资料 |
| `POST` | `/api/hr/employees/{id}/documents/{fileId}` | 绑定 HRMS 员工资料附件 |
| `DELETE` | `/api/hr/employees/{id}/documents/{fileId}` | 移除 HRMS 员工资料附件并软删除文件元数据 |
| `GET` | `/api/hr/employees/{id}/lifecycle-events` | 查询 HRMS 员工生命周期时间线 |
| `GET` | `/api/hr/employees/{employeeId}/contracts` | 查询 HRMS 员工合同列表 |
| `GET` | `/api/hr/contracts/expiring` | 查询 HRMS 即将到期员工合同 |
| `GET` | `/api/hr/contracts/expiring/export` | 导出 HRMS 即将到期员工合同 CSV |
| `POST` | `/api/hr/employees/{employeeId}/contracts` | 新增 HRMS 员工合同 |
| `PUT` | `/api/hr/employees/{employeeId}/contracts/{contractId}` | 编辑 HRMS 员工合同 |
| `POST` | `/api/hr/contracts/{contractId}/terminate` | 终止 HRMS 员工合同 |
| `GET` | `/api/hr/roster-import/template` | 下载 HRMS 花名册导入 CSV 模板 |
| `POST` | `/api/hr/roster-import/batches` | 上传并导入 HRMS 花名册，成功行进入员工主数据链路 |
| `GET` | `/api/hr/roster-import/batches` | 查询 HRMS 花名册导入批次 |
| `GET` | `/api/hr/roster-import/batches/{id}` | 查询 HRMS 花名册导入批次详情 |
| `GET` | `/api/hr/roster-import/batches/{id}/errors` | 查询 HRMS 花名册导入错误行 |
| `POST` | `/api/hr/employees/{id}/transfer` | HRMS 员工调岗，并写入任职记录和生命周期事件 |
| `POST` | `/api/hr/employees/{id}/regularize` | HRMS 员工转正，并写入生命周期事件 |
| `POST` | `/api/hr/employees/{id}/resign` | HRMS 员工离职，并写入生命周期事件 |
| `GET` | `/api/health` | 应用健康检查 |

## 架构文档

- [技术选型分析](docs/architecture/technology-selection.md)
- [认证架构设计](docs/architecture/authentication.md)
- [数据库迁移治理规范](docs/architecture/database-migration-governance.md)
- [HRMS 一期企业级设计方案](docs/architecture/hrms-phase-one-design.md)

## 后续路线

1. 持续补充 Flyway 增量迁移脚本和数据库变更回滚说明。
2. 为 Sa-Token Redis 会话补充独立库、Key 前缀、过期监控和多实例压测，完善分布式登录态运维能力。
3. 完成系统参数等企业后台基础模块，继续推进文件管理页、文件访问策略、数据字典接入更多 HRMS 和系统表单，并将数据权限扩展到更多业务域、自定义部门集合和字段脱敏策略。
4. 完成飞书 OAuth 回调、用户绑定和企业 SSO 后端适配层，再扩展企业微信、钉钉等其他第三方入口。
5. 增加多租户、代码生成、工作流与监控告警，持续增强角色数据范围、字段级权限和跨租户隔离能力。
