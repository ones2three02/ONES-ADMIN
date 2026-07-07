# ONES-ADMIN 后端工程化审计与优化路线

更新时间：2026-07-07
当前分支：develop

## 1. 调研来源

本轮重点参考了以下开源企业后台项目，调研渠道为 agent-reach GitHub/dev 路由、公开 GitHub 页面与网页检索，访问日期为 2026-07-01 至 2026-07-03。

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
| [Kong/kong](https://github.com/Kong/kong) | API/AI Gateway、插件化策略、流量治理，2026-07-02 仍保持高活跃 | 策略目录化、网关能力边界、接口治理与运行时权限策略分层 |
| [frappe/hrms](https://github.com/frappe/hrms) | Open Source HR and Payroll Software，2026-07-02 仍保持更新 | 员工生命周期、薪酬和 HR 流程的一体化建模，提示 ONES-ADMIN 不能只做员工当前状态 CRUD |
| [orangehrm/orangehrm](https://github.com/orangehrm/orangehrm) | 综合 HRM 系统，2026-06-30 仍保持更新 | 员工管理、报表和 HR Administration 需要可追溯的员工事件时间线 |
| [apiman/apiman](https://github.com/apiman/apiman) | Java API Management，可扩展策略插件 | API 管理策略、扩展点和治理边界 |
| [TykTechnologies/tyk](https://github.com/TykTechnologies/tyk) | Go API Gateway，支持 REST、GraphQL、TCP、gRPC | API 网关、鉴权、流量治理思路 |
| [pb33f/openapi-changes](https://github.com/pb33f/openapi-changes) | OpenAPI 破坏性变更检测 CLI | CI/CD 契约变更报告、破坏性变更识别 |
| [oasdiff/oasdiff](https://github.com/oasdiff/oasdiff) | OpenAPI Diff 与 Breaking Changes | Manifest Diff 与发布门禁设计 |
| [OpenAPITools/openapi-diff](https://github.com/OpenAPITools/openapi-diff) | Java OpenAPI 差异比较工具 | Java 生态契约差异检测 |
| [stoplightio/spectral](https://github.com/stoplightio/spectral) | OpenAPI/AsyncAPI 规则化 lint 工具 | 规则编码、检查结果、CI 报告可机器解析 |
| [opticdev/optic](https://github.com/opticdev/optic) | OpenAPI lint、diff、testing | API 设计质量、破坏性变更预防、接口文档准确性 |
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
- Spectral/Optic 的 API lint 思路要求规范以规则编码沉淀，oasdiff 的差异治理要求版本元数据可比较；ONES-ADMIN 的 `sinceVersion`、`sunsetVersion` 必须遵循统一产品版本格式，避免接口目录只有文本而无法进入 Jenkins 审计。
- ONES-ADMIN 当前保持 Spring Boot 3 + Sa-Token + MyBatis-Plus，不跟随 Cool Admin Java 切换 MyBatis-Flex；后续只吸收它的模块边界和初始化治理思想。
- 本轮复核的接口管理开源标杆中，Backstage 适合借鉴 API Catalog 的可发现性与 Owner/Lifecycle 元数据，Gravitee 适合借鉴 API 生命周期与集中管理，Apicurio 适合借鉴 API/Schema Registry，SmartAdmin/CoolAdmin 继续作为后端安全、模块边界和初始化治理参考。
- 2026-07-03 继续复核后，本项目本轮不新增外部 API 网关依赖，先把公开接口的“开放原因”纳入阻断规则，确保登录、健康检查、飞书回调、SSO 回跳等公开入口后续都有可审计依据。

## 2. 当前后端基线

已具备：

- Spring Boot 3.5.x + Java 21。
- Maven Enforcer 已在 `validate` 阶段强制校验 JDK 21+ 和 Maven 3.9.0+，避免 Jenkins 或本机误用 Java 8 时才在测试阶段暴露 class version 报错。
- Sa-Token 登录认证，`/api/**` 默认登录保护。
- MyBatis-Plus 数据访问。
- 用户、角色、菜单、部门基础 RBAC。
- 登录失败次数、临时锁定、最后登录时间，失败次数与锁定时长可配置。
- 登录日志、系统、认证安全、时区与 HRMS 写操作日志已具备后端记录、分页查询和 CSV 导出接口。
- 已建立统一分页模型，审计日志查询支持分页与筛选。
- 已建立运行时接口资源清单，可扫描后端真实 `/api/**` 路由、方法、模块、摘要、权限点和写操作标识。
- 接口资源清单已提供稳定接口标识 `apiKey`、稳定操作标识 `operationId`、处理器定位 `handler` 和废弃状态 `deprecated`，便于接口管理页、API Catalog、客户端生成、审计排障和 Jenkins 报告直接定位到 Controller 方法。
- 接口资源清单已补充接口负责人、引入版本、生命周期、风险级别、计划下线版本和替代接口元数据，便于接口归属、下线计划、风险审计和 Jenkins 报告分组。
- 已提供接口资源 CSV 导出接口，便于审计留档、接口变更对比和 Jenkins 产物归档。
- 已提供接口资源 Manifest，输出稳定 JSON 清单和 `SHA-256` 指纹，便于接口契约归档和版本间变更比对。
- 已提供接口资源 Manifest Diff，可对比上一版本 Manifest 和当前运行时 Manifest，输出新增、删除、修改和破坏性变更统计。
- 已提供接口资源 Manifest 发布门禁，可综合接口治理错误、破坏性接口变更和人工确认原因输出发布准入结果。
- Manifest Gate 已输出机器可读 `checks` 检查项，Jenkins 可按 `checkCode`、`passed`、`blocking` 和 `remediation` 生成稳定门禁报告。
- 已提供接口治理聚合报告，一次性返回 summary、governance、rules、manifest 和 latestGate，并补充负责人、调用方、规则命中和治理分类聚合，便于 Jenkins、接口管理页和人工巡检使用同一份接口治理视图。
- 接口治理聚合报告已补充 `referenceBenchmarks`、`recommendedActions` 和 `actionItems`，将接口管理/API 网关/HRMS 参考基准、下一步建议与可跟踪治理动作结构化输出，便于 Jenkins 和前端直接消费。
- 接口治理聚合报告已补充 `qualityScore` 与 `qualityDimensions`，按 SECURITY、AUDIT、CONTRACT、LIFECYCLE、CATALOG、DOCUMENTATION 六个维度输出治理评分、扣分数量、参考基准和整改建议，便于 Jenkins 设置趋势阈值，也便于前端接口管理页展示维度健康度。
- 接口治理聚合报告已补充 `releaseReadiness`，将 Manifest Gate、基线快照、治理评分和动作项汇总为机器可读发布状态，Jenkins 可直接读取 `ready`、`status`、`blockingCheckCount` 和 `nextActionCode`。
- 接口治理聚合报告已补充 `ownerActionSummaries`，按负责人聚合治理动作总数、打开动作数、阻断动作数、P0/P1/P2 待办、分类和下一步动作，便于 Jenkins 和接口管理页按团队追踪整改负载。
- 接口管理前端已在 v0.0.71 补齐 Manifest 基线视图，直接展示当前 Manifest 指纹、最新归档快照、历史快照列表和发布状态，让 Jenkins 使用的接口契约基线也能被后台页面审阅。
- 审计日志前端已在 v0.0.72 补齐登录日志、操作日志、CSV 导出、保留策略预览和过期日志清理闭环，并通过动态菜单挂载 `/system/audit`。
- 数据字典已在 v0.0.73 完成基础闭环：`sys_dict_type` + `sys_dict_item` 两表模型、类型/字典项 CRUD、登录态选项接口、动态菜单 `/system/dict`、按钮权限和 Vben 风格前端页面均已落地；v0.0.75 已继续接入 HRMS 员工、合同、花名册和概览枚举展示。
- 数据权限已在 v0.0.76 完成第一阶段闭环：角色模型新增 `data_scope`，支持全部数据、本部门及子部门、本部门、本人四类范围，HRMS 员工主数据读接口、合同查询、导出和人力概览已接入后端强约束。
- 敏感字段治理已在 v0.0.77 完成第一阶段闭环：新增通用脱敏工具，HRMS 员工列表和花名册导出默认脱敏手机号、邮箱和证件号，前端编辑抽屉打开时重新拉取详情，避免列表脱敏值被误写回库。v0.0.78 已继续补齐 `hr:employee:sensitive:view` 字段级查看权限，员工详情和写接口响应按权限决定联系方式是否完整展示，无权限编辑时后端保留原手机号和邮箱。v0.0.79 已将花名册导入错误行调整为脱敏快照，避免错误报告旁路泄露联系方式。v0.0.80 已让合同附件引用进入文件元数据和业务归属校验，避免附件 ID 成为不可追踪的裸引用。v0.0.81 已增加文件软删除和绑定保护，避免已引用附件被误删。v0.0.82 已补齐系统文件资产列表和前端文件管理页，文件元数据进入可运营状态。v0.0.83 已把合同附件上传、查看和移除接入 HRMS 合同业务页面，避免文件能力停留在系统后台而无法支撑业务闭环。v0.0.84 已把文件下载纳入业务归属二次授权，合同附件下载必须同时满足合同权限和员工数据范围。v0.0.85 已补齐上传人临时文件元数据和下载访问，避免合同保存前的未绑定附件被横向访问。v0.0.86 已补齐过期已删除文件物理清理闭环，避免软删文件长期占用本地或 MinIO 存储。v0.0.90 已将员工资料附件纳入同一文件治理链路，员工资料文件必须绑定 `HR_EMPLOYEE_DOCUMENT/{employeeId}` 并通过员工详情权限和数据范围校验。
- 接口管理参考基准已覆盖 Backstage 的 API Catalog、Gravitee 的 API Management 生命周期、Kong/APISIX/Tyk 的网关策略、Spectral 的 OpenAPI 规则校验和 oasdiff 的破坏性变更识别；ONES-ADMIN 当前优先吸收“目录资产化、策略门禁化、契约可比对、整改动作可跟踪”四类能力。
- 已将 `operationId` 纳入接口清单、CSV、Manifest、Manifest 指纹和 Manifest Diff，接口操作标识变化按破坏性变更处理。
- 已建立 `operationId` 命名规范和唯一性门禁，当前格式为 `^[A-Z][A-Za-z0-9]*_[a-z][A-Za-z0-9]*$`，重复或非法命名会作为治理错误阻断发布。
- 已建立显式 HTTP 方法治理，`/api/**` 接口不得使用未指定 method 的 `ALL` 泛匹配路由，避免接口契约、网关路由和客户端生成不稳定。
- 已建立接口版本元数据格式治理，当前格式为 `^v\d+\.\d+\.\d+$`，`sinceVersion` 和废弃接口 `sunsetVersion` 不规范时输出治理警告，便于 Jenkins 和接口管理页追踪版本计划。
- 已建立接口治理质量门禁，可输出权限缺口、系统写接口无权限、接口文档元数据缺失等违规项。
- 已建立接口治理规则目录，结构化输出规则编码、严重级别、是否阻断、分类和修复建议，便于 Jenkins、接口管理页和人工巡检复用同一套规则解释。
- 已建立公开接口访问策略治理，`PUBLIC` 接口必须显式声明 `@ApiAccessPolicy(ApiAuthType.PUBLIC, reason = "...")`、填写开放原因并同步加入 Sa-Token 运行时白名单，否则作为安全错误阻断发布。
- 已建立废弃接口下线治理，废弃 API 必须维护 `sunsetVersion` 和 `replacementApiKey`，避免只有废弃标记但没有迁移路径。
- 已建立接口生命周期一致性治理，`DEPRECATED` 必须同步 OpenAPI 废弃标记，`REMOVED` 不允许继续暴露运行时路由。
- 已建立高风险写接口策略一致性治理，接口清单、CSV 和 Manifest 输出 `repeatSubmitProtected`，非公开高风险写接口缺少 `@RepeatSubmit` 时阻断发布。
- 已建立写接口操作审计覆盖治理，接口清单、CSV、Manifest 和 Manifest Diff 输出 `operationAuditProtected`，非公开写接口缺少操作审计覆盖时通过 `WRITE_API_WITHOUT_OPERATION_AUDIT` 阻断发布。
- 已建立接口权限注册一致性检查，可识别代码权限点是否写入 `sys_permission`，以及是否挂载到 `sys_menu.auth_code` 供角色授权。
- 已建立权限码命名规范检查，当前统一使用小写冒号分段格式：`^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*){1,3}$`。
- 已建立模块化错误码体系，通用、认证、系统模块错误码分层维护，并通过测试校验跨模块错误码唯一性。
- 已接入 Flyway 数据库迁移治理，基线表结构位于 `server/src/main/resources/db/migration/V1__init_schema.sql`。
- 系统数据字典迁移位于 `server/src/main/resources/db/migration/V4__create_system_dictionary.sql`，只新增 `sys_dict_type`、`sys_dict_item` 和相关索引；如需回滚，必须先确认无生产字典数据依赖，再按审批流程删除这两张表和对应菜单权限数据。
- 已补充数据库迁移质量门禁，校验脚本命名、版本连续性、旧 `schema.sql` 禁用和破坏性 SQL 审批标记。
- 高风险写接口已接入防重复提交保护，正式环境默认使用 Redis 存储防重票据；Sa-Token 登录态正式默认 Redis，可通过显式配置在本地 E2E 切换内存 DAO；登录接口继续由失败次数和临时锁定策略保护。
- 文件存储已抽象为 `FileStorageService`，默认本地目录存储，可通过配置切换到 MinIO。
- v0.0.80 已补齐 `sys_file` 文件元数据基础闭环，上传成功后记录原始文件名、存储对象名、URL、类型、大小、存储类型、桶名、上传人和业务归属，并提供受 `system:file:read` 控制的元数据详情接口。
- v0.0.81 已补齐文件状态和软删除保护，文件删除只更新元数据状态，已绑定业务归属的文件禁止删除。
- v0.0.82 已补齐文件资产分页查询接口和 Vben 文件管理页，支持按文件名、状态、存储类型和业务归属筛选，并通过菜单权限树进行授权。
- v0.0.83 已补齐 HRMS 合同附件前端闭环：合同抽屉支持上传、替换、查看、移除附件，合同历史列表支持附件列和快捷查看动作；后端继续复用文件元数据、业务归属绑定、软删除保护和 `system:file:*` 权限边界。
- v0.0.84 已新增文件下载业务访问策略和 HRMS 合同附件元数据业务接口，未绑定文件继续由 `system:file:read` 控制，HRMS 合同附件下载和元数据查看由 `hr:contract:list` 与员工数据范围共同控制，避免文件 URL 脱离业务权限。
- v0.0.85 已将系统文件元数据详情和下载统一纳入 `FileAccessService`，上传人可访问自己的未绑定临时文件，其他普通用户不可横向访问，系统文件管理员仍可治理全局文件资产。
- v0.0.86 已补齐文件保留策略预览和过期物理清理能力，已删除文件默认保留 30 天，可通过 `ONES_FILE_DELETED_RETENTION_DAYS` 配置；清理后元数据进入 `PURGED` 状态，本地存储和 MinIO 存储均支持物理删除。
- v0.0.90 已补齐员工资料附件业务策略，`HR_EMPLOYEE_DOCUMENT` 文件下载和元数据查询由员工档案接口承接，下载前统一校验 `hr:employee:detail` 权限和员工数据范围，绑定前还会校验用户对未绑定临时文件的访问权。
- v0.0.91 已将员工资料附件的 HR 业务元数据拆入 `hr_employee_document`，资料类型和有效期由 HRMS 管理，`sys_file` 继续只负责文件物理和业务归属边界，避免系统文件表被业务字段污染。
- v0.0.92 已补齐员工资料到期预警，跨员工预警接口继续执行 `hr:employee:detail` 权限、员工数据范围和文件 ACTIVE 状态过滤，并将 30 天内到期资料、已过期资料接入 HRMS 人力概览。
- v0.0.93 已补齐员工资料预警前端运营页和动态菜单入口，复用 `hr:employee:detail` 权限，不新增数据库迁移和后端接口；动态菜单测试已覆盖 `/hr/document-warning` 路由，避免前端静态路由与后端授权树漂移。
- v0.0.94 已将 HRMS 合同附件、员工资料附件和资料预警页的附件查看统一收口到前端受控 Blob 下载链路；前端通过 Axios 携带 `Authorization` 请求头访问 `/api/system/files/{storedName}`，后端继续通过 `FileAccessService` 与 HRMS 业务策略执行二次授权，避免直接打开存储 URL 时丢失 Sa-Token Header。
- v0.0.95 已将系统文件下载纳入操作审计，成功、越权失败、元数据缺失、物理对象缺失和读取失败都会写入 `sys_operation_log`，记录 TraceId、文件 ID、业务归属、权限语义、成功状态和响应码，避免敏感附件只有授权没有访问留痕。
- v0.0.96 已将操作日志查询和导出补齐到模块、操作名称、权限码维度；审计人员可用 `module`、`operation`、`permissionCode` 在页面和 CSV 留档中定位文件访问、HRMS 写操作和系统管理动作，本版本不新增数据库迁移。
- v0.0.97 已在操作日志查询和导出中加入异常操作视角，`abnormalOnly=true` 会聚焦 `success=false` 或响应码非 `0` 的记录，让越权、文件缺失、业务失败等风险事件可快速检索并留档，本版本不新增数据库迁移。
- v0.0.98 已补齐 HRMS 员工资料到期预警 CSV 导出，导出接口复用 `hr:employee:detail` 权限、员工数据范围、到期窗口和 ACTIVE 文件过滤，并使用 `CsvExportUtils` 防公式注入，让资料合规风险可从页面运营延伸到离线留档。
- v0.0.99 已补齐 HRMS 员工合同到期预警 CSV 导出，导出接口复用 `hr:contract:list` 权限、合同到期窗口校验和员工数据范围过滤，并使用 `CsvExportUtils` 防公式注入，让合同续签风险与资料合规风险具备一致的页面运营和离线留档闭环。
- v0.0.100 已将合同到期预警前端窗口扩展为 7/30/60/90 天，并修复员工姓名字段与后端 `realName` 响应不一致的问题；本版本不新增接口，重点收敛合同预警与资料预警的运营体验和导出口径。
- v0.0.101 已补齐合同到期预警运营指标和风险状态列，前端直接复用现有列表结果计算预警总数、7 天内到期和当前窗口，不扩大后端接口面即可提升 HR 续签风险识别效率。
- v0.0.102 已抽取 HRMS 前端预警共用工具，统一合同预警与资料预警的到期天数计算、预警窗口表单、风险状态 Tag 和 7 天内到期指标口径；本版本不新增接口，重点降低后续 HRMS 预警页面的重复实现和口径漂移风险。
- v0.0.103 已将 HRMS 人力概览页硬编码文案迁移到 `hr.overview` 中英文语言资源，并让更新时间格式跟随当前前端语言偏好，降低国际化遗漏和页面文案分散维护风险。
- v0.0.104 已将 HRMS 合同管理页员工搜索、员工卡片、合同历史标题和合同终止表单硬编码文案迁移到中英文语言资源，继续压缩 HRMS 页面内散落文案，保持后台风格不变。
- v0.0.105 已将 HRMS 花名册导入页指标、最近批次、CSV 上传区、模板下载、文件校验、上传结果和错误明细表文案迁移到中英文语言资源，让批量导入这种高频运营入口也纳入统一国际化治理。
- v0.0.106 已将 HRMS 岗位管理与职级管理状态切换确认、启用/禁用动作和成功提示收敛到 `hr.statusChange` 中英文语言资源，优先治理基础资料同构交互，减少后续页面复制硬编码。
- v0.0.107 已将 HRMS 员工档案抽屉标题、摘要卡、基础信息、合同、资料附件、任职记录、组织关系、生命周期和资料附件元信息弹窗文案迁移到 `hr.employeeProfile` 中英文语言资源，优先治理未来员工详情扩展的核心容器。
- v0.0.108 已将 HRMS 员工列表导出、部门搜索、档案操作、生命周期抽屉和调岗/转正/离职表单标题与字段迁移到 `hr.employeeList` 与 `hr.employeeLifecycle` 中英文语言资源，让员工主数据高频运营入口与员工档案抽屉保持统一多语言维护方式。
- v0.0.109 已将 HRMS 员工、合同、花名册和资料附件共用的字典兜底枚举项迁移到 `hr.dictFallback` 中英文语言资源，并让通用字典 fallback 支持动态工厂函数，避免接口字典不可用时出现固定中文或语言切换缓存残留。
- v0.0.110 已治理 HRMS 前端 API 响应边界和抽屉表单提交类型，将岗位、职级、员工、合同和花名册常用接口从 `any` 强转收敛为远端响应类型、normalize 函数和 DTO 泛型提交，减少前后端契约漂移风险。
- v0.0.111 已治理 HRMS 员工列表部门树、合同管理员工列表和员工生命周期抽屉的加载失败提示，统一使用 Ant Design Vue `message.error` 与中英文语言资源，避免失败只停留在控制台。
- v0.0.112 已抽取 HRMS 前端共享错误工具，合同、员工、员工档案、资料预警和花名册导入复用同一套错误消息归一化函数，降低后续业务页复制成本。
- v0.0.113 已抽取 HRMS 前端共享文件工具，统一文件大小展示和 Blob 下载封装，让合同附件、员工资料附件和花名册模板下载复用同一工程入口。
- v0.0.114 已统一 HRMS CSV 导出下载入口，员工花名册、合同到期预警和资料到期预警导出均通过 `downloadHrBlob` 调用，避免业务页直接依赖底层下载工具。
- v0.0.115 已治理 HRMS 上传与附件预览错误提示入口，花名册模板下载、上传校验、合同附件预览和员工资料上传失败分支统一复用共享错误工具，减少业务页直接读取 `error.message` 或固定兜底文案。
- v0.0.116 已统一 HRMS 受控附件打开入口，合同列表、合同抽屉、员工档案和资料预警页均通过 `openHrFile` 校验存储标识并调用系统受控 Blob 打开链路，页面只保留业务文案和提示。
- v0.0.117 已统一 HRMS 受控附件打开提示模式，`openHrFileWithFeedback` 统一处理文件不可用提示和打开失败提示，减少业务页重复 `try/catch`。
- v0.0.118 已统一 HRMS 受控附件打开 loading 反馈，合同附件、员工资料附件和资料预警附件打开期间通过 Ant Design Vue `message.loading` 提供明确处理中状态。
- v0.0.119 已补齐 HRMS 员工资料附件删除失败反馈，删除接口异常时通过 `errorMessageOf` 展示业务化错误提示，并保持确认弹窗异步失败语义。
- v0.0.120 已治理 HRMS 岗位/职级启停失败反馈，用户取消与接口失败分支分离，接口异常时通过 `errorMessageOf` 展示业务化错误提示，避免空 `catch` 吞掉真实失败。
- v0.0.121 已抽取 HRMS 状态切换确认共享工具，`confirmHrStatusChange` 和 `isHrStatusChangeCancelled` 统一承接岗位/职级启停确认与取消判断，减少同构页面复制。
- v0.0.122 已抽取 HRMS Blob 下载反馈共享工具，员工花名册导出、合同预警导出、资料预警导出和花名册模板下载统一复用 loading、成功和失败提示入口。
- v0.0.123 已抽取 HRMS 上传反馈共享工具，合同附件上传、员工资料附件上传和花名册导入上传统一复用 loading、成功提示、错误归一化和 Upload 回调入口。
- v0.0.124 已抽取 HRMS 状态切换执行反馈共享工具，岗位和职级启停统一复用确认后执行、成功提示、取消静默和失败提示入口。
- v0.0.125 已统一 HRMS Upload 文件解析与前置失败反馈，合同附件、员工资料附件和花名册导入统一复用 `File` 校验、业务错误提示与 Upload `onError` 触发入口。
- v0.0.126 已补齐 HRMS Upload 共享工具 Vitest 单元测试，并治理根级 `pnpm test:unit` 入口边界、Vue 单测插件、资源加载测试和 Sortable mock 位置，让前端单元测试恢复为可回归验证门禁。
- v0.0.127 已补齐 `@vben/playground` 包级 `test:unit` 脚本，Jenkins 可按包单独运行 HRMS 前端源码单测，根级单测和 Playwright E2E 边界更清晰。
- v0.0.128 已将 Playground 包级 `test:unit` 从单文件路径升级为 `playground/src` 目录入口，后续新增 HRMS/Vben 页面源码单测可自动纳入包级门禁。
- v0.0.129 已新增根目录 `Jenkinsfile`，将前端单测、类型检查、后端测试、前端构建、版本残留扫描和敏感信息扫描固化为 GitHub 关联 Jenkins 的验证门禁基线，当前阶段不包含部署动作。
- v0.0.130 已新增 CI 版本一致性门禁，动态校验 `VERSION`、README、CHANGELOG、Maven 版本、后端默认版本和接口治理测试断言，降低后续小版本发布时版本号漂移风险。
- v0.0.131 已新增 `scripts/ci/verify.sh`，统一 Jenkins 与本地验证阶段入口，避免 CI 命令散落在 Jenkinsfile、README 和人工执行说明中。
- v0.0.132 已新增 CI 构建元数据归档，Jenkins 会保存产品版本、Git 提交、分支、构建时间和工具链版本，便于后续交付包、测试报告和源码提交精确追溯。
- v0.0.133 已新增 CI 验证摘要归档，Jenkins 会保存门禁结果、后端测试报告统计和前端构建产物状态，便于后续质量趋势、发布审计和排障使用同一份机器可读摘要。
- v0.0.134 已新增 CI 接口治理报告归档，Jenkins 会保存真实接口治理聚合报告、质量分、发布准备度、Manifest 指纹和治理动作项，让 API 治理结果进入发布审计链。
- v0.0.135 已新增 CI 数据库迁移审计报告归档，Jenkins 会保存 Flyway 迁移版本连续性、命名规范、旧 `schema.sql` 禁用、破坏性 SQL 审批和迁移脚本指纹状态，让数据库变更治理进入发布审计链。
- v0.0.136 已新增 CI 发布证据包归档，Jenkins 会把构建来源、数据库迁移治理、接口治理、测试与前端构建状态汇总成 `.ci-artifacts/release-evidence.json`，用于测试交付、人工审批和后续回滚评审。
- 登录日志、操作日志落库后可发布统一系统事件，默认关闭外发，可通过配置切换到 RabbitMQ。
- 系统、认证令牌刷新/退出登录、时区设置与 HRMS 写接口已纳入统一操作审计，员工、岗位、职级、合同和花名册导入等写操作可通过 TraceId 在操作日志中追踪；业务生命周期事件只记录业务状态流转，不替代操作日志。
- HRMS 已完成一期企业级设计稿，并在 v0.0.51 开始落地 Phase 1A：新增 `hr_` 基础表迁移、岗位/职级/员工基础接口、独立 HR 错误码、Swagger 分组、权限初始化和授权树挂载。v0.0.52 已继续落地员工调岗、转正、离职生命周期动作，v0.0.54 已补齐员工生命周期时间线查询接口，v0.0.55 已补齐员工合同列表、新增、编辑接口，v0.0.56 已补齐合同终止和即将到期合同查询，v0.0.58 已开始落地花名册模板下载、批量导入、批次和错误行查询，v0.0.69 已补齐员工花名册 CSV 导出、脱敏证件号输出、`hr:employee:export` 权限和接口治理元数据，v0.0.70 已补齐人力概览统计、`hr:overview:view` 权限和 Vben 概览页，v0.0.87 已补齐员工档案抽屉，v0.0.88 已补齐员工任职记录/岗位历史查询和档案分栏，v0.0.89 已补齐员工组织关系查询和档案分栏，v0.0.90 已补齐员工资料附件查询、绑定、解绑和档案分栏，v0.0.91 已补齐资料类型和有效期治理，v0.0.92 已补齐资料到期预警和概览指标，v0.0.93 已补齐资料预警前端运营页，v0.0.94 已统一 HRMS 附件查看的受控下载链路，v0.0.95 已补齐敏感文件下载审计，v0.0.96 已补齐审计日志模块/操作/权限码筛选，v0.0.97 已补齐异常操作审计视角，v0.0.98 已补齐资料预警导出留档，v0.0.99 已补齐合同预警导出留档，v0.0.100 已统一合同预警窗口筛选体验，v0.0.101 已补齐合同预警运营指标，v0.0.102 已抽取 HRMS 预警前端共用工具，v0.0.103 已治理人力概览页国际化文案，v0.0.104 已治理合同管理页国际化文案，v0.0.105 已治理花名册导入页国际化文案，v0.0.106 已治理岗位/职级状态切换国际化文案，v0.0.107 已治理员工档案抽屉国际化文案，v0.0.108 已治理员工列表与生命周期操作国际化文案，v0.0.109 已治理 HRMS 字典兜底枚举国际化文案，v0.0.110 已治理 HRMS 前端 API 和表单类型契约，v0.0.111 已治理 HRMS 前端加载失败提示，v0.0.112 已抽取 HRMS 前端错误归一化共享工具，v0.0.113 已抽取 HRMS 前端文件展示与下载共享工具，v0.0.114 已统一 HRMS CSV 导出下载入口，v0.0.115 已治理 HRMS 上传与附件预览错误提示入口，v0.0.116 已统一 HRMS 受控附件打开入口，v0.0.117 已统一 HRMS 受控附件打开提示模式，v0.0.118 已统一 HRMS 受控附件打开 loading 反馈，v0.0.119 已补齐员工资料附件删除失败反馈，v0.0.120 已治理岗位/职级启停失败反馈，v0.0.121 已抽取 HRMS 状态切换确认共享工具，v0.0.122 已抽取 HRMS Blob 下载反馈共享工具，v0.0.123 已抽取 HRMS 上传反馈共享工具，v0.0.124 已抽取 HRMS 状态切换执行反馈共享工具，v0.0.125 已统一 HRMS Upload 文件解析与前置失败反馈，v0.0.126 已补齐 HRMS Upload 共享工具单元测试并恢复前端单测门禁，v0.0.127 已补齐 Playground 包级单测脚本，v0.0.128 已将包级单测升级为目录入口，v0.0.129 已新增 Jenkins 验证门禁基线，v0.0.130 已新增 CI 版本一致性门禁，v0.0.131 已统一本地与 Jenkins 验证入口，v0.0.132 已新增 CI 构建元数据归档，v0.0.133 已新增 CI 验证摘要归档，v0.0.134 已新增 CI 接口治理报告归档，v0.0.135 已新增 CI 数据库迁移审计报告归档，v0.0.136 已新增 CI 发布证据包归档，形成“员工主数据 + 组织关系 + 任职记录 + 资料附件 + 生命周期 + 合同流程 + 批量导入导出 + 概览分析 + 员工档案 + 权限治理 + 文件访问治理 + 审计检索 + 异常运营 + 合规留档 + 预警前端工程化复用 + HRMS 前端国际化治理 + 类型契约治理 + 可见错误提示 + 错误归一化复用 + 文件工具复用 + 导出下载入口统一 + 上传与附件预览错误治理 + 受控附件打开入口统一 + 受控附件打开 loading 反馈 + 资料附件删除反馈闭环 + 岗位/职级启停失败反馈 + 状态切换确认复用 + Blob 下载反馈复用 + 上传反馈复用 + 状态切换执行反馈复用 + Upload 文件解析复用 + Upload 工具测试覆盖 + 前端单测门禁恢复 + 包级单测脚本分层 + Playground 单测目录入口 + Jenkins 验证门禁基线 + CI 版本一致性门禁 + 本地/Jenkins 统一验证入口 + CI 构建元数据归档 + CI 验证摘要归档 + CI 接口治理报告归档 + CI 数据库迁移审计报告归档 + CI 发布证据包归档”的基础闭环。
- MySQL、Redis、RabbitMQ、MinIO 配置通过环境变量注入，本地敏感配置不提交 Git。

主要缺口：

- 错误码模块化已起步，后续需要继续细分第三方登录、数据权限、系统参数等新模块号段，并在字典能力扩展时保持号段连续可审计。
- 数据字典基础 CRUD 与 HRMS 员工/合同/资料附件表单接入已落地，后续需要补缓存、更多业务表单接入、字典变更审计、导入导出和多租户隔离；系统参数、多租户仍待规划，文件元数据、文件管理页、文件下载业务归属授权、上传人临时文件访问、过期物理清理和员工资料附件业务策略已完成基础闭环，但仍需生命周期归档、定时清理任务、在线预览、资料归档和更细的文件分享策略，数据权限和敏感字段脱敏已完成第一阶段落地，后续需要扩展到更多业务域、自定义部门集合、字段级权限和可配置脱敏策略。
- HRMS 当前已形成员工主数据、组织关系、任职记录/岗位历史、资料附件、生命周期、合同、合同附件前端上传/查看/移除与下载授权、花名册导入导出、人力概览、员工档案抽屉和 Vben 页面基础闭环，后续需要继续补在线预览能力、更深入的 HRMS 报表分析和员工档案薪酬、绩效等更多业务分栏。
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
  - 提供 `/api/system/api-resources/governance` 接口治理质量门禁，返回是否通过、错误数、警告数、权限码正则、`operationId` 正则、接口版本正则和违规明细。
  - 提供 `/api/system/api-resources/governance/rules` 接口治理规则目录，返回规则编码、严重级别、是否阻断、规则分类、说明和修复建议。
  - 提供 `/api/system/api-resources/governance/report` 接口治理聚合报告，返回应用版本、生成时间、发布就绪摘要、治理汇总、治理结果、规则目录、Manifest、最新快照门禁干跑结果、参考基准、推荐动作和可跟踪动作项。
  - 聚合报告的 `qualityScore` 与 `qualityDimensions` 会把接口治理结果转成 0-100 分维度评分，覆盖安全、审计、契约、生命周期、目录和文档质量，便于 Jenkins 做趋势阈值和前端展示治理健康度。
  - 聚合报告的 `releaseReadiness` 会把 Manifest Gate、基线快照、治理评分和动作项归并成 READY、READY_WITH_WARNINGS、BASELINE_REQUIRED、MANUAL_REVIEW_REQUIRED、BLOCKED、UNKNOWN 等状态。
  - 聚合报告的 `actionItems` 会把治理规则违规、Manifest 基线缺失、Diff 归档和 HRMS 页面接入等事项转成包含优先级、来源、负责人、模块、接口、阻断状态、状态和验证方式的结构化动作，便于 Jenkins、接口管理页和人工巡检共用同一份待办。
  - 自动区分 `PUBLIC`、`LOGIN`、`PERMISSION` 三类接口认证级别。
  - 提供 `@ApiAccessPolicy` 显式标记登录态和公开接口的设计意图，减少安全巡检误报。
  - 登录入口和健康检查已补充公开访问策略说明，后续飞书扫码、飞书 SSO 回调等公开入口必须复用同一治理口径。
  - 对仅登录保护、且没有显式访问策略的系统接口标记权限缺口，便于安全巡检。
  - 对接口未显式声明 HTTP 方法、公开接口缺少显式访问策略、公开接口未同步运行时白名单、系统写接口无权限点、非公开高风险写接口缺少重复提交防护、非公开写接口缺少操作审计覆盖、权限码命名不规范、`operationId` 命名不规范、`operationId` 重复、权限点未注册、权限点不可授权、废弃接口、废弃接口缺少下线版本、废弃接口下线版本格式不规范、废弃接口缺少替代接口、`DEPRECATED` 未同步 OpenAPI 废弃标记、`REMOVED` 接口仍暴露路由、缺少接口负责人、缺少引入版本、接口引入版本格式不规范、缺少生命周期、缺少风险级别、缺少 OpenAPI 模块标签、缺少接口摘要等问题输出结构化治理结果，便于 Jenkins 自动巡检。
  - 接口资源列表、CSV 导出、Manifest 和 Manifest Diff 已纳入 `repeatSubmitProtected`，重复提交防护策略变化会进入 Manifest 指纹和发布差异。
  - 接口资源列表、CSV 导出、Manifest 和 Manifest Diff 已纳入 `operationAuditProtected`，操作审计覆盖变化会进入 Manifest 指纹和发布差异。
  - 每条接口治理违规项均返回 `remediation` 修复建议，便于 Jenkins 输出可执行治理动作，也便于后续接口管理页直接展示整改指引。
  - 新增权限点 `system:api:list` 和 `system:api:publish`，超级管理员启动时自动补齐授权，接口查询与接口发布分权治理。
  - 新增文件上传权限点 `system:file:upload`，文件上传不再只是登录态即可访问。
  - 接口管理已按 Vben 风格落地前端页面，并通过动态菜单挂载 `/system/api-resources`，`system:api:list` 与 `system:api:publish` 按页面按钮权限分权治理。
  - 审计日志已按 Vben 风格落地前端页面，并通过动态菜单挂载 `/system/audit`，`system:audit:login-log`、`system:audit:operation-log` 与 `system:audit:retention` 按页面按钮权限分权治理；文件上传等暂未落前端页面的权限继续以菜单 button 节点挂入授权树。
- 新增系统数据字典：
  - 提供 `/api/system/dicts/types` 字典类型分页查询、新增、编辑、删除接口。
  - 提供 `/api/system/dicts/items` 字典项分页查询、新增、编辑、删除接口。
  - 提供 `/api/system/dicts/{dictCode}/options` 登录态选项接口，供 HRMS 和系统表单按字典编码复用启用项。
  - 删除字典类型前校验是否存在字典项，避免误删业务表单仍依赖的选项集合。
  - 初始化 `hr_employment_type`、`hr_employment_status`、`hr_contract_type` 三组 HRMS 基础字典，为后续员工、合同表单去硬编码打底。
  - 前端已按 Vben / Ant Design Vue 风格落地 `/system/dict` 页面，采用类型表格和字典项表格联动管理，新增/编辑使用项目既有抽屉表单模式。
  - HRMS 员工新增、员工列表筛选、员工列表展示、合同新增/编辑和合同列表展示已消费字典 options，合同类型字典值与后端业务校验统一为 `FIXED_TERM`、`OPEN_ENDED`、`INTERNSHIP`、`SERVICE`。
  - 相关接口纳入 Sa-Token 权限、`@RepeatSubmit`、接口治理元数据、操作审计和集成测试。
- 主要 Controller 补充 `@Tag`、`@Operation`，接口管理更清晰。
- 文件上传改为配置化治理：
  - 上传目录、公开访问前缀、最大文件大小、允许后缀均可配置。
  - 阻止空文件、超限文件和不允许的扩展名。
  - 下载时补充响应 Content-Type。
  - 存储实现通过 `ones.file.storage-type` 切换，当前支持 `local` 和 `minio`，便于开发、本地测试和对象存储部署共用同一接口。
- 新增登录日志：
  - 记录用户名、用户 ID、成功/失败、失败原因、IP、User-Agent、TraceId、时间。
  - 提供 `/api/system/audit/login-logs` 查询接口。
  - 提供 `/api/system/audit/login-logs/export` CSV 导出接口，沿用查询筛选条件和审计权限。
- 新增系统操作日志：
  - 自动记录 `/api/system/**`、`/api/hr/**`、`/api/timezone/**` 下 POST、PUT、PATCH、DELETE 写操作，以及 `/api/auth/refresh`、`/api/auth/logout` 认证安全写操作。
  - 记录用户 ID、请求方法、路径、模块、动作、权限点、业务响应码、结果、TraceId、耗时。
  - 提供 `/api/system/audit/operation-logs` 查询接口。
  - 提供 `/api/system/audit/operation-logs/export` CSV 导出接口，沿用查询筛选条件和审计权限。
- 新增审计日志保留策略：
  - 登录日志和操作日志默认保留 180 天，可分别通过 `ONES_AUDIT_LOGIN_LOG_RETENTION_DAYS` 与 `ONES_AUDIT_OPERATION_LOG_RETENTION_DAYS` 覆盖。
  - 提供 `/api/system/audit/retention` 查询保留策略、过期阈值和待清理数量。
  - 提供 `/api/system/audit/retention/cleanup` 清理过期登录日志与操作日志，使用独立权限 `system:audit:retention`，并接入防重复提交和操作审计。
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
- 新增 Maven Enforcer 构建运行时门禁：
  - `validate` 阶段要求 JDK 21 或更高版本。
  - `validate` 阶段要求 Maven 3.9.0 或更高版本。
  - Jenkins 和本地开发建议优先使用项目内 `./mvnw`，并显式配置 `JAVA_HOME` 指向 JDK 21。
- 新增接口治理负责人待办摘要：
  - `/api/system/api-resources/governance/report` 输出 `ownerActionSummaries`，把治理动作按负责人聚合成团队工作台视图。
  - 每个负责人摘要包含状态、优先级、打开动作数、阻断动作数、P0/P1/P2 分布、分类、下一步动作和整改建议。
  - 该能力借鉴 Backstage API Catalog 的 Owner 责任制，避免接口治理只停留在规则明细，便于 Jenkins、接口管理页和人工巡检按团队推进整改。
- 新增角色数据范围基础治理：
  - `sys_role` 新增 `data_scope` 字段，超级管理员默认为全部数据，普通角色默认本部门及子部门。
  - 后端统一计算当前用户数据范围上下文，支持 `ALL`、`DEPT_AND_CHILD`、`DEPT`、`SELF` 四类口径。
  - HRMS 员工列表、员工详情、生命周期查询、花名册导出、员工合同查询、即将到期合同和人力概览已接入数据范围过滤，避免只依赖前端隐藏造成越权读取。
  - 角色管理接口和前端角色页已支持数据范围读写与展示，为后续请假、考勤、薪资等 HRMS 模块复用打底。
- 新增 HRMS 敏感字段脱敏基础治理：
  - 通用 `SensitiveDataMaskingUtils` 提供手机号、邮箱、证件号和通用文本脱敏方法，避免脱敏规则散落在业务代码中。
  - HRMS 员工列表和员工花名册 CSV 导出默认返回脱敏手机号、邮箱和证件号，降低批量浏览与导出的敏感数据暴露面。
  - HRMS 员工详情、创建、编辑、调岗、转正和离职响应按 `hr:employee:sensitive:view` 权限决定手机号、邮箱是否完整展示，并通过 `sensitiveVisible` 给前端提供明确状态。
  - 无敏感查看权限的用户编辑员工时，前端禁用手机号、邮箱字段，后端也保留原联系方式，避免脱敏值被误提交回数据库。
  - HRMS 花名册导入失败行 `rawJson` 保存脱敏行快照，错误行查询和前端弹窗不再展示原始手机号、邮箱。
  - 本轮对标 Smart Admin、RuoYi-Vue-Plus、JeecgBoot 的数据脱敏基础能力；ONES-ADMIN 当前先采用轻量工具层、字段权限和导入旁路治理落地，后续再演进为注解化和动态脱敏策略。

## 4. 后续优化路线

### 第一优先级：接口治理与安全

- 扩展错误码目录：认证、权限、用户、角色、菜单、文件、系统等模块单独枚举。
- 将统一分页模型逐步推广到用户、角色、菜单、部门、文件等列表接口，并规范分页接口命名。
- 将接口管理页继续扩展到 Manifest 快照、Diff、Gate 检查项和规则目录视图，让前端能完整承接 Jenkins/人工巡检的接口治理闭环。
- 为文件上传、系统参数等基础能力继续补齐前端页面，并将数据字典继续接入 HRMS 花名册、概览筛选和更多系统表单，延续动态菜单和按钮权限治理口径。
- 为防重复提交增加后台配置页、按接口覆盖默认间隔、Redis 不可用告警。

### 第二优先级：企业支撑能力

- 系统参数管理：把可运营配置从配置文件逐步沉淀到数据库。
- 数据字典：基础 CRUD 与 HRMS 员工/合同表单接入已落地，后续补缓存、更多业务接入、变更审计、导入导出和租户隔离。
- 文件表：已记录文件名、大小、类型、存储对象名、上传人、上传时间、业务归属、状态和删除时间，后续补充文件访问策略、物理清理任务和生命周期归档。
- 文件存储：已将 MinIO 对象名、桶名、上传人、业务归属和软删除状态落库，并补齐前端文件管理页，后续继续补文件级访问策略、物理清理任务和生命周期归档。
- 事件中心：后续把审计事件扩展到站内通知、异步任务、WebSocket 推送和告警策略。
- 数据变更记录：关键表变更前后差异可追踪。
- 数据权限：第一阶段已按角色支持全部数据、本部门及子部门、本部门、本人四类范围，后续扩展自定义部门集合、字段级权限和跨业务域复用。
- 敏感字段脱敏：第一阶段已覆盖 HRMS 员工列表、导出和员工详情字段级查看权限，后续扩展到合同附件、导入错误报告、审计日志和可配置脱敏策略。
- 模块边界：参考 Smart Admin 的基础模块拆分与 Cool Admin Java 的 `core/modules` 思路，把公共底座和业务模块逐步拆清。
- HRMS 一期按 [HRMS 一期企业级设计方案](hrms-phase-one-design.md) 推进，优先落地员工主数据、任职历史、生命周期事件、合同与花名册导入导出。

### 第三优先级：架构演进

- 按 `auth`、`system`、`support`、`common` 梳理模块边界。
- 后续表结构变更必须新增 Flyway 增量脚本，并在交付说明中明确影响范围和回滚方式。
- 预留 SSO 模块：飞书扫码、飞书 SSO、OAuth2/OIDC 等统一接入。
- 多租户能力仅在业务确认需要后再引入，避免过早复杂化。
- 代码生成、插件化、低代码作为长期增强项，不作为当前主线底座。

## 5. Jenkins 与分支建议

- `develop`：持续集成、测试环境部署、日常开发合入。
- `main`：稳定版本、正式环境部署来源。
- Jenkins 先接 `develop` 做验证门禁，根目录 `Jenkinsfile` 已在 v0.0.129 固化当前阶段的测试、类型检查、构建和仓库扫描，在 v0.0.130 补齐版本一致性门禁，在 v0.0.131 统一为 `scripts/ci/verify.sh` 阶段入口，在 v0.0.132 增加构建元数据归档，在 v0.0.133 增加机器可读验证摘要归档，在 v0.0.134 增加接口治理报告归档，在 v0.0.135 增加数据库迁移审计报告归档，并在 v0.0.136 增加发布证据包归档，不包含部署动作：
  - 后端构建节点必须配置 JDK 21+，Maven Enforcer 会在 `validate` 阶段提前拦截错误 JDK 或 Maven 版本
  - `cd server && ./mvnw test`
  - `cd web && pnpm -F @vben/playground run test:unit`
  - `cd web && pnpm test:unit`
  - `cd web && pnpm -F @vben/playground run typecheck`
  - `cd web && pnpm -F @vben/playground run build`
  - 执行版本残留扫描和敏感信息扫描，避免旧版本号或本地中间件凭据进入仓库
  - 生成 `.ci-artifacts/build-metadata.json` 并归档，便于从 Jenkins 构建反查产品版本、Git 提交、分支和工具链版本
  - 生成 `.ci-artifacts/verification-summary.json` 并归档，便于从 Jenkins 构建反查门禁结果、后端测试报告统计和前端构建产物状态
  - 生成 `.ci-artifacts/api-governance-report.json` 并归档，便于从 Jenkins 构建反查接口治理质量分、发布准备度、Manifest 指纹和治理动作项
  - 生成 `.ci-artifacts/database-migration-report.json` 并归档，便于从 Jenkins 构建反查 Flyway 迁移版本连续性、命名规范、破坏性 SQL 审批和脚本指纹状态
  - 生成 `.ci-artifacts/release-evidence.json` 并归档，便于从 Jenkins 构建反查构建来源、质量门禁、数据库/API 治理证据和发布结论
  - 检查 Flyway 迁移测试通过，确保 `flyway_schema_history` 有迁移记录
  - 登录测试账号后调用 `/api/system/api-resources/governance`，要求 `passed=true` 且 `errorCount=0`
  - 推荐调用 `/api/system/api-resources/governance/report` 生成聚合治理报告，作为 Jenkins 报告、构建产物和前端接口管理页统一数据源
  - Jenkins 可读取报告中的 `qualityScore` 与 `qualityDimensions`，例如要求主干发布 `qualityScore >= 95` 且 SECURITY、AUDIT、CONTRACT 维度无 ERROR
  - Jenkins 可读取报告中的 `releaseReadiness.ready`、`releaseReadiness.status`、`releaseReadiness.blockingCheckCount` 和 `releaseReadiness.nextActionCode`，直接输出发布准备度与下一步动作
  - Jenkins 可读取报告中的 `ownerActionSummaries`，按负责人输出接口治理待办负载、阻断动作和下一步动作
  - Jenkins 可读取报告中的 `recommendedActions` 和 `actionItems`，按 `priority`、`category`、`actionCode`、`owner`、`module` 和 `blocking` 输出本次发布需要执行或跟踪的整改动作
  - 可调用 `/api/system/api-resources/governance/rules` 输出规则目录，作为 Jenkins 报告中 ruleCode 的解释来源
  - Jenkins 应将 `API_METHOD_NOT_EXPLICIT` 视为阻断项，要求所有 `/api/**` 接口使用明确 HTTP 方法，禁止 `ALL` 泛匹配路由
  - Jenkins 应将 `PUBLIC_API_WITHOUT_ACCESS_POLICY` 视为阻断项，要求公开接口补充 `@ApiAccessPolicy(ApiAuthType.PUBLIC, reason = "...")`
  - Jenkins 应将 `PUBLIC_API_NOT_IN_RUNTIME_WHITELIST` 视为阻断项，要求公开接口同步加入 `SaTokenConfig.LOGIN_EXCLUDE_PATH_PATTERNS` 或改回登录态/权限态接口
  - Jenkins 应对 `DEPRECATED_API_MISSING_SUNSET_VERSION` 和 `DEPRECATED_API_MISSING_REPLACEMENT` 输出整改提示，要求废弃接口有明确下线版本和替代接口
  - Jenkins 应将 `REMOVED_API_STILL_MAPPED` 视为阻断项，要求已移除接口不再存在运行时路由
  - Jenkins 应将 `HIGH_RISK_WRITE_API_WITHOUT_REPEAT_SUBMIT` 视为阻断项，要求非公开高风险写接口补充 `@RepeatSubmit` 或降低风险级别并说明理由
  - Jenkins 应将 `WRITE_API_WITHOUT_OPERATION_AUDIT` 视为阻断项，要求非公开写接口纳入 `OperationAuditInterceptor` 覆盖范围或经安全评审改为公开接口
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
  - 部署阶段等待服务器环境、凭据管理和人工审批策略确认后再接入，不在当前验证门禁中默认执行
- 合入 `main` 前必须确保后端测试通过、前端构建通过、数据库迁移说明完整。

更多分支说明见 [BRANCHING_AND_JENKINS.md](../BRANCHING_AND_JENKINS.md)。
