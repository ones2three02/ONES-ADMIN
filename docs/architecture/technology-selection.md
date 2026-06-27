# ONES-ADMIN 技术选型分析

## 目标定位

ONES-ADMIN 面向企业级后端管理系统，首要目标不是堆满功能，而是建立一套可持续迭代的工程基座：权限清晰、前后端分层明确、后续能平滑扩展多租户、数据权限、代码生成、工作流、第三方登录与监控能力。

## 前端选型

首版采用 Vue3 + Vite + TypeScript + Element Plus + Pinia + Vue Router。

候选项目对比：

| 项目 | 优点 | 风险 | 适合借鉴 |
| --- | --- | --- | --- |
| Vben Admin | 工程化成熟、社区体量大、Vue3/Vite/TS 基础好 | 功能体系较大，直接照搬成本高 | 工程结构、权限路由、布局设计 |
| Cool Admin | 模块化、插件化、CRUD 效率突出 | 框架特有约定较多 | 插件体系、快速 CRUD 思路 |
| Art Design Pro | 视觉体验和交互完成度较高 | 企业后台工程纵深不如 Vben | UI 质感、页面细节 |

推荐策略：以 Vben 的工程化和 `web-ele` 视觉范式作为主要参考，吸收 Cool Admin 的模块化/插件化/CRUD 思路，再借鉴 Art Design Pro 的页面细节。首版先落地一个轻量但真实可运行的 Vue3 管理端，不直接复制任何一个框架。

当前前端实现要求：

- 布局、色彩 token、侧栏、顶栏、标签栏、工作台信息密度对齐 Vben Admin `apps/web-ele`。
- 使用 Element Plus 作为控件体系，但不另起一套自定义后台审美。
- 后续新增页面优先复用当前页面头、查询工具条、表格、弹窗表单模式。

## 后端选型

首版采用 Spring Boot 3.5.x + Sa-Token + SpringDoc + Actuator，先使用模块化单体，保留未来拆分微服务的边界。

后端参考项目：

| 项目 | 借鉴点 | 不直接采用的原因 |
| --- | --- | --- |
| RuoYi-Vue | 经典后台管理功能完整，权限模型清晰 | 老版本包袱较多，部分实现需要现代化 |
| RuoYi-Vue-Plus | Sa-Token、多租户、插件化、数据权限、监控能力强 | 体系较大，首版全量引入会增加复杂度 |
| Yudao Cloud | 模块边界和业务能力地图完整 | 微服务与业务模块很重，不适合首版起步 |
| JeecgBoot | 低代码、代码生成、在线表单能力强 | 平台化程度高，容易过早绑定复杂体系 |
| Pig | OAuth2、网关、微服务治理成熟 | 更偏统一认证和微服务平台 |

推荐策略：首版选择模块化单体，先把登录、用户、角色、菜单、数据权限这些核心基础打牢。等业务边界稳定后，再按网关、认证中心、系统服务、业务服务拆分。

## Sa-Token 与 Spring Security

| 维度 | Sa-Token | Spring Security |
| --- | --- | --- |
| 后台 RBAC | API 和注解直观，适合菜单/按钮权限 | 能力强，但配置与扩展复杂 |
| Token 会话 | 多端登录、踢人、续期、Redis 会话更贴近后台场景 | 需要组合 Resource Server、JWT、Session 等机制 |
| OAuth2/OIDC | 支持 SSO/OAuth2 插件，适合业务系统集成 | 官方生态更强，适合标准身份平台 |
| 学习成本 | 低 | 高 |
| 适用定位 | 企业后台业务系统 | 统一身份、安全合规、复杂企业 IdP |

推荐结论：ONES-ADMIN 首版采用 Sa-Token。它更适合当前“企业后台管理系统”的核心需求：登录、RBAC、菜单权限、按钮权限、多端登录、Token 管理和快速迭代。

如果项目未来升级为统一身份中心，或必须深度对接 Okta、Azure AD、ADFS、OIDC、SAML，再评估 Spring Security / Spring Authorization Server / Keycloak。

## 首版落地架构

```text
Vue3 管理端
  ├─ 登录页
  ├─ Axios Token 拦截
  ├─ Pinia 登录态
  └─ 动态菜单与路由守卫

Spring Boot 后端
  ├─ AuthController
  ├─ AuthService
  ├─ MyBatisUserRepository
  ├─ UserManagementService
  ├─ SaPermissionProvider
  ├─ MenuController
  ├─ UserController
  ├─ RoleController
  └─ GlobalExceptionHandler

Sa-Token
  ├─ 登录态签发
  ├─ Token 校验
  ├─ 角色权限查询
  └─ 注解鉴权
```

当前用户、角色、权限已经接入 MyBatis-Plus + H2 内存库，默认账号由 `SystemDataInitializer` 初始化。H2 只作为本地验证基座，生产环境需要切换 MySQL/PostgreSQL，并补充数据库迁移工具。

## 演进路线

1. 基础后台：用户、角色、菜单、部门、岗位、字典、参数配置、操作日志。
2. 持久化：MyBatis-Plus、数据库迁移脚本、Redis 会话存储。
3. 企业权限：菜单权限、按钮权限、数据权限、租户权限。
4. 第三方登录：飞书、企业微信、钉钉，通过统一认证适配层接入。
5. 效率工具：代码生成、通用 CRUD、导入导出、审计日志。
6. 高阶能力：工作流、消息通知、监控告警、微服务拆分。
