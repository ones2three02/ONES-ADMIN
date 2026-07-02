# ONES-ADMIN

ONES-ADMIN 是一个企业级后台管理系统起步工程，当前采用 **Vue3 + Spring Boot + Sa-Token + MyBatis-Plus** 完成前后端登录、权限菜单和用户管理闭环。

当前产品版本：`v0.0.51`

## 当前能力

- 后端：Spring Boot 3.5.9、Java 21、Maven Enforcer、Sa-Token 1.45.0、MyBatis-Plus 3.5.16、Flyway、MySQL、Redis、RabbitMQ、MinIO、统一响应、统一异常、TraceId 链路追踪、登录认证、角色权限、动态菜单、用户 CRUD、HRMS 岗位/职级/员工基础接口、接口资源治理、接口治理聚合报告、接口管理参考基准、接口治理推荐动作、HRMS 一期企业级设计、接口版本元数据治理、接口生命周期治理、接口受众治理、高风险写接口策略治理、公开接口访问策略治理、接口治理修复建议、Actuator、Swagger UI。
- 前端：Vue3、Vite、TypeScript、Element Plus、Pinia、Vue Router、Axios 请求拦截、错误提示 TraceId 展示、Vben 风格企业级安全登录页、ONES 1S 品牌视觉面板、管理布局、工作台、用户管理页、接口管理页、接口调用方筛选与展示、发布门禁与治理规则视图、Playwright 登录冒烟用例。
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

默认 `ones.events.broker=none`、`ones.file.storage-type=local`，本地和测试环境不会强依赖 RabbitMQ 或 MinIO。需要启用开发环境中间件时，在 `server/config/application-local.yml` 或环境变量中改为 `ONES_EVENTS_BROKER=rabbitmq`、`ONES_FILE_STORAGE_TYPE=minio`，并补齐对应连接信息。MinIO 客户端默认对内网对象存储直连，只有明确需要走代理时才设置 `ONES_MINIO_PROXY_ENABLED=true`。

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

## 验证命令

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
cd server
./mvnw test

cd web
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
| `GET` | `/api/system/api-resources/governance/rules` | 查询接口治理规则目录 |
| `GET` | `/api/system/api-resources/governance/report` | 生成接口治理聚合报告，包含参考基准与推荐动作，供 Jenkins 和接口管理页消费 |
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
| `GET` | `/api/health` | 应用健康检查 |

## 架构文档

- [技术选型分析](docs/architecture/technology-selection.md)
- [认证架构设计](docs/architecture/authentication.md)
- [数据库迁移治理规范](docs/architecture/database-migration-governance.md)
- [HRMS 一期企业级设计方案](docs/architecture/hrms-phase-one-design.md)

## 后续路线

1. 持续补充 Flyway 增量迁移脚本和数据库变更回滚说明。
2. 将 Sa-Token 会话存储切换到 Redis，支持分布式部署。
3. 完成角色、菜单、部门、岗位、字典、操作日志等企业后台基础模块。
4. 增加飞书、企业微信、钉钉第三方登录适配层。
5. 增加多租户、数据权限、代码生成、工作流与监控告警。
