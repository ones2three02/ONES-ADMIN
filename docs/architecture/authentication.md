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

飞书扫码登录本质是平台 OAuth 授权流程。推荐接入方式：

```text
飞书扫码 / 飞书工作台免登
  ↓
飞书返回授权码
  ↓
ONES-ADMIN ThirdPartyAuthProvider 换取飞书用户身份
  ↓
ExternalIdentityService 绑定或创建本地用户
  ↓
AuthService 加载本地角色、菜单、数据权限
  ↓
Sa-Token 签发 ONES-ADMIN Token
```

这样可以避免让外部平台直接决定系统内权限，便于后续支持多个平台。

## 后续模块建议

建议新增 `auth-provider` 模块或包：

```text
auth-provider
├── ThirdPartyAuthProvider
├── ThirdPartyAuthRequest
├── ThirdPartyIdentity
├── FeishuAuthProvider
├── WeComAuthProvider
└── DingTalkAuthProvider
```

统一接口示例：

```java
public interface ThirdPartyAuthProvider {
    String platform();

    ThirdPartyIdentity authenticate(ThirdPartyAuthRequest request);
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
