# ONES-ADMIN

ONES-ADMIN 是一个企业级后台管理系统起步工程，首版采用 **Vue3 + Spring Boot + Sa-Token** 完成前后端登录闭环。

## 当前能力

- 后端：Spring Boot 3.5.9、Sa-Token 1.45.0、统一响应、统一异常、登录认证、角色权限、动态菜单、Actuator、Swagger UI。
- 前端：Vue3、Vite、TypeScript、Element Plus、Pinia、Vue Router、Axios 请求拦截、登录页、管理布局、仪表盘。
- 认证：`Authorization: Bearer <token>`，由 Sa-Token 签发与校验。
- 演示账号：`admin / admin123`

## 项目结构

```text
ONES-ADMIN
├── server/                         # Spring Boot + Sa-Token 后端
├── web/                            # Vue3 + Vite 前端
├── docs/architecture/              # 架构与选型文档
├── mvnw / mvnw.cmd / .mvn/         # Maven Wrapper
└── README.md
```

## 本地运行

### 环境要求

- JDK 21
- Node.js 20+
- pnpm 10+

### 启动后端

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
| `GET` | `/api/health` | 应用健康检查 |

## 架构文档

- [技术选型分析](docs/architecture/technology-selection.md)
- [认证架构设计](docs/architecture/authentication.md)

## 后续路线

1. 将当前内存用户仓储替换为 MyBatis-Plus + 数据库表。
2. 将 Sa-Token 会话存储切换到 Redis，支持分布式部署。
3. 增加用户、角色、菜单、部门、岗位、字典、操作日志等企业后台基础模块。
4. 增加飞书、企业微信、钉钉第三方登录适配层。
5. 增加多租户、数据权限、代码生成、工作流与监控告警。
