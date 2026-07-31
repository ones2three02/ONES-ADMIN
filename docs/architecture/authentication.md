# ONES-ADMIN 认证架构设计

## 设计原则

ONES-ADMIN 的认证设计遵循一个核心原则：外部平台只负责证明“你是谁”，ONES-ADMIN 自己负责判断“你能做什么”。

因此首版采用 Sa-Token 作为主权限框架，后续飞书、企业微信、钉钉、OIDC 等第三方登录都通过认证适配层接入，最终统一转换为本系统用户与权限。

## 当前认证流程

```text
用户输入账号密码
  ↓
POST /api/auth/login
  ↓
AuthService 校验用户与密码
  ↓
MyBatisUserRepository 读取用户、角色、权限
  ↓
Sa-Token 签发 Token
  ↓
前端保存 tokenValue/tokenPrefix
  ↓
后续请求携带 Authorization: Bearer <token>
  ↓
SaInterceptor 校验登录态
  ↓
SaPermissionProvider 提供角色与权限
```

## 权限模型

首版已经具备以下模型边界：

- 用户：`sys_user` / `UserProfile`
- 角色：`sys_role` / `roles`
- 权限：`sys_permission` / `permissions`
- 用户角色关系：`sys_user_role`
- 角色权限关系：`sys_role_permission`
- 菜单：`MenuItem`
- Sa-Token 权限适配：`SaPermissionProvider`

测试环境账号：

```text
用户名：admin
密码：admin123
角色：SUPER_ADMIN
```

测试配置显式启用 `SystemDataInitializer` 并注入上述账号密码。正式运行默认不装配初始化器，代码和正式配置不包含固定管理员密码；空数据库首次初始化必须显式设置 `ONES_BOOTSTRAP_ENABLED=true` 和 `ONES_BOOTSTRAP_ADMIN_PASSWORD`，初始化完成后立即关闭开关。

## v0.0.148 生产安全基线

- `sys_user.failed_login_count` 使用数据库原子 SQL 递增，避免并发失败请求相互覆盖。
- 账号锁定继续由 `ONES_LOGIN_MAX_FAILED_COUNT` 和 `ONES_LOGIN_LOCK_DURATION` 控制，默认 5 次失败锁定 15 分钟。
- Redis 登录限流同时按账号和客户端 IP 计数，Redis Key 使用 SHA-256 摘要，不直接暴露用户名或 IP；测试环境可切换为内存存储。
- 账号默认 10 次、IP 默认 30 次失败进入 15 分钟限流，限流响应使用 HTTP 429 和认证错误码 `4203`。
- 账号登录成功后清理账号维度失败状态，但不会清理共享 IP 的失败状态，避免单个成功登录绕过 IP 防护。
- Swagger、OpenAPI 和 Actuator 纳入 Sa-Token 拦截范围；生产默认关闭 Swagger，公开端点必须通过 `PublicEndpointRegistry` 对应配置显式放开。
- 后端统一增加 `nosniff`、禁止 iframe、Referrer Policy 和 Permissions Policy；跨域只允许配置清单内的来源，Token 继续只从 Authorization Header 读取。

## 飞书扫码登录与 SSO 接入建议

飞书扫码登录本质是平台 OAuth 授权流程。v0.0.149 已落地以下链路：

```text
飞书扫码 / 飞书工作台免登
  ↓
飞书返回授权码
  ↓
ONES-ADMIN ThirdPartyAuthProvider 换取飞书用户身份
  ↓
ExternalIdentityService 只解析管理员预绑定的本地用户
  ↓
AuthService 加载本地角色、菜单、数据权限
  ↓
Sa-Token 签发 ONES-ADMIN Token
```

这样可以避免让外部平台直接决定系统内权限，便于后续支持多个平台。

## v0.0.149 飞书 OAuth 闭环

- `GET /api/auth/providers` 只返回 Provider 名称和启用状态，不返回 App ID 或 Secret。
- `GET /api/auth/oauth/feishu/authorize` 由后端生成 256 bit 随机 state，默认在 Redis 保留 5 分钟。
- `GET /api/auth/oauth/feishu/callback` 原子消费 state，调用飞书应用访问令牌、用户访问令牌和用户信息接口，以 `tenant_key + union_id` 解析外部身份。
- `POST /api/auth/oauth/exchange` 原子消费 60 秒短期票据，加载本地账号角色与权限后签发 Sa-Token；state 和票据均不可重放。
- `sys_external_identity` 保存 Provider、租户键、外部主体、本地用户、状态和最后登录时间；不保存飞书 access token。
- 登录日志增加 `auth_method`、`provider` 和 `external_identity_id`，账号密码与飞书登录使用同一审计查询和导出链路。
- 默认不按邮箱自动创建或绑定账号。管理员使用 `/api/system/users/{userId}/external-identities` 预绑定，未绑定身份返回错误码 `4213`。
- 飞书 App Secret 只允许通过后端环境变量或 Jenkins Credentials 注入，前端只读取 Provider 启用状态和后端生成的授权地址。

### 数据库迁移影响与回滚

V9 是纯新增迁移：新增 `sys_external_identity` 表，为 `sys_login_log` 增加三个可兼容审计字段和索引，不删除或重命名旧字段。真实 MySQL 执行前必须完成备份、表结构检查和回调配置确认。本项目不对已执行迁移做降级 SQL；需要停用时先关闭 `ONES_AUTH_FEISHU_ENABLED`，保留数据，结构调整通过后续前向补偿迁移完成。

## 后续 Provider 扩展

当前 `auth.oauth` 包已经形成以下边界：

```text
auth.oauth
├── ThirdPartyAuthProvider
├── ThirdPartyAuthProviderRegistry
├── OAuthFlowService / OAuthFlowStore
├── ExternalIdentityService
└── feishu/FeishuThirdPartyAuthProvider
```

新增企业微信、钉钉或 OIDC 时继续实现统一接口，不得绕过 state、一次性票据、本地身份绑定和登录审计：

```java
public interface ThirdPartyAuthProvider {
    String id();

    ExternalIdentityProfile authenticate(String authorizationCode);
}
```

## Sa-Token 存储演进

当前默认使用 Redis 会话，支持多实例共享登录态；本地 E2E 或临时 H2 验证可通过 `ONES_SECURITY_SESSION_STORAGE=memory` 显式切换为内存 DAO，避免非联调场景强依赖 Redis：

- 配置独立 Redis 库或独立 key 前缀。
- 登录、踢人、续期、并发登录控制全部走 Redis。
- 多实例部署时所有节点共享登录态。
- 内存 DAO 只用于本地验证，不用于正式部署。

## 与 Spring Security 的边界

当前不引入 Spring Security 作为主认证框架，原因是首版核心诉求是企业后台 RBAC 和快速迭代。

只有出现以下情况才建议重新评估：

- ONES-ADMIN 需要成为统一身份认证中心。
- 客户明确要求标准 OIDC/SAML。
- 需要深度接入 Okta、Azure AD、ADFS、LDAP。
- 安全团队要求采用 Spring 官方安全生态。
