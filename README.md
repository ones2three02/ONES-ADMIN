# ONES-ADMIN

ONES-ADMIN 是一个企业级后台管理系统起步工程，当前采用 **Vue3 + Spring Boot + Sa-Token + MyBatis-Plus** 完成前后端登录、权限菜单和用户管理闭环。

当前产品版本：`v0.0.148`

## 当前能力

- 后端基座：Spring Boot 3.5.9、Java 21、Maven Enforcer、Sa-Token 1.45.0、MyBatis-Plus 3.5.16、Flyway、统一响应、统一异常、TraceId、登录认证、角色权限、角色数据范围、动态菜单、用户/角色/菜单/部门、数据字典、文件资产、审计日志、防重复提交、接口资源治理、受控 Actuator / Swagger、登录账号/IP 限流和安全响应头。
- HRMS 一期：岗位、职级、员工主数据、员工基础信息编辑、调岗/转正/离职生命周期、任职记录、组织关系、员工资料附件、资料到期预警、合同管理、合同附件、花名册导入导出、人力概览、员工档案抽屉、员工读接口数据范围和敏感字段脱敏第一阶段。
- 前端基座：Vue3、Vite、TypeScript、Vben / Ant Design Vue、Pinia、Vue Router、Axios 请求拦截、错误提示 TraceId 展示、登录页 ONES/1S 品牌资源、管理布局、工作台、系统管理页、HRMS 一期页面、接口治理页、审计页、文件管理页、存量业务表格稳定高度治理、前端布局治理报告和引导式交互规范。
- 工程化基座：Jenkins 验证门禁、本地统一验证入口、版本一致性检查、仓库安全扫描、构建元数据、环境配置报告、数据库迁移报告、前端布局报告、接口治理报告、验证摘要和发布证据包。
- 认证：`Authorization: Bearer <token>`，由 Sa-Token 签发与校验。
- 测试账号：测试配置显式启用初始化时使用 `admin / admin123`；正式运行默认不创建任何演示账号。

## 当前边界

- 飞书 OAuth、企业 SSO、短信登录和二维码扫码登录尚未完成后端认证闭环；当前前端只保留入口、授权跳转配置和未配置提示。
- 系统初始化默认关闭，Swagger 默认关闭；Actuator 和启用后的 Swagger 默认要求登录，公开范围必须通过配置显式放开。
- 前端业务 VXE 表格已完成 `height: auto` 存量清理，复杂分栏页和单栏业务页均进入稳定高度治理口径；后续仍需补充更多视觉回归和业务页 E2E，防止新页面回退。
- 当前 Jenkins 只执行验证门禁，不执行生产部署；真实测试、预发、生产环境矩阵和发布回滚策略仍需单独设计。
- 当前项目是企业级 HRMS 起步工程，不应把未完成的规划能力视为生产可用能力。详细状态见 [项目状态与问题闭环审计](docs/architecture/project-status-closure-audit.md)。

## 项目结构

```text
ONES-ADMIN
├── server/                         # Spring Boot + Sa-Token 后端
├── web/                            # Vue3 + Vite 前端
├── docs/architecture/              # 架构与选型文档
├── docs/CHANGELOG.md               # 产品版本记录
├── Jenkinsfile                     # Jenkins 验证门禁流水线
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

默认 `ones.security.session.storage=redis`、`ones.security.repeat-submit.storage=redis`、`ones.security.login-rate-limit.storage=redis`，正式环境依赖 Redis 保存登录态、防重票据和登录限流状态；本地 E2E 或临时 H2 验证可显式切换为 `memory`。默认 `ones.events.broker=none`、`ones.file.storage-type=local`，本地和测试环境不会强依赖 RabbitMQ 或 MinIO。需要启用开发环境中间件时，在 ignored 本地配置或环境变量中开启对应能力并补齐连接信息；RabbitMQ、MinIO 和初始化管理员密码均不提供可提交的敏感兜底值。

空数据库首次开发初始化必须显式提供开关与密码，已有正式数据库不得再次开启：

```bash
export ONES_BOOTSTRAP_ENABLED=true
export ONES_BOOTSTRAP_ADMIN_PASSWORD='仅用于本地初始化的强密码'
```

初始化器只在 `ONES_BOOTSTRAP_ENABLED=true` 时装配；开关开启但密码为空会拒绝启动。生产环境应通过受控运维流程创建管理员，并保持该开关关闭。

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
cd server
./mvnw spring-boot:run
```

后端默认端口：`8080`

常用地址：

- 健康检查：`http://localhost:8080/api/health`
- Swagger UI：设置 `ONES_API_DOCS_ENABLED=true` 后访问 `http://localhost:8080/swagger-ui.html`，默认需要登录
- Actuator：`http://localhost:8080/actuator/health`，默认需要登录；公开监控端点需显式设置 `ONES_ACTUATOR_PUBLIC=true`

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

Jenkins 已提供根目录 `Jenkinsfile` 作为当前阶段验证门禁，默认只安装前端锁定依赖并执行测试、类型检查、构建、版本一致性检查、版本残留扫描和敏感信息扫描，不包含部署动作；本地与 Jenkins 共用 `scripts/ci/verify.sh` 阶段入口，版本检查沉淀在 `scripts/ci/version-guard.sh`，仓库扫描逻辑沉淀在 `scripts/ci/repository-guard.sh`。流水线会生成 `.ci-artifacts/build-metadata.json`，用于归档当前产品版本、Git 提交、分支和工具链版本，生成 `.ci-artifacts/environment-config-report.json` 归档环境变量清单、生产必填项、占位值、本地默认值和敏感项审计结论，生成 `.ci-artifacts/database-migration-report.json` 归档 Flyway 迁移脚本版本、命名、破坏性 SQL 审批和指纹状态，生成 `.ci-artifacts/frontend-layout-report.json` 归档后台列表页共享分栏布局、稳定表格高度和旧布局类回退扫描结论，生成 `.ci-artifacts/api-governance-report.json` 归档接口治理聚合报告，生成 `.ci-artifacts/verification-summary.json` 汇总门禁结果、环境配置治理、数据库迁移治理、前端布局治理、接口治理状态、后端测试报告和前端构建产物，并生成 `.ci-artifacts/release-evidence.json` 汇总构建来源、质量门禁和发布证据结论，便于 Jenkins 构建追溯、测试交付和质量审计。环境配置报告和前端布局报告只扫描可提交配置模板与源码，不读取 `docs/local/`、`server/config/application-local.yml` 等 ignored 敏感文件，也不会输出真实密码或 Secret。

```bash
bash scripts/ci/verify.sh all
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
- [HRMS 架构治理路线图](docs/architecture/hrms-architecture-governance-roadmap.md)
- [前端引导式交互治理规范](docs/architecture/frontend-guided-interaction-governance.md)
- [项目状态与问题闭环审计](docs/architecture/project-status-closure-audit.md)

## 后续路线

1. 持续补充 Flyway 增量迁移脚本和数据库变更回滚说明。
2. 为 Sa-Token Redis 会话补充独立库、Key 前缀、过期监控和多实例压测，完善分布式登录态运维能力。
3. 按 HRMS 架构治理路线图和前端引导式交互规范持续推进服务边界、字段级权限、数据范围、前端页面模板和部署环境矩阵。
4. 完成飞书 OAuth 回调、用户绑定和企业 SSO 后端适配层，再扩展企业微信、钉钉等其他第三方入口。
5. 增加多租户、代码生成、工作流与监控告警，持续增强角色数据范围、字段级权限和跨租户隔离能力。
