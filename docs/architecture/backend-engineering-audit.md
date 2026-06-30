# ONES-ADMIN 后端工程化审计与优化路线

更新时间：2026-06-30
当前分支：develop

## 1. 调研来源

本轮重点参考了以下开源企业后台项目，调研渠道为 agent-reach GitHub/dev 路由与 GitHub CLI，访问日期为 2026-06-30。

| 项目 | 后端特征 | 可借鉴重点 |
| --- | --- | --- |
| [YunaiV/ruoyi-vue-pro](https://github.com/YunaiV/ruoyi-vue-pro) | Spring Boot、MyBatis-Plus、RBAC、多租户、工作流、第三方登录 | 权限体系、数据权限、SaaS 多租户、接口治理 |
| [dromara/RuoYi-Vue-Plus](https://github.com/dromara/RuoYi-Vue-Plus) | Spring Boot、Sa-Token、MyBatis-Plus、SpringDoc、OSS | Sa-Token 落地方式、接口文档、文件存储 |
| [jeecgboot/JeecgBoot](https://github.com/jeecgboot/JeecgBoot) | 低代码、Online 表单、权限、报表 | 低代码和企业支撑能力边界 |
| [1024-lab/smart-admin](https://github.com/1024-lab/smart-admin) | Spring Boot 3、Sa-Token、MyBatis-Plus、三级等保、安全体系；Java 17 版本采用 `sa-base`、`sa-admin` 多模块 | 错误码体系、接口文档、登录安全、操作日志、数据字典、数据变更记录、基础能力模块化 |
| [cool-team-official/cool-admin-java](https://github.com/cool-team-official/cool-admin-java) | Spring Boot 3、MyBatis-Flex、`core` + `modules`、插件化、自动初始化 | 核心能力和业务模块分离、插件扩展思想、统一返回/分页、文件配置、请求日志、代码生成 |
| [backstage/backstage](https://github.com/backstage/backstage) | 开发者门户、软件目录、API Catalog、Owner 与生命周期治理 | API 资产目录、负责人、生命周期、稳定接口身份 |
| [gravitee-io/gravitee-api-management](https://github.com/gravitee-io/gravitee-api-management) | Java 开源 API Management | API 生命周期、发布治理、订阅与网关策略 |
| [apache/apisix](https://github.com/apache/apisix) | 云原生 API Gateway | 网关策略、路由治理、插件化能力边界 |
| [apiman/apiman](https://github.com/apiman/apiman) | Java API Management，可扩展策略插件 | API 管理策略、扩展点和治理边界 |
| [TykTechnologies/tyk](https://github.com/TykTechnologies/tyk) | Go API Gateway，支持 REST、GraphQL、TCP、gRPC | API 网关、鉴权、流量治理思路 |
| [pb33f/openapi-changes](https://github.com/pb33f/openapi-changes) | OpenAPI 破坏性变更检测 CLI | CI/CD 契约变更报告、破坏性变更识别 |
| [oasdiff/oasdiff](https://github.com/oasdiff/oasdiff) | OpenAPI Diff 与 Breaking Changes | Manifest Diff 与发布门禁设计 |
| [OpenAPITools/openapi-diff](https://github.com/OpenAPITools/openapi-diff) | Java OpenAPI 差异比较工具 | Java 生态契约差异检测 |
| [Kong/kong](https://github.com/Kong/kong) | API/AI Gateway、插件化策略、流量治理 | 策略目录化、网关能力边界、接口治理与运行时策略分层 |
| [stoplightio/spectral](https://github.com/stoplightio/spectral) | OpenAPI/AsyncAPI 规则化 lint 工具 | 规则编码、检查结果、CI 报告可机器解析 |
| [APIParkLab/APIPark](https://github.com/APIParkLab/APIPark) | AI/API 网关、开放平台、API 申请审批、调用统计 | API 申请审批、调用统计、开放平台体验 |
| [apioo/fusio](https://github.com/apioo/fusio) | Self-hosted API Management | API 管理后台、接口发布、开发者消费侧能力 |

结论：ONES-ADMIN 当前不宜盲目切换 ORM 或引入插件化/低代码大框架，短期应优先补齐接口治理、安全边界、审计日志、文件治理、数据字典等企业后台底座。

补充取舍：

- Smart Admin 的价值在于“安全与规范”：登录安全、数据脱敏、数据变更记录、统一错误码、操作日志、系统参数、字典等能力适合作为 ONES-ADMIN 的企业级底座路线。
- Cool Admin Java 的价值在于“模块化与扩展”：`core` 放通用底座，`modules` 放业务模块的划分方式值得借鉴；插件化、AI 代码生成、多租户暂不作为当前主线，避免底座阶段复杂度过高。
- RuoYi/Yudao 的价值在于“框架化治理”：API 访问日志、错误日志、操作日志、安全框架、数据权限等能力拆成 starter 的方式值得后续模块化时参考。
- 本轮接口管理吸收了三类做法：Smart Admin 的接口文档标签规范、Cool Admin Java 的 OpenAPI 自定义资源思路、RuoYi/Yudao 的 API 访问日志可定位 Controller 方法思路。
- API Catalog 类项目强调稳定接口身份、负责人和生命周期；API Gateway/API Management 类项目强调发布准入、策略和流量治理；OpenAPI Diff 类项目强调 CI 中识别破坏性变更。
- Kong、APISIX、Tyk、apiman、Gravitee 这类 API 管理项目普遍把“策略/规则”作为一等资产管理，而不是只在报错时输出一段提示；Spectral 这类规则化 lint 工具也强调稳定规则编码和机器可读结果；ONES-ADMIN 的接口治理应持续暴露规则目录和检查项，便于 CI、前端和人工巡检统一解释。
- 公开接口本质上属于运行时安全策略资产，登录入口、健康检查、第三方扫码回调、SSO 回跳地址都必须在接口目录中显式说明开放原因、调用方和安全补偿措施，避免白名单随迭代失控。
- Kong、APISIX、Tyk、apiman 这类网关或 API Management 项目都强调路由策略必须和运行时执行行为一致；ONES-ADMIN 的接口目录不能只描述“想要公开”，还必须校验 Sa-Token 拦截器是否真实放行。
- Spectral 强调 OpenAPI 契约可规则化 lint，oasdiff 强调 method+path 级别识别接口差异；ONES-ADMIN 的接口扫描必须避免 `ALL` 泛匹配接口，否则 Manifest Diff、网关路由和客户端生成都会失去稳定契约。
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
- 接口资源清单已提供稳定接口标识 `apiKey`、稳定操作标识 `operationId`、处理器定位 `handler` 和废弃状态 `deprecated`，便于接口管理页、API Catalog、客户端生成、审计排障和 Jenkins 报告直接定位到 Controller 方法。
- 接口资源清单已补充接口负责人、引入版本、生命周期、风险级别、计划下线版本和替代接口元数据，便于接口归属、下线计划、风险审计和 Jenkins 报告分组。
- 已提供接口资源 CSV 导出接口，便于审计留档、接口变更对比和 Jenkins 产物归档。
- 已提供接口资源 Manifest，输出稳定 JSON 清单和 `SHA-256` 指纹，便于接口契约归档和版本间变更比对。
- 已提供接口资源 Manifest Diff，可对比上一版本 Manifest 和当前运行时 Manifest，输出新增、删除、修改和破坏性变更统计。
- 已提供接口资源 Manifest 发布门禁，可综合接口治理错误、破坏性接口变更和人工确认原因输出发布准入结果。
- Manifest Gate 已输出机器可读 `checks` 检查项，Jenkins 可按 `checkCode`、`passed`、`blocking` 和 `remediation` 生成稳定门禁报告。
- 已将 `operationId` 纳入接口清单、CSV、Manifest、Manifest 指纹和 Manifest Diff，接口操作标识变化按破坏性变更处理。
- 已建立 `operationId` 命名规范和唯一性门禁，当前格式为 `^[A-Z][A-Za-z0-9]*_[a-z][A-Za-z0-9]*$`，重复或非法命名会作为治理错误阻断发布。
- 已建立显式 HTTP 方法治理，`/api/**` 接口不得使用未指定 method 的 `ALL` 泛匹配路由，避免接口契约、网关路由和客户端生成不稳定。
- 已建立接口治理质量门禁，可输出权限缺口、系统写接口无权限、接口文档元数据缺失等违规项。
- 已建立接口治理规则目录，结构化输出规则编码、严重级别、是否阻断、分类和修复建议，便于 Jenkins、接口管理页和人工巡检复用同一套规则解释。
- 已建立公开接口访问策略治理，`PUBLIC` 接口必须显式声明 `@ApiAccessPolicy(ApiAuthType.PUBLIC, reason = "...")` 并同步加入 Sa-Token 运行时白名单，否则作为安全错误阻断发布。
- 已建立废弃接口下线治理，废弃 API 必须维护 `sunsetVersion` 和 `replacementApiKey`，避免只有废弃标记但没有迁移路径。
- 已建立接口生命周期一致性治理，`DEPRECATED` 必须同步 OpenAPI 废弃标记，`REMOVED` 不允许继续暴露运行时路由。
- 已建立高风险写接口策略一致性治理，接口清单、CSV 和 Manifest 输出 `repeatSubmitProtected`，非公开高风险写接口缺少 `@RepeatSubmit` 时阻断发布。
- 已建立接口权限注册一致性检查，可识别代码权限点是否写入 `sys_permission`，以及是否挂载到 `sys_menu.auth_code` 供角色授权。
- 已建立权限码命名规范检查，当前统一使用小写冒号分段格式：`^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*){1,3}$`。
- 已建立模块化错误码体系，通用、认证、系统模块错误码分层维护，并通过测试校验跨模块错误码唯一性。
- 已接入 Flyway 数据库迁移治理，基线表结构位于 `server/src/main/resources/db/migration/V1__init_schema.sql`。
- 已补充数据库迁移质量门禁，校验脚本命名、版本连续性、旧 `schema.sql` 禁用和破坏性 SQL 审批标记。
- 高风险写接口已接入防重复提交保护，正式环境默认使用 Redis 存储防重票据；登录接口继续由失败次数和临时锁定策略保护。
- 文件存储已抽象为 `FileStorageService`，默认本地目录存储，可通过配置切换到 MinIO。
- 登录日志、操作日志落库后可发布统一系统事件，默认关闭外发，可通过配置切换到 RabbitMQ。
- MySQL、Redis、RabbitMQ、MinIO 配置通过环境变量注入，本地敏感配置不提交 Git。

主要缺口：

- 错误码模块化已起步，后续需要继续细分第三方登录、数据权限、系统参数、数据字典等新模块号段。
- 审计日志已具备基础能力，但前端页面、导出、保留策略仍待补齐。
- 数据字典、系统参数、文件元数据、数据权限、多租户仍待规划。
- Flyway 已接入基线迁移，后续仍需为每次表结构变更补充增量脚本、影响范围和回滚说明。

## 3. 本轮已落地优化

- 新增统一错误码接口 `ErrorCode`，并按通用、认证、系统模块拆分 `CommonErrorCode`、`AuthErrorCode`、`SystemErrorCode`。
- 新增错误码治理测试，校验跨模块错误码唯一性，并约束认证模块 `4200-4299`、系统模块 `4100-4399` 号段。
- 保持现有 `ApiResult<T>` 的 `code/message/data` 响应结构，新增基于错误码的失败响应方法。
- 新增 Flyway 数据库迁移治理：
  - 迁移脚本统一放在 `classpath:db/migration`，按 `V版本号__说明.sql` 命名。
  - 基线脚本 `V1__init_schema.sql` 承载当前系统表结构。
  - 关闭 `spring.sql.init`，移除旧的启动 DDL 补偿类，避免应用启动时隐式改表。
  - 对已有非空库启用 `baseline-on-migrate`，首次接入 Flyway 时记录基线，不重复执行 V1。
  - 新增测试校验 `flyway_schema_history` 存在迁移记录、迁移文件命名规范、版本连续且无旧 `schema.sql`。
  - 破坏性 SQL 必须包含 `ONES-MIGRATION-APPROVED-DESTRUCTIVE` 审批标记。
  - 详细规范见 [数据库迁移治理规范](database-migration-governance.md)。
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
  - 返回稳定接口标识 `apiKey`、稳定操作标识 `operationId`、处理器定位 `handler`、废弃状态 `deprecated`，用于接口排障、变更审计、API Catalog 和生命周期治理。
  - 返回接口负责人 `owner`、引入版本 `sinceVersion`、生命周期 `lifecycle`、风险级别 `riskLevel`、计划下线版本 `sunsetVersion`、替代接口 `replacementApiKey`，用于接口资产归属、废弃迁移和风险分级管理。
  - 支持分页和按方法、路径、模块、权限点、处理器、认证级别、写操作、权限缺口、废弃状态筛选。
  - 支持按负责人、生命周期和风险级别筛选接口资源。
  - 提供 `/api/system/api-resources/export` 接口资源 CSV 导出，字段覆盖接口标识、操作标识、处理器、权限点、权限状态、认证级别、废弃状态、下线计划和访问策略。
  - 提供 `/api/system/api-resources/manifest` 接口资源 Manifest，字段覆盖应用版本、资源总数、`SHA-256` 指纹、`operationId`、废弃迁移计划和关键接口元数据。
  - 提供 `/api/system/api-resources/manifest/snapshots` 接口资源 Manifest 发布快照查询与发布能力，支持把接口契约以服务端版本资产落库。
  - 提供 `/api/system/api-resources/manifest/snapshots/latest` 最新发布快照查询，便于 Jenkins 获取上一版契约做差异比对。
  - Manifest Snapshot 发布前会自动执行 Manifest Gate，门禁失败不落库；同一应用版本和指纹重复发布时返回既有快照，保证发布动作幂等。
  - 提供 `/api/system/api-resources/manifest/diff` 接口资源 Manifest Diff，支持比对上一版本 Manifest 与当前运行时 Manifest。
  - Manifest Diff 会识别新增、删除、修改接口资源；删除接口、`operationId`、认证级别、权限码、权限模式、写操作属性变化按破坏性变更处理。
  - 提供 `/api/system/api-resources/manifest/gate` 接口资源 Manifest 发布门禁，输出 `passed`、`status`、阻断原因、治理计数、机器可读检查项和差异明细。
  - 提供 `/api/system/api-resources/manifest/gate/latest` 最新发布快照门禁干跑，自动读取服务端最新 Manifest Snapshot 作为对比基线，并返回基线快照 ID、版本和指纹。
  - Manifest Gate 对接口治理错误直接阻断；对破坏性接口契约变更默认阻断，显式允许并填写人工确认原因后返回 `MANUAL_APPROVED`。
  - Manifest Gate 的 `checks` 覆盖 `API_GOVERNANCE_ERROR`、`BREAKING_CHANGE_REVIEW`、`GOVERNANCE_WARNING_TRACKING`、`MANIFEST_DIFF_ARCHIVE`，便于 Jenkins 不解析中文原因字符串即可生成质量报告。
  - 提供 `/api/system/api-resources/summary` 治理汇总接口，返回接口总数、写操作数、权限缺口数、访问策略数、废弃接口数、认证级别分布和模块分布。
  - 提供 `/api/system/api-resources/governance` 接口治理质量门禁，返回是否通过、错误数、警告数、权限码正则、`operationId` 正则和违规明细。
  - 提供 `/api/system/api-resources/governance/rules` 接口治理规则目录，返回规则编码、严重级别、是否阻断、规则分类、说明和修复建议。
  - 自动区分 `PUBLIC`、`LOGIN`、`PERMISSION` 三类接口认证级别。
  - 提供 `@ApiAccessPolicy` 显式标记登录态和公开接口的设计意图，减少安全巡检误报。
  - 登录入口和健康检查已补充公开访问策略说明，后续飞书扫码、飞书 SSO 回调等公开入口必须复用同一治理口径。
  - 对仅登录保护、且没有显式访问策略的系统接口标记权限缺口，便于安全巡检。
  - 对接口未显式声明 HTTP 方法、公开接口缺少显式访问策略、公开接口未同步运行时白名单、系统写接口无权限点、非公开高风险写接口缺少重复提交防护、权限码命名不规范、`operationId` 命名不规范、`operationId` 重复、权限点未注册、权限点不可授权、废弃接口、废弃接口缺少下线版本、废弃接口缺少替代接口、`DEPRECATED` 未同步 OpenAPI 废弃标记、`REMOVED` 接口仍暴露路由、缺少接口负责人、缺少引入版本、缺少生命周期、缺少风险级别、缺少 OpenAPI 模块标签、缺少接口摘要等问题输出结构化治理结果，便于 Jenkins 自动巡检。
  - 接口资源列表、CSV 导出、Manifest 和 Manifest Diff 已纳入 `repeatSubmitProtected`，重复提交防护策略变化会进入 Manifest 指纹和发布差异。
  - 每条接口治理违规项均返回 `remediation` 修复建议，便于 Jenkins 输出可执行治理动作，也便于后续接口管理页直接展示整改指引。
  - 新增权限点 `system:api:list` 和 `system:api:publish`，超级管理员启动时自动补齐授权，接口查询与接口发布分权治理。
  - 新增文件上传权限点 `system:file:upload`，文件上传不再只是登录态即可访问。
  - 审计日志、接口资源、文件上传等暂未落前端页面的权限，以菜单 button 节点挂入授权树，避免出现无页面组件的空路由。
  - 暂不新增前端菜单，避免出现无页面组件的空路由；后续按 Vben 风格补接口管理页后再挂菜单。
- 主要 Controller 补充 `@Tag`、`@Operation`，接口管理更清晰。
- 文件上传改为配置化治理：
  - 上传目录、公开访问前缀、最大文件大小、允许后缀均可配置。
  - 阻止空文件、超限文件和不允许的扩展名。
  - 下载时补充响应 Content-Type。
  - 存储实现通过 `ones.file.storage-type` 切换，当前支持 `local` 和 `minio`，便于开发、本地测试和对象存储部署共用同一接口。
- 新增登录日志：
  - 记录用户名、用户 ID、成功/失败、失败原因、IP、User-Agent、TraceId、时间。
  - 提供 `/api/system/audit/login-logs` 查询接口。
- 新增系统操作日志：
  - 自动记录 `/api/system/**` 下 POST、PUT、PATCH、DELETE 写操作。
  - 记录用户 ID、请求方法、路径、模块、动作、权限点、业务响应码、结果、TraceId、耗时。
  - 提供 `/api/system/audit/operation-logs` 查询接口。
- 新增系统事件发布基础设施：
  - 提供 `SystemEventPublisher` 和统一事件载荷 `SystemEventPayload`。
  - 默认 `ones.events.broker=none`，不外发事件，保证本地和测试环境稳定。
  - 配置为 `rabbitmq` 时自动声明审计事件交换机、队列和路由绑定，并使用 JSON 消息体。
  - 事件发布失败只记录告警，不影响登录和审计日志写库主流程。
- 新增防重复提交：
  - 提供 `@RepeatSubmit` 注解，拦截短时间内同一用户、同一方法、同一参数的重复请求。
  - 用户、角色、菜单、部门、文件上传等写接口已接入。
  - 认证刷新、退出登录和接口 Manifest 发布等高风险写接口已接入。
  - 非公开高风险写接口缺少 `@RepeatSubmit` 时触发 `HIGH_RISK_WRITE_API_WITHOUT_REPEAT_SUBMIT` 错误规则，阻断接口发布门禁。
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
- 为接口资源清单增加导出、权限缺口治理建议、废弃接口下线计划，并在前端按 Vben 选型风格落地接口管理页。
- 为审计日志查询增加前端页面、导出、保留周期和清理策略。
- 为防重复提交增加后台配置页、按接口覆盖默认间隔、Redis 不可用告警。

### 第二优先级：企业支撑能力

- 系统参数管理：把可运营配置从配置文件逐步沉淀到数据库。
- 数据字典：统一维护状态、枚举、选项。
- 文件表：记录文件名、大小、类型、存储路径、上传人、上传时间。
- 文件存储：补充文件元数据表后，将 MinIO 对象名、桶名、上传人、业务归属和访问策略落库。
- 事件中心：后续把审计事件扩展到站内通知、异步任务、WebSocket 推送和告警策略。
- 数据变更记录：关键表变更前后差异可追踪。
- 数据权限：按部门、本人、本部门及子部门等范围控制数据可见性。
- 模块边界：参考 Smart Admin 的基础模块拆分与 Cool Admin Java 的 `core/modules` 思路，把公共底座和业务模块逐步拆清。

### 第三优先级：架构演进

- 按 `auth`、`system`、`support`、`common` 梳理模块边界。
- 后续表结构变更必须新增 Flyway 增量脚本，并在交付说明中明确影响范围和回滚方式。
- 预留 SSO 模块：飞书扫码、飞书 SSO、OAuth2/OIDC 等统一接入。
- 多租户能力仅在业务确认需要后再引入，避免过早复杂化。
- 代码生成、插件化、低代码作为长期增强项，不作为当前主线底座。

## 5. Jenkins 与分支建议

- `develop`：持续集成、测试环境部署、日常开发合入。
- `main`：稳定版本、正式环境部署来源。
- Jenkins 先接 `develop` 做后端测试与构建：
  - `cd server && ./mvnw test`
  - 检查 Flyway 迁移测试通过，确保 `flyway_schema_history` 有迁移记录
  - 登录测试账号后调用 `/api/system/api-resources/governance`，要求 `passed=true` 且 `errorCount=0`
  - 可调用 `/api/system/api-resources/governance/rules` 输出规则目录，作为 Jenkins 报告中 ruleCode 的解释来源
  - Jenkins 应将 `API_METHOD_NOT_EXPLICIT` 视为阻断项，要求所有 `/api/**` 接口使用明确 HTTP 方法，禁止 `ALL` 泛匹配路由
  - Jenkins 应将 `PUBLIC_API_WITHOUT_ACCESS_POLICY` 视为阻断项，要求公开接口补充 `@ApiAccessPolicy(ApiAuthType.PUBLIC, reason = "...")`
  - Jenkins 应将 `PUBLIC_API_NOT_IN_RUNTIME_WHITELIST` 视为阻断项，要求公开接口同步加入 `SaTokenConfig.LOGIN_EXCLUDE_PATH_PATTERNS` 或改回登录态/权限态接口
  - Jenkins 应对 `DEPRECATED_API_MISSING_SUNSET_VERSION` 和 `DEPRECATED_API_MISSING_REPLACEMENT` 输出整改提示，要求废弃接口有明确下线版本和替代接口
  - Jenkins 应将 `REMOVED_API_STILL_MAPPED` 视为阻断项，要求已移除接口不再存在运行时路由
  - Jenkins 应将 `HIGH_RISK_WRITE_API_WITHOUT_REPEAT_SUBMIT` 视为阻断项，要求非公开高风险写接口补充 `@RepeatSubmit` 或降低风险级别并说明理由
  - 可调用 `/api/system/api-resources/export` 导出 CSV 作为构建产物，便于接口清单留档和版本差异比对
  - 可调用 `/api/system/api-resources/manifest` 生成 JSON 与 `checksum`，用于识别接口契约是否发生变更
  - 推荐 Jenkins 在开发/测试环境调用 `/api/system/api-resources/manifest/snapshots/latest` 获取上一版发布快照
  - 可将上一版本 Manifest POST 到 `/api/system/api-resources/manifest/diff`，若 `breakingChangeCount > 0` 则要求人工确认或阻断发布
  - 推荐 Jenkins 使用 `/api/system/api-resources/manifest/gate` 作为最终接口契约发布门禁，要求 `passed=true`
  - Jenkins 报告应优先读取 Manifest Gate 的 `checks`，按 `checkCode`、`passed`、`blocking` 和 `remediation` 输出门禁明细
  - 更推荐 Jenkins 使用 `/api/system/api-resources/manifest/gate/latest` 做发布前干跑门禁，减少流水线自行拼装上一版 Manifest 请求体的复杂度
  - 发布通过后调用 `POST /api/system/api-resources/manifest/snapshots` 落库当前 Manifest，作为下一次发布的对比基线
  - 权限码命名统一遵循 `/api/system/api-resources/governance` 返回的 `permissionCodePattern`
  - `operationId` 命名统一遵循 `/api/system/api-resources/governance` 返回的 `operationIdPattern`，且必须全局唯一
  - 前端后续可接 `pnpm build`
- 合入 `main` 前必须确保后端测试通过、前端构建通过、数据库迁移说明完整。

更多分支说明见 [BRANCHING_AND_JENKINS.md](../BRANCHING_AND_JENKINS.md)。
