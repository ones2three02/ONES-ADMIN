# ONES-ADMIN 版本记录

## v0.0.47 - 2026-07-02

- 后端新增审计日志保留策略配置，登录日志和操作日志默认保留 180 天，可通过环境变量分别覆盖。
- 后端新增审计保留策略查询接口 `/api/system/audit/retention`，返回过期阈值和待清理数量，便于运维和 Jenkins 发布前巡检。
- 后端新增过期审计日志清理接口 `/api/system/audit/retention/cleanup`，使用独立权限 `system:audit:retention`，并接入防重复提交和操作审计。
- 产品版本递增至 `v0.0.47`。

## v0.0.46 - 2026-07-02

- 后端新增登录日志 CSV 导出接口 `/api/system/audit/login-logs/export`，沿用登录日志查询筛选条件和 `system:audit:login-log` 权限。
- 后端新增操作日志 CSV 导出接口 `/api/system/audit/operation-logs/export`，沿用操作日志查询筛选条件和 `system:audit:operation-log` 权限。
- 新增通用 CSV 导出工具，统一处理逗号、换行、引号和公式注入防护，降低审计留档文件在表格软件中打开时的安全风险。
- 产品版本递增至 `v0.0.46`。

## v0.0.45 - 2026-07-02

- 新增 HRMS 企业级人力系统调研与一期范围建议，沉淀 Frappe HR、OrangeHRM、IceHrm、Odoo Employees 等参考来源和模块边界。
- 修正前端技术选型文档，明确当前主线为 `vbenjs/vue-vben-admin` 新版 playground、Vben 组件体系和 Ant Design Vue 风格，不再沿用早期 Element Plus 描述。
- 产品版本递增至 `v0.0.45`。

## v0.0.44 - 2026-07-02

- 增强接口治理报告，新增接口负责人、主要调用方、治理规则和治理分类聚合字段。
- 接口管理页补充目录分布与治理分布视图，保持 Vben/Ant Design Vue 现有后台风格。
- 补充后端治理报告断言与前端 E2E 可见性断言，确保新增治理视角可被持续验证。
- 产品版本递增至 `v0.0.44`。

## v0.0.43 - 2026-07-01

- 登录页在 Vben 认证布局内继续升级为 ONES / 1S 系统核心视觉，保留原有登录表单、工具栏、国际化和提交流程，不另起独立页面风格。
- 左侧品牌区从卡片化面板调整为沉浸式核心舞台，强化 1S 发光核心、环绕轨道、能力锚点和底部理念条，更贴近参考图里的企业级品牌系统视觉。
- 右侧登录标题区新增 `ONES-ADMIN ACCESS` 识别，统一认证、锁定保护、审计追踪改为克制的信息条，降低卡片堆叠感并保持企业后台入口稳定感。
- 新增 `docs/design/login-page-v0.0.43-implementation.png` 视觉证据，更新 `design-qa.md`，并完成桌面暗色态截图、DOM 可见性和文本溢出检查。
- 产品版本递增至 `v0.0.43`。

## v0.0.42 - 2026-07-01

- 登录页按 Vben 认证布局继续深化为企业级安全登录体验，右侧表单新增“安全登录”标题和统一认证、锁定保护、审计追踪状态项，保留真实账号密码、手机号登录和扫码登录入口。
- 收起未接入的公开注册入口和通用第三方图标区，避免企业后台登录页表现为可随意注册或存在未落地的消费级登录能力。
- 左侧 ONES 1S 品牌视觉升级为完整视觉面板，强化核心舞台、能力节点、轨道层次和理念条，并补齐窄桌面下表单居中、宽屏下展示品牌视觉的响应式策略。
- 新增登录页企业化 E2E 断言，并产出 `design-qa.md` 与 `docs/design/login-page-v0.0.42-comparison.png` 视觉 QA 证据，确保设计落地可追溯。
- 产品版本递增至 `v0.0.42`。

## v0.0.41 - 2026-07-01

- 后端接口资源元数据新增 `audience` 受众字段，接口列表、Manifest、CSV 导出和 Manifest 指纹均纳入调用方信息，便于研发、运维、审计和集成方按接口服务对象治理资产。
- 接口治理规则目录新增 `MISSING_API_AUDIENCE`，要求接口通过 `@ApiResourceMetadata(audience = "...")` 明确主要调用方；现有接口补齐 `ADMIN_PORTAL`、`ARCHITECTURE_GOVERNANCE`、`SECURITY_AUDITOR`、`OPS_PLATFORM` 等受众标识。
- 接口管理页新增调用方筛选项和表格列，后端接口目录能力同步落到前端可视化，不只停留在 API 响应体。
- 补充企业级接口管理参考：Backstage 强调 Developer Portal/API Catalog 可发现性，Gravitee 偏完整 API Management，Kong/APISIX/Tyk 偏网关与策略资产化；ONES-ADMIN 当前优先补齐接口目录元数据闭环。
- 产品版本递增至 `v0.0.41`。

## v0.0.40 - 2026-07-01

- 前端请求错误提示接入后端统一响应 `traceId`，业务错误弹窗会附带 `追踪ID`，便于用户截图报障后快速关联后端日志、审计记录和接口响应。
- 登录 E2E 新增失败登录场景，验证 `用户名或密码错误` 与 `追踪ID` 同时可见，避免可观测性只停留在后端响应体。
- 复用 Vben 请求拦截器风格，保持业务 API 返回仍只解包 `data` 字段，不破坏现有页面数据类型。
- 补充企业级可观测性参考：Sentry 强调面向用户报错的错误追踪，OpenTelemetry 强调服务链路追踪；ONES-ADMIN 当前将用户可见错误与后端 traceId 先闭环。
- 产品版本递增至 `v0.0.40`。

## v0.0.39 - 2026-07-01

- 后端统一响应 `ApiResult` 新增 `traceId` 字段，成功响应和异常响应都与 `X-Trace-Id` 响应头保持一致，便于前端报错、后端日志和审计记录串联排查。
- `TraceIdFilter` 暴露当前请求 traceId 读取方法，继续复用现有 MDC 与操作审计链路，不新增额外中间件依赖。
- 接口基础设施测试补充成功响应和参数校验错误响应的 traceId 断言，避免后续统一响应改造破坏可观测性契约。
- 补充企业级可观测性参考：OpenTelemetry Java instrumentation 强调 Java 服务链路追踪和自动埋点，ONES-ADMIN 当前先保持轻量 traceId 契约，后续可平滑升级标准分布式追踪。
- 产品版本递增至 `v0.0.39`。

## v0.0.38 - 2026-07-01

- 前端接口管理页接入 `/api/system/api-resources/governance/report` 聚合报告，统一消费后端治理汇总、规则目录和最新发布门禁干跑结果。
- 接口管理页新增 `发布门禁` 面板，展示当前版本、破坏性变更数、人工复核要求和机器可读检查项，例如 `API_GOVERNANCE_ERROR`。
- 接口管理页新增 `治理规则` 面板，展示规则编码、严重级别、阻断标识和修复建议，借鉴 Backstage API Catalog 的可发现性、Gravitee 的生命周期视角、Kong/APISIX/Tyk 的策略资产化思路。
- Playwright 接口管理 E2E 增加发布门禁与治理规则断言，确保后端治理能力不只停留在接口层。
- 产品版本递增至 `v0.0.38`。

## v0.0.37 - 2026-07-01

- 后端接口管理新增 `/api/system/api-resources/governance/report` 聚合报告接口，一次性返回应用版本、生成时间、治理汇总、治理结果、规则目录、Manifest 和基于最新快照的 Gate 干跑结果。
- 接口治理报告沿用 `system:api:list` 权限，便于 Jenkins、后续接口管理页和人工巡检消费同一份机器可读报告。
- 前端接口资源 API client 补充治理报告类型与请求函数，为接口管理页继续扩展 Manifest、Gate 和规则目录视图打底。
- 产品版本递增至 `v0.0.37`。

## v0.0.36 - 2026-07-01

- 后端新增 Maven Enforcer 构建运行时门禁，在 `validate` 阶段要求 JDK 21+ 和 Maven 3.9.0+，避免 Jenkins 或本地误用 Java 8 时出现难以定位的 Spring class version 报错。
- README 和后端工程化审计文档补充 Java 21 / Maven Enforcer 基线，明确 Jenkins 应优先使用项目内 `./mvnw` 并配置 JDK 21。
- 补充企业级接口管理调研结论：Backstage API Catalog 强调接口资产可发现性，Gravitee/Apicurio 强调接口生命周期与 API/Schema 注册，SmartAdmin/CoolAdmin 继续作为后端工程化、安全和模块边界参考。
- 产品版本递增至 `v0.0.36`。

## v0.0.35 - 2026-07-01

- 系统管理新增 `接口管理` 页面，按 Vben 现有后台风格展示接口治理状态、接口总数、写操作、权限缺失、废弃接口和接口资源清单。
- 前端新增接口资源 API client，支持查询 `/system/api-resources`、`/summary` 和 `/governance`，并提供路径、模块、方法、访问策略、风险等级、生命周期和权限缺失筛选。
- 后端动态菜单初始化新增 `/system/api-resources` 菜单页，并将 `system:api:list`、`system:api:publish` 权限挂到接口管理页面下，避免接口治理能力只存在于后端接口。
- 新增前端 Playwright 接口管理冒烟用例，验证登录后可访问接口管理页、看到治理摘要和接口资源清单。
- 补充企业级接口管理调研结论：Backstage 借鉴 API Catalog 的可发现性，Gravitee 借鉴 API 生命周期和集中目录，oasdiff 借鉴破坏性变更门禁，Spectral 借鉴规则化 API lint；ONES-ADMIN 当前继续以自有接口资源治理闭环落地。
- 产品版本递增至 `v0.0.35`。

## v0.0.34 - 2026-06-30

- 前端 Playwright 配置修复本地 E2E 端口契约，测试服务显式使用 `5555`，避免 `.env` 默认端口导致登录冒烟用例启动超时。
- 前端登录 E2E 断言从旧 `Vben Admin` 品牌同步为 `ONES-ADMIN`，确保品牌落地后质量门禁不再误报。
- 登录 E2E helper 按 Vben `SliderCaptcha` 真实成功条件拖动滑块，并断言登录后进入 `/dashboard/overview`，避免只点击按钮但没有验证登录结果。
- 登录页左侧升级为 Vben 认证布局内的 ONES 1S 品牌视觉组件，融入执行核心、实时感知、安全守护、服务交付、数据沉淀、持续进化、连接协同、智能决策等企业级能力节点。
- 登录页补充 `ONE SECOND`、`ONE SYSTEM`、`ONE SERVICE`、`ONE GOAL` 理念展示，移除登录标题中的非企业化表情，并将页脚版权品牌统一为 `ONES-ADMIN`。
- 补充企业级前端调研结论：Vben 主线强调 Vue3/Vite/TypeScript/Monorepo 与多 UI 适配，Cool Admin Vue 借鉴模块化和 CRUD 效率，Art Design Pro 借鉴视觉体验，但 ONES-ADMIN 前端风格继续以 Vben 为准。
- 产品版本递增至 `v0.0.34`。

## v0.0.33 - 2026-06-30

- 接口治理响应和规则目录新增 `apiVersionPattern`，统一暴露接口版本元数据格式规范：`^v\d+\.\d+\.\d+$`。
- 接口治理新增 `API_SINCE_VERSION_INVALID_FORMAT` 警告规则，识别 `@ApiResourceMetadata.sinceVersion` 不符合产品版本格式的接口资产。
- 废弃接口治理新增 `DEPRECATED_API_SUNSET_VERSION_INVALID_FORMAT` 警告规则，识别 `sunsetVersion` 格式不规范导致下线计划无法审计的问题。
- 补充企业级接口管理调研结论：Spectral/Optic 强调规则化 API lint 与设计质量门禁，oasdiff 强调可比较的版本差异治理，Backstage 强调 API Catalog 元数据可追踪。
- 产品版本递增至 `v0.0.33`。

## v0.0.32 - 2026-06-30

- 接口契约治理新增 `API_METHOD_NOT_EXPLICIT` 阻断规则，禁止 `/api/**` 暴露未显式声明 HTTP 方法的 `ALL` 泛匹配接口。
- 接口治理可识别仅使用 `@RequestMapping` 且未指定 `method` 的接口，要求改为 `@GetMapping`、`@PostMapping` 或显式 `@RequestMapping(method = ...)`。
- 补充企业级接口管理调研结论：Spectral 强调规则化契约 lint，oasdiff 强调 method+path 级别的接口差异，Kong 强调运行时路由策略稳定性。
- 产品版本递增至 `v0.0.32`。

## v0.0.31 - 2026-06-30

- 公开接口治理新增 `PUBLIC_API_NOT_IN_RUNTIME_WHITELIST` 阻断规则，要求 `@ApiAccessPolicy(ApiAuthType.PUBLIC)` 必须同步加入 Sa-Token 运行时白名单。
- 接口治理可识别“接口目录标记公开，但运行时拦截器未放行”的不一致风险，避免后续飞书扫码、SSO 回调等入口只改文档不改拦截器。
- 补充企业级接口管理调研结论：Kong、APISIX、Tyk、apiman 均强调路由策略与运行时网关行为一致。
- 产品版本递增至 `v0.0.31`。

## v0.0.30 - 2026-06-30

- 公开接口治理新增 `PUBLIC_API_WITHOUT_ACCESS_POLICY` 阻断规则，`PUBLIC` 接口必须显式声明 `@ApiAccessPolicy(ApiAuthType.PUBLIC, reason = "...")`。
- 登录入口和健康检查补充公开访问策略说明，明确开放原因和安全补偿措施，便于后续飞书扫码、SSO 回调等公开入口复用同一治理口径。
- 补充企业级接口管理调研结论：Kong、APISIX、Tyk 强调 API 策略资产化，Backstage 强调 API Catalog 的 owner、生命周期和可解释元数据。
- 产品版本递增至 `v0.0.30`。

## v0.0.29 - 2026-06-30

- 接口资源清单、CSV 导出、Manifest 和 Manifest 指纹新增 `repeatSubmitProtected`，用于记录接口是否具备重复提交防护。
- 接口治理规则新增 `HIGH_RISK_WRITE_API_WITHOUT_REPEAT_SUBMIT`，非公开高风险写接口缺少 `@RepeatSubmit` 时作为 ERROR 阻断发布门禁。
- 为认证刷新、退出登录和接口 Manifest 发布等高风险写接口补充重复提交防护，登录接口继续由失败次数与临时锁定策略保护。
- 补充企业级接口管理调研结论：SmartAdmin 强调登录安全与三级等保能力，apiman/APIPark 强调 API 管理策略资产化，Spectral 强调规则化质量门禁。
- 产品版本递增至 `v0.0.29`。

## v0.0.28 - 2026-06-30

- 接口生命周期治理新增 `LIFECYCLE_DEPRECATED_WITHOUT_DEPRECATED_FLAG` 规则，要求 `lifecycle=DEPRECATED` 的接口同步 OpenAPI `deprecated` 标记。
- 接口生命周期治理新增 `REMOVED_API_STILL_MAPPED` 阻断规则，生命周期为 `REMOVED` 的接口不得继续暴露运行时路由。
- 补充企业级接口管理调研结论：Backstage/Gravitee 强调 API 生命周期资产治理，Spectral 强调规则化一致性检查。
- 产品版本递增至 `v0.0.28`。

## v0.0.27 - 2026-06-30

- `@ApiResourceMetadata` 新增 `sunsetVersion` 和 `replacementApiKey`，用于记录废弃接口计划下线版本和替代接口。
- 接口资源列表、CSV 导出、Manifest 和 Manifest 指纹均纳入废弃接口迁移元数据，便于 API Catalog、Jenkins 和人工审计跟踪接口下线计划。
- 接口治理规则新增 `DEPRECATED_API_MISSING_SUNSET_VERSION` 和 `DEPRECATED_API_MISSING_REPLACEMENT`，对缺少下线版本或替代接口的废弃 API 输出 WARN 级治理结果。
- 产品版本递增至 `v0.0.27`。

## v0.0.26 - 2026-06-30

- Manifest Gate 响应新增 `checks` 机器可读检查项列表，覆盖接口治理错误、破坏性变更人工确认、治理警告跟踪和差异归档建议。
- Jenkins 可按 `checkCode`、`passed`、`blocking` 和 `remediation` 生成稳定门禁报告，避免解析中文 `reasons` 字符串。
- 补充企业级接口管理调研结论：Gravitee/Kong 强调策略化 API 管理，oasdiff 强调破坏性变更识别，Spectral 强调规则化 lint/check。
- 产品版本递增至 `v0.0.26`。

## v0.0.25 - 2026-06-30

- 新增 `/api/system/api-resources/governance/rules` 接口，结构化输出接口治理规则目录、严重级别、阻断属性、分类和修复建议。
- 将接口治理规则从散落的修复建议收敛为统一规则目录，便于 Jenkins、接口管理页和人工巡检按规则编码解释治理结果。
- 补充企业级接口管理调研结论：Backstage 强调 API Catalog 与 Owner，Gravitee/APISIX/Kong/Tyk/apiman 强调策略化 API 管理，oasdiff/openapi-diff 强调契约差异和破坏性变更门禁。
- 产品版本递增至 `v0.0.25`。

## v0.0.24 - 2026-06-30

- 将 MinIO Java SDK 调整为 `8.5.17` 稳定线，避免 SDK 9.x 与当前 Maven/OkHttp 5 JVM 依赖组合在本地 MinIO 服务上出现响应解析兼容问题。
- 移除显式 `okhttp-jvm` 依赖，减少对象存储接入的依赖面。
- MinIO 客户端新增 `proxy-enabled` 配置，默认对内网对象存储直连，避免 Java/OkHttp 误走系统代理导致上传请求断流。
- 产品版本递增至 `v0.0.24`。

## v0.0.23 - 2026-06-30

- 新增文件存储抽象，文件上传控制器只负责校验和响应，存储实现可在本地目录与 MinIO 之间配置切换。
- 新增 RabbitMQ 系统事件发布抽象，登录日志和操作日志落库后发布审计事件，默认关闭外发以避免本地与测试环境强依赖消息中间件。
- 新增 MinIO SDK 接入和 RabbitMQ 事件交换机、队列、绑定配置，生产配置统一通过环境变量或本地忽略配置注入。
- 产品版本递增至 `v0.0.23`。

## v0.0.22 - 2026-06-30

- 接口治理违规项新增 `remediation` 修复建议字段，让 Jenkins、后续接口管理页和人工巡检能直接看到治理动作。
- 为权限、`operationId`、生命周期元数据、OpenAPI 元数据、废弃接口等治理规则补充规则级修复建议。
- 产品版本递增至 `v0.0.22`。

## v0.0.21 - 2026-06-30

- 新增 `/api/system/api-resources/manifest/gate/latest` 接口，支持基于最新已发布 Manifest 快照执行接口契约门禁干跑。
- 返回门禁基线快照 ID、版本和指纹，便于 Jenkins 和人工审批定位本次发布对比基线。
- 产品版本递增至 `v0.0.21`。

## v0.0.20 - 2026-06-30

- 新增接口资源 Manifest 发布快照表 `sys_api_manifest_snapshot`，支持将接口契约以服务端版本资产落库。
- 新增 `/api/system/api-resources/manifest/snapshots` 查询接口，支持分页查看历史 Manifest 快照。
- 新增 `/api/system/api-resources/manifest/snapshots/latest` 查询接口，便于 Jenkins 获取上一版契约。
- 新增 `/api/system/api-resources/manifest/snapshots` 发布接口，发布前自动执行 Manifest Gate，未通过门禁不落库。
- 发布快照接口具备幂等性，同一应用版本和 Manifest 指纹重复发布时返回既有快照。
- 新增权限点 `system:api:publish`，接口资源发布与接口资源查询分权治理。
- 产品版本递增至 `v0.0.20`。

## v0.0.19 - 2026-06-30

- 新增 1S 品牌静态资产，替换前端 favicon、侧边栏 Logo 和认证页品牌图标。
- 登录页文案融入 `ONE SYSTEM · ONE SERVICE · ONE SAFE · ONE SECOND` 品牌理念，保持 Vben 原有认证布局与组件风格。
- 系统概览页新增四项轻量品牌原则展示：`ONE SYSTEM`、`ONE SERVICE`、`ONE SAFE`、`ONE SECOND`。
- 认证布局的自定义 `sloganImage` 补充 `object-contain`，避免品牌图在固定展示区域内被拉伸变形。
- 产品版本递增至 `v0.0.19`，保持前后端版本标识一致。

## v0.0.18 - 2026-06-29

- 接口治理质量门禁新增 `operationIdPattern`，当前规范为 `^[A-Z][A-Za-z0-9]*_[a-z][A-Za-z0-9]*$`。
- 新增 `OPERATION_ID_INVALID_FORMAT` 错误规则，阻断不符合命名规范的接口操作标识。
- 新增 `OPERATION_ID_DUPLICATED` 错误规则，阻断重复 `operationId`，保证 API Catalog、客户端生成和 Jenkins 报告可稳定定位接口。
- 接口治理违规项新增 `operationId` 字段，便于前端接口管理页和 CI 报告直接定位问题接口。
- 补充专门测试 Controller，验证重复和非法 `operationId` 会导致治理门禁失败。

## v0.0.17 - 2026-06-29

- 接口资源清单新增稳定 `operationId` 字段，默认采用 `ControllerSimpleName_methodName`，优先兼容 `@Operation.operationId` 显式配置。
- 接口资源 CSV 导出和 Manifest 均纳入 `operationId`，便于 API Catalog、客户端生成和 Jenkins 产物归档。
- Manifest 指纹计算纳入 `operationId`，接口身份变化会触发契约变更。
- Manifest Diff 将 `operationId` 变化识别为破坏性变更，避免客户端生成、接口目录和自动化调用引用失效。
- 后端工程化审计补充 Backstage、Gravitee、APISIX、Tyk、apiman、openapi-changes 等接口管理项目调研结论。

## v0.0.16 - 2026-06-29

- 新增接口资源 Manifest 发布门禁接口 `/api/system/api-resources/manifest/gate`。
- 发布门禁会综合接口治理结果和 Manifest Diff，输出 `passed`、`status`、阻断原因和差异明细。
- 存在接口治理错误时直接阻断发布；存在破坏性接口契约变更时默认阻断发布。
- 破坏性变更只有在显式允许并填写人工确认原因后才返回 `MANUAL_APPROVED` 通过状态。
- README 首版接口表新增 Manifest Gate 入口，便于 Jenkins 直接接入发布准入。

## v0.0.15 - 2026-06-29

- 新增接口资源 Manifest 差异对比接口 `/api/system/api-resources/manifest/diff`。
- Manifest Diff 支持识别新增、删除、修改接口资源，并统计破坏性变更数量。
- 删除接口、认证级别变更、权限码变更、权限模式变更和写操作属性变更会被标记为破坏性变更。
- Manifest Diff 沿用 `system:api:list` 权限控制，便于 Jenkins 在发布前做接口契约差异门禁。
- README 首版接口表新增 Manifest Diff 入口。

## v0.0.14 - 2026-06-29

- 新增接口生命周期元数据注解 `@ApiResourceMetadata`，用于维护接口负责人、引入版本、生命周期和风险级别。
- 接口资源列表、Manifest、CSV 导出和汇总接口新增生命周期元数据字段，便于 Jenkins、审计和后续接口管理页使用。
- 接口资源查询支持按负责人、生命周期和风险级别筛选。
- 接口治理质量门禁新增缺少负责人、引入版本、生命周期、风险级别的结构化警告规则。
- 现有后端 Controller 已补齐接口元数据，RBAC、文件上传等写接口标记为高风险。

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
