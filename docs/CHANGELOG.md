# ONES-ADMIN 版本记录

## v0.0.107 - 2026-07-07

- HRMS 员工档案抽屉移除标题、摘要卡、基础信息、合同、资料附件、任职记录、组织关系、生命周期和资料附件元信息弹窗中的硬编码中文，统一收敛到 `hr.employeeProfile` 与既有 `hr.employee` 中英文语言资源。
- 员工档案抽屉继续保持现有 Vben Drawer、Ant Design Vue Tabs、Descriptions、Timeline、Card、Upload 和 Modal 结构，只治理文案来源，为后续薪酬、绩效、考勤等更多员工分栏预留统一多语言入口。
- 本版本不新增后端接口，不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.107`。

## v0.0.106 - 2026-07-07

- HRMS 岗位管理和职级管理状态切换确认、启用/禁用动作、成功提示与取消异常文案统一收敛到 `hr.statusChange` 中英文语言资源。
- 岗位/职级页面继续保持现有 Vben Grid、CellSwitch、Ant Design Vue Modal 和 message 交互，只治理同构状态切换文案来源，降低后续基础资料页面复制硬编码的维护风险。
- 本版本不新增后端接口，不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.106`。

## v0.0.105 - 2026-07-07

- HRMS 花名册导入页移除指标卡、最近批次、CSV 上传区域、模板下载、文件校验、上传解析结果和错误明细表中的硬编码中文，统一收敛到 `hr.rosterImport` 与 `hr.employee` 中英文语言资源。
- 花名册导入模块继续保持现有 Vben / Ant Design Vue 上传、卡片和表格结构，仅治理文案来源，降低批量导入入口后续多语言和产品文案维护成本。
- 本版本不新增后端接口，不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.105`。

## v0.0.104 - 2026-07-07

- HRMS 合同管理页移除员工搜索、员工卡片、合同历史标题和合同终止表单中的硬编码中文，统一收敛到 `hr.contract` 与 `hr.employee` 中英文语言资源。
- 合同模块继续保持 Vben / Ant Design Vue 现有布局和交互，仅治理文案来源，降低多语言切换和后续文案维护成本。
- 本版本不新增后端接口，不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.104`。

## v0.0.103 - 2026-07-07

- HRMS 人力概览页移除页面内硬编码中文，将标题、说明、指标、统计卡和错误提示统一收敛到 `hr.overview` 中英文语言资源。
- 人力概览更新时间展示改为跟随当前前端语言偏好格式化，和 Vben 语言切换体系保持一致。
- 本版本不新增后端接口，不新增数据库迁移，不新增中间件，继续保持 Vben / Ant Design Vue 现有页面风格。
- 产品版本递增至 `v0.0.103`。

## v0.0.102 - 2026-07-07

- HRMS 前端新增预警共用工具，统一到期天数计算、7/30/60/90 天窗口筛选、风险状态 Tag 渲染和 7 天内到期指标口径。
- 合同到期预警和员工资料到期预警改为复用同一套预警工具，减少重复逻辑，避免后续试用期、证照、合同等预警页面出现口径分叉。
- 本版本不新增后端接口，不新增数据库迁移，不新增中间件，继续保持 Vben / Ant Design Vue 现有页面风格。
- 产品版本递增至 `v0.0.102`。

## v0.0.101 - 2026-07-07

- HRMS 合同管理页的“即将到期合同”Tab 新增预警合同总数、7 天内到期和当前窗口三项指标，帮助 HR 快速判断续签风险压力。
- 合同预警表格新增风险状态列，按到期日展示“今日到期”“N 天后到期”等状态，和员工资料预警页的运营识别方式保持一致。
- 合同预警指标和风险状态全部复用现有查询结果，不新增后端接口，不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.101`。

## v0.0.100 - 2026-07-07

- HRMS 合同管理页的“即将到期合同”Tab 新增 7/30/60/90 天预警窗口筛选，查询和 CSV 导出共用当前窗口，和员工资料预警页的运营口径保持一致。
- 合同预警表格修复员工姓名字段映射，将前端列从不存在的 `employeeName` 对齐为后端返回的 `realName`，避免姓名列空值。
- 合同预警表格补齐合同类型和合同状态字典展示，减少 HR 用户在续签风险处理时来回切换合同详情。
- 本版本不新增后端接口，不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.100`。

## v0.0.99 - 2026-07-07

- HRMS 员工合同到期预警新增 CSV 导出接口 `GET /api/hr/contracts/expiring/export`，复用 `hr:contract:list` 权限、合同到期窗口校验和员工数据范围过滤。
- 合同预警导出使用现有 `CsvExportUtils`，避免公式注入，并输出员工工号、姓名、合同编号、合同类型、状态、生效日期、到期日期、续签提醒日期和合同标识，便于 HR 对续签风险进行线下跟进留档。
- 合同管理页的“即将到期合同”Tab 在现有 Vben / Ant Design Vue 表格工具栏中新增“导出 CSV”按钮，沿用当前 30 天预警窗口，不新增视觉体系。
- 补充 HRMS 管理集成测试和接口治理 Manifest 断言，验证 30 天窗口导出只包含到期合同，并将新接口纳入版本、权限和风险等级治理。
- 本版本不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.99`。

## v0.0.98 - 2026-07-07

- HRMS 员工资料到期预警新增 CSV 导出接口 `GET /api/hr/employees/documents/expiring/export`，复用 `hr:employee:detail` 权限、员工数据范围、资料有效期窗口和 ACTIVE 文件过滤。
- 资料预警导出使用现有 `CsvExportUtils`，避免公式注入，并输出员工工号、姓名、部门、资料类型、文件名、签发日期、到期日期、风险状态和文件标识，便于 HR 合规跟进留档。
- 资料预警页在现有 Vben / Ant Design Vue 工具栏中新增“导出 CSV”按钮，沿用当前 7/30/60/90 天窗口，不新增视觉体系。
- 补充 HRMS 管理集成测试，验证 30 天窗口导出只包含到期资料，不包含窗口外资料。
- 本版本不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.98`。

## v0.0.97 - 2026-07-07

- 操作日志查询新增 `abnormalOnly` 条件，将 `success=false` 或响应码非 `0` 的记录归入异常操作视角，便于安全人员快速定位越权、文件缺失、业务失败等风险事件。
- 操作日志 CSV 导出同步支持异常操作筛选，保证安全留档与页面检索口径一致。
- 审计日志页在现有 Vben / Ant Design Vue 查询表单中新增“异常操作”筛选项，不新增视觉体系。
- 补充异常操作日志列表和导出的集成测试，覆盖正常日志被排除、异常日志可检索和 CSV 可留档。
- 本版本不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.97`。

## v0.0.96 - 2026-07-07

- 操作日志查询新增 `module`、`operation`、`permissionCode` 条件，安全人员可按业务模块、操作名称和权限码定位审计记录。
- 操作日志 CSV 导出复用同一套筛选条件，保证页面检索结果和导出留档口径一致。
- 审计日志页在现有 Vben / Ant Design Vue 查询表单中补齐模块、操作、权限码字段，不新增视觉体系。
- 补充操作日志列表和导出的集成测试，验证模块、操作、权限码模糊筛选对真实写操作审计生效。
- 本版本不新增数据库迁移，不新增中间件。
- 产品版本递增至 `v0.0.96`。

## v0.0.95 - 2026-07-07

- 系统文件下载入口新增操作审计：`GET /api/system/files/{storedName}` 在成功、授权失败、元数据缺失、物理对象缺失或读取失败时都会写入 `sys_operation_log`。
- 下载审计复用现有操作日志模型，记录用户、TraceId、IP、User-Agent、耗时、文件 ID、业务归属、原始文件名、成功状态和响应码，不新增数据库迁移。
- HRMS 合同附件和员工资料附件继续通过 `FileAccessService` 执行业务二次授权，本版本进一步保证敏感文件访问可回溯，便于安全审计、问题排查和后续 Jenkins/运营报表复用。
- 补充文件下载成功与越权失败的集成测试，验证下载流读取和授权失败都会产生可查询的操作日志。
- 产品版本递增至 `v0.0.95`。

## v0.0.94 - 2026-07-07

- 系统文件上传响应新增 `storedName` 字段，前端可直接使用服务端存储对象名访问受控文件下载入口，不再从公开 URL 反解析文件标识。
- 前端新增 `openSystemFile` 统一文件查看工具，通过 Axios 携带 `Authorization` 请求头拉取 Blob 后再打开临时预览地址，适配当前 Sa-Token 只读 Header、不读 Cookie 的安全配置。
- HRMS 合同附件、员工档案资料附件和资料预警页的“查看附件”动作统一切换到受控 Blob 下载链路，避免页面直接打开 `sys_file.url` 导致认证头丢失或绕开业务授权语义。
- 本版本不新增数据库迁移，不新增中间件，继续复用 `/api/system/files/{storedName}`、`FileAccessService`、HRMS 合同/员工资料业务访问策略和现有文件存储抽象。
- 产品版本递增至 `v0.0.94`。

## v0.0.93 - 2026-07-07

- HRMS 新增“资料预警”前端菜单页：`/hr/document-warning`，将 `v0.0.92` 的员工资料到期预警接口升级为可操作的 HR 运营视图。
- 资料预警页沿用 Vben / Ant Design Vue 的 `Page`、`Card`、`Statistic`、`VxeGrid` 和 `VbenTableAction` 组合，提供 7/30/60/90 天预警窗口筛选、风险状态标签和附件查看动作。
- 后端默认菜单树新增 `HrDocumentWarning` 菜单，复用 `hr:employee:detail` 权限，保证动态菜单、前端路由和接口权限保持一致。
- 补充动态菜单路由测试，确保 `/hr/document-warning` 能随 HRMS 菜单初始化进入授权路由。
- 本版本不新增数据库迁移，不新增后端接口，继续复用 `hr_employee_document`、`sys_file` 和既有文件下载授权策略。
- 产品版本递增至 `v0.0.93`。

## v0.0.92 - 2026-07-07

- HRMS 新增员工资料到期预警接口：`GET /api/hr/employees/documents/expiring`，默认查询 30 天内到期资料，`days` 支持 0 到 365 天。
- 员工资料响应新增 `employeeNo`、`realName`、`deptName`，让资料预警列表可直接展示员工上下文，避免前端二次查询员工信息。
- 人力概览新增 `expiringDocumentCount`、`expiredDocumentCount` 和 `documentExpiringBefore`，将 v0.0.91 的资料有效期字段纳入 HR 运营指标。
- 员工资料预警继续复用 `hr:employee:detail` 权限、员工数据范围和 `sys_file` ACTIVE 状态过滤，避免软删文件和越权员工进入预警结果。
- 前端人力概览页新增“30天内到期资料”和“已过期资料”指标卡，继续沿用 Vben / Ant Design Vue 的 `Statistic` + `Card` 密度。
- 本版本不新增数据库迁移，继续复用 `hr_employee_document` 与 `sys_file` 的职责边界。
- 产品版本递增至 `v0.0.92`。

## v0.0.91 - 2026-07-07

- HRMS 员工资料附件从单纯文件归属升级为业务资料记录，新增 `hr_employee_document` 表保存员工、文件、资料类型、签发日期、到期日期和备注。
- 资料附件接口响应新增 `documentId`、`documentType`、`issueDate`、`expireDate`、`expired`、`expiringSoon` 和 `remark`，继续保留文件元数据字段，兼容员工档案查看和文件下载授权。
- 预置 `hr_employee_document_type` 系统字典，包含身份证明、学历证明、资格证书、体检报告和其他资料，避免前端硬编码资料分类。
- 员工档案“资料附件”页签新增资料信息弹窗和到期状态标签，继续沿用 Vben / Ant Design Vue 的 `Modal`、`Form`、`Select`、`DatePicker`、`Tag` 和 `Card` 组合。
- 文件下载仍通过 `sys_file.business_type/business_id = HR_EMPLOYEE_DOCUMENT/{employeeId}` 执行业务授权，HR 业务元数据不污染系统文件表。
- 补充 HRMS 管理接口测试、字典预置测试、接口治理 Manifest 断言和前端类型检查。
- 产品版本递增至 `v0.0.91`。

## v0.0.90 - 2026-07-07

- HRMS 新增员工资料附件接口：`GET /api/hr/employees/{id}/documents`、`POST /api/hr/employees/{id}/documents/{fileId}`、`DELETE /api/hr/employees/{id}/documents/{fileId}`。
- 员工资料附件复用 `sys_file` 元数据和 `HR_EMPLOYEE_DOCUMENT` 业务归属，不新增数据库迁移；绑定要求员工数据范围可见且当前用户可访问未绑定临时文件。
- 文件下载新增员工资料附件业务访问策略，必须同时满足 `hr:employee:detail` 权限和员工数据范围，避免附件 URL 脱离员工档案授权边界。
- 员工档案抽屉新增“资料附件”分栏，沿用 Vben / Ant Design Vue 的 `Tabs`、`Card`、`Upload`、`Button`、`Tag` 和 `Empty` 组合，支持上传、查看和移除资料。
- Sa-Token 会话存储新增 `ones.security.session.storage` 配置，正式默认 Redis，本地 E2E 可显式切换内存 DAO，避免测试启动误依赖未配置的 Redis。
- 补充 HRMS 管理接口测试、数据范围下载授权测试、接口治理 Manifest 断言、前端类型检查和员工档案 E2E 断言。
- 产品版本递增至 `v0.0.90`。

## v0.0.89 - 2026-07-07

- HRMS 新增员工组织关系接口 `GET /api/hr/employees/{id}/org-context`，复用 `hr:employee:detail` 权限和员工数据范围校验。
- 组织关系返回组织路径、直属上级、当前员工、可见直属下级和直属下级数量，先用现有 `sys_dept`、`hr_employee.manager_employee_id`、岗位和职级数据计算，不新增数据库迁移。
- 员工档案抽屉新增“组织关系”分栏，继续使用 Vben / Ant Design Vue 的 `Tabs`、`Card`、`Descriptions`、`Tag` 和 `Empty` 组合。
- 对标 Frappe HRMS 的 employee profile 中心入口思路，ONES-ADMIN 将员工档案继续从静态详情推进为组织关系、任职记录、生命周期和合同的统一入口。
- 补充 HRMS 管理接口测试、接口治理 Manifest 断言和员工档案 E2E 断言。
- 产品版本递增至 `v0.0.89`。

## v0.0.88 - 2026-07-07

- HRMS 新增员工任职记录接口 `GET /api/hr/employees/{id}/jobs`，复用 `hr:employee:detail` 权限和员工数据范围校验。
- 任职记录返回部门、岗位、职级、直属上级、用工类型、起止日期和变动原因，按生效日期倒序展示员工岗位历史。
- 员工档案抽屉新增“任职记录”分栏，继续使用 Vben / Ant Design Vue 的 `Tabs`、`Timeline`、`Card`、`Descriptions` 和 `Tag` 组合，不引入新的视觉体系。
- 修正员工档案生命周期入职事件颜色识别，前端按后端实际 `ONBOARD` 事件展示。
- 补充 HRMS 管理接口测试、接口治理 Manifest 断言和员工档案 E2E 断言；本版本不新增数据库迁移。
- 产品版本递增至 `v0.0.88`。

## v0.0.87 - 2026-07-07

- HRMS 员工列表新增“档案”入口，按 Vben / Ant Design Vue 现有抽屉风格集中展示员工基础信息、合同记录和生命周期事件。
- 员工档案抽屉复用现有员工详情、员工合同和生命周期接口，不新增后端契约，避免重复建模和接口膨胀。
- 档案视图继续消费 HRMS 字典缓存，员工状态、用工类型、合同类型和合同状态展示与系统数据字典保持一致。
- 员工档案 E2E 覆盖新增入口和“基础信息 / 合同 / 生命周期”三段式视图，防止后续前端重构破坏 HRMS 中心档案工作流。
- 产品版本递增至 `v0.0.87`。

## v0.0.86 - 2026-07-07

- 系统文件新增清理保留策略接口 `GET /api/system/files/retention`，输出已删除文件保留天数、清理阈值、待清理数量和容量。
- 系统文件新增过期物理清理接口 `POST /api/system/files/retention/purge`，仅清理超过保留期且未绑定业务的已删除文件，清理后元数据状态进入 `PURGED` 以保留审计线索。
- 文件存储抽象新增物理删除能力，本地存储和 MinIO 存储均已实现；MinIO 删除使用官方 Java SDK `removeObject(RemoveObjectArgs)`。
- 新增 `system:file:purge` 独立权限和文件管理页“清理过期”按钮，物理清理不复用软删除权限，便于角色授权和 Jenkins 审计分权。
- 文件管理页支持 `PURGED` 状态筛选和待清理指标，继续保持 Vben / Ant Design Vue 现有系统页风格。
- 补充文件保留策略预览、物理清理和无权限拒绝测试。
- 产品版本递增至 `v0.0.86`。

## v0.0.85 - 2026-07-07

- 系统文件元数据和下载接口统一接入 `FileAccessService`，文件访问边界从“接口权限”升级为“文件归属 + 业务策略”。
- 未绑定临时文件允许上传人查看元数据和下载，系统文件管理员仍可通过 `system:file:read` 管理全局文件资产。
- 其他普通用户无法查看或下载非本人上传的未绑定文件，避免合同附件保存前的临时文件被横向访问。
- 删除接口仍要求 `system:file:delete`，并在删除前复用文件归属校验，已绑定业务附件继续禁止删除。
- 文件详情和下载接口治理元数据更新为 `v0.0.85`，补充上传人临时访问说明。
- 补充上传人临时文件元数据、下载和删除权限测试。
- 产品版本递增至 `v0.0.85`。

## v0.0.84 - 2026-07-07

- 文件下载新增业务归属访问策略：未绑定文件要求 `system:file:read`，HRMS 合同附件要求 `hr:contract:list` 且员工在当前用户数据范围内。
- 合同附件下载在读取底层存储前先校验合同绑定关系和员工数据范围，避免仅凭 `storedName` 绕过 HRMS 授权边界。
- 新增 HRMS 合同附件元数据业务接口，合同页面查看附件不再依赖系统文件管理的 `system:file:read` 权限。
- 合同页附件查看按钮改为跟随 `hr:contract:list` 权限展示，HR 用户无需获得系统文件管理权限即可查看自己数据范围内的合同附件。
- 文件下载接口治理元数据更新为 `v0.0.84`，公开访问说明明确为“登录态 + 文件业务归属二次授权”。
- 补充系统文件下载权限测试和 HRMS 合同附件数据范围测试。
- 产品版本递增至 `v0.0.84`。

## v0.0.83 - 2026-07-07

- HRMS 合同抽屉新增轻量合同附件区，支持上传、替换、查看和移除附件，保持 Vben / Ant Design Vue 既有表单风格。
- 合同历史列表新增附件列和附件查看动作，HR 可在合同列表直接打开已绑定附件。
- 前端文件 API 新增单个文件元数据查询，并统一上传、详情、删除响应的文件 ID 字符串归一化。
- 合同附件前端闭环复用 `system:file:upload` 和 `system:file:read` 权限，后端继续负责合同保存后的业务归属绑定与已绑定文件删除保护。
- 产品版本递增至 `v0.0.83`。

## v0.0.82 - 2026-07-07

- 新增文件元数据分页查询接口 `GET /api/system/files`，支持文件名、扩展名、存储类型、状态、业务归属、上传人和上传时间筛选。
- 系统文件能力从隐藏权限点升级为正式菜单 `/system/file`，补齐文件管理菜单、查询、上传和删除按钮权限树。
- 前端按 Vben / Ant Design Vue 现有系统页风格新增文件管理页，支持文件资产列表、状态指标、工具栏上传和软删除操作。
- 继续吸收 RuoYi-Vue-Plus 的 OSS 资产管理思路，ONES-ADMIN 当前先落地轻量文件资产可视化，不引入复杂对象存储同步任务。
- 产品版本递增至 `v0.0.82`。

## v0.0.81 - 2026-07-07

- `sys_file` 新增 `status` 和 `deleted_at` 生命周期字段，文件默认 `ACTIVE`，删除后进入 `DELETED` 状态。
- 新增文件软删除接口 `DELETE /api/system/files/{id}`，补齐 `system:file:delete` 权限和授权树节点。
- 文件详情和下载默认只暴露 `ACTIVE` 文件，已删除文件不再通过元数据详情入口返回。
- 已绑定 HRMS 合同等业务归属的文件禁止删除，避免附件被误删导致业务记录断链。
- 产品版本递增至 `v0.0.81`。

## v0.0.80 - 2026-07-07

- 新增 `sys_file` 文件元数据表，记录原始文件名、存储对象名、URL、类型、大小、存储类型、桶名、上传人和业务归属。
- 文件上传接口保持 `url` 兼容返回，并新增文件 ID、原始文件名、大小、类型、扩展名和存储类型。
- 新增文件元数据详情接口 `/api/system/files/{id}/metadata`，并补齐 `system:file:read` 权限和授权树节点。
- HRMS 合同附件从裸 ID 升级为有效文件引用校验，合同创建/更新会绑定或解除附件业务归属。
- 产品版本递增至 `v0.0.80`。

## v0.0.79 - 2026-07-07

- HRMS 花名册导入错误行 `rawJson` 改为保存脱敏后的行快照，手机号、邮箱不再以原文进入错误报告。
- 导入链路仍使用原始手机号、邮箱创建成功员工，只在失败行落库和错误行接口返回时使用脱敏副本，不影响导入业务能力。
- 补充花名册导入错误行脱敏断言，确保错误行响应包含脱敏手机号、脱敏邮箱，同时不包含原始联系方式。
- 继续对标 RuoYi-Plus 系列敏感字段注解和权限绑定做法；ONES-ADMIN 当前复用轻量脱敏工具治理导入旁路，后续再抽象注解化策略。
- 产品版本递增至 `v0.0.79`。

## v0.0.78 - 2026-07-07

- 新增 `hr:employee:sensitive:view` 权限和员工管理授权树按钮，用于控制员工手机号、邮箱等敏感联系方式的完整展示。
- HRMS 员工详情以及创建、编辑、调岗、转正、离职等接口响应按敏感查看权限决定手机号、邮箱是否脱敏，员工列表和花名册导出继续默认脱敏。
- 无敏感查看权限的用户编辑员工时，后端保留原手机号和邮箱，前端 Vben 编辑抽屉同步禁用手机号、邮箱字段并剔除更新 payload，避免脱敏展示值污染主数据。
- 员工响应新增 `sensitiveVisible` 标记，前端可基于后端授权结果决定字段交互状态，不再猜测字段是否已脱敏。
- 补充 HRMS 敏感字段权限集成测试，覆盖超级管理员完整查看、普通 HR 脱敏查看以及无权更新联系方式的服务端保护。
- 通过 agent-reach GitHub/dev 路由继续对标 Smart Admin、RuoYi-Vue-Plus、JeecgBoot 的注解化脱敏和权限绑定思路，ONES-ADMIN 当前先采用权限码 + 服务层响应治理，后续再演进为注解策略。
- 产品版本递增至 `v0.0.78`。

## v0.0.77 - 2026-07-07

- 新增通用敏感数据脱敏工具，先覆盖手机号、邮箱、证件号和通用文本脱敏，为后续字段级权限和注解化脱敏治理打底。
- HRMS 员工列表和员工花名册 CSV 导出默认脱敏手机号、邮箱和证件号，避免批量浏览与导出场景暴露完整联系方式。
- HRMS 员工详情继续保留可编辑所需的完整手机号和邮箱，前端编辑抽屉打开时重新拉取员工详情，避免把列表脱敏值误提交回数据库。
- 补充敏感字段脱敏单元测试和 HRMS 列表/导出脱敏断言。
- 通过 agent-reach GitHub/dev 路由对标 Smart Admin、RuoYi-Vue-Plus、JeecgBoot 的脱敏基础能力，ONES-ADMIN 当前采用轻量工具层落地，后续再升级为注解化或字段级权限策略。
- 产品版本递增至 `v0.0.77`。

## v0.0.76 - 2026-07-07

- 角色模型新增 `data_scope` 数据范围字段，支持 `ALL`、`DEPT_AND_CHILD`、`DEPT`、`SELF` 四类口径，并通过 Flyway 增量脚本 `V5__add_role_data_scope.sql` 管理迁移。
- 后端新增统一数据范围上下文与计算服务，基于当前登录用户、部门和角色数据范围计算可见部门集合，为后续请假、考勤、薪资等 HRMS 模块复用打底。
- HRMS 员工列表、员工详情、生命周期查询、花名册导出、员工合同列表、即将到期合同和人力概览接入数据范围强约束，防止仅靠前端隐藏导致越权读取。
- 系统角色管理接口和 Vben 角色页新增数据范围读写与展示，超级管理员保持 `ALL`，普通新增角色默认 `DEPT_AND_CHILD`。
- 新增角色数据范围接口测试和 HRMS 数据范围集成测试，覆盖部门范围、本人范围、越权详情、合同、导出和概览统计。
- 产品版本递增至 `v0.0.76`。

## v0.0.75 - 2026-07-06

- 新增通用前端字典选项缓存工具，HRMS 保留业务字典码和兜底选项，后续模块可复用同一套 options 加载、缓存、格式化和缓存清理能力。
- 后端默认字典补齐 `hr_gender`、`hr_contract_status`、`hr_roster_import_status`，并在启动初始化时停用合同状态旧值 `EXPIRED` 和花名册导入旧值 `PENDING`。
- HRMS 员工性别、合同状态、花名册导入状态和人力概览状态颜色接入系统字典，继续减少前端硬编码枚举。
- 修正合同新增/编辑前端提交契约，新增合同默认提交 `ACTIVE` 状态，编辑合同沿用已有状态，保持与后端 `status` 必填校验一致。
- 字典接口测试补充 HRMS 性别、合同状态、花名册导入状态 options 契约断言。
- 产品版本递增至 `v0.0.75`。

## v0.0.74 - 2026-07-06

- HRMS 员工新增表单、员工列表筛选和员工列表展示接入系统数据字典，优先消费 `hr_employment_type`、`hr_employment_status` 的登录态 options 接口。
- HRMS 合同新增/编辑表单和合同列表展示接入 `hr_contract_type` 字典，不再使用前端硬编码合同类型。
- 修正 HRMS 合同类型默认字典值，与后端合同接口保持一致：`FIXED_TERM`、`OPEN_ENDED`、`INTERNSHIP`、`SERVICE`；历史旧值 `FIXED`、`UNFIXED`、`DISPATCH`、`INTERN` 会在初始化时停用。
- 字典接口测试补充合同类型 options 契约断言，避免前端字典值和后端业务校验再次偏离。
- 产品版本递增至 `v0.0.74`。

## v0.0.73 - 2026-07-06

- 后端新增系统数据字典基础能力，采用 `sys_dict_type` + `sys_dict_item` 两表模型，提供字典类型、字典项 CRUD 和登录态字典选项接口。
- 启动初始化补齐系统数据字典菜单 `/system/dict`、按钮权限和超级管理员授权，并预置 HRMS 用工类型、用工状态、合同类型三组基础字典。
- 前端按 Vben / Ant Design Vue 现有后台风格新增系统数据字典页，采用左侧字典类型、右侧字典项的管理布局，不引入独立设计体系。
- 新增 Flyway 增量脚本 `V4__create_system_dictionary.sql`，并补充字典接口集成测试、菜单断言和接口治理版本断言。
- 产品版本递增至 `v0.0.73`。

## v0.0.72 - 2026-07-06

- 前端新增系统管理审计日志页，按 Vben / Ant Design Vue 现有风格承接登录日志、操作日志、CSV 导出、保留策略预览和过期日志清理。
- 后端动态菜单将审计日志从授权树 button 节点升级为真实菜单 `/system/audit`，登录日志、操作日志和保留策略继续按按钮权限分权治理。
- 认证菜单测试补充 `/system/audit` 路由断言，确保超级管理员登录后能拿到审计日志页面。
- 产品版本递增至 `v0.0.72`。

## v0.0.71 - 2026-07-06

- 前端接口管理页新增 Manifest 基线视图，展示当前 Manifest 版本、接口数、指纹、最新归档快照、历史快照列表和发布状态。
- 前端 API client 补齐 `/system/api-resources/manifest/snapshots` 与 `/system/api-resources/manifest/snapshots/latest` 类型和请求方法，让 Jenkins 使用的接口契约基线能力也能被后台页面直接承接。
- 通过 agent-reach GitHub/dev 路由复核 API Management、API Catalog、OpenAPI Diff 开源项目，继续吸收“版本资产化、基线可见、差异可追踪”的接口治理思路。
- 产品版本递增至 `v0.0.71`。

## v0.0.70 - 2026-07-06

- HRMS 新增人力概览后端接口 `GET /api/hr/overview`，输出员工总数、在职/试用期/离职人数、部门数量、有效合同、30 天内到期合同、30 天内待转正、员工状态分布、部门分布和近 30 天生命周期事件。
- 新增 `hr:overview:view` 权限和 HRMS 菜单节点，概览接口纳入 Sa-Token、接口治理 Manifest、风险等级、权限注册和授权树检查。
- 前端按 Vben / Ant Design Vue 当前后台风格新增 HRMS 人力概览页，承接员工状态、部门分布、合同预警和生命周期事件统计。
- 通过 agent-reach GitHub/dev 路由复核开源 API Management 项目，继续吸收 Developer Portal、分析统计、审批工作流和治理门户化思路。
- 产品版本递增至 `v0.0.70`。

## v0.0.69 - 2026-07-05

- HRMS 员工管理新增员工花名册 CSV 导出接口，复用当前员工筛选条件，导出字段仅包含脱敏证件号，并设置 5000 行上限避免大批量误导出。
- 新增 `hr:employee:export` 权限和员工管理导出按钮节点，导出接口纳入 Sa-Token、接口治理 Manifest、风险等级和权限注册检查。
- 前端员工列表按 Vben / Ant Design Vue 当前风格新增“导出花名册”按钮，导出时携带当前查询条件和左侧部门筛选。
- 产品版本递增至 `v0.0.69`。

## v0.0.68 - 2026-07-05

- 接口治理聚合报告新增 `ownerActionSummaries`，按负责人聚合治理动作总数、打开动作数、阻断动作数、P0/P1/P2 待办、分类和下一步动作。
- 负责人治理摘要借鉴 API Catalog 的 Owner 责任制，便于 Jenkins、接口管理页和人工巡检按团队追踪接口治理负载。
- 产品版本递增至 `v0.0.68`。

## v0.0.67 - 2026-07-05

- 修复 HRMS 花名册导入前端批次列表分页响应字段，从旧 `records` 对齐为后端统一 `list`，并同步使用 `pageNum` 查询参数。
- 花名册导入页按 Vben / Ant Design Vue 现有风格补齐批次概览、最近批次状态、前端 CSV 类型与 2MB 大小校验，并修正 `PARTIAL_SUCCESS`、`VALIDATION_FAILED` 等后端状态展示。
- 接口治理报告移除已完成的 `IMPLEMENT_HRMS_ROSTER_FRONTEND` 推荐动作和待办项，避免 Jenkins 与接口管理页继续提示过期整改动作。
- 产品版本递增至 `v0.0.67`。

## v0.0.66 - 2026-07-05

- 接口治理聚合报告新增 `releaseReadiness` 发布就绪摘要，输出机器可读状态、发布可用性、优先级、基线状态、阻断检查数、打开动作数和下一步动作，便于 Jenkins 直接判定接口发布准备度。
- 前端接口管理页同步展示发布就绪卡片和门禁摘要，保持 Vben / Ant Design Vue 现有页面风格。
- 产品版本递增至 `v0.0.66`。

## v0.0.65 - 2026-07-05

- HRMS 员工档案补齐 `PUT /api/hr/employees/{id}` 基础信息编辑接口，限定只更新姓名、昵称、性别、手机号、邮箱、证件号脱敏、关联用户、试用期结束日期和备注，不允许绕过调岗、转正、离职等生命周期接口。
- 新增 `hr:employee:update` 权限和授权树节点，员工编辑接口接入 Sa-Token、`@RepeatSubmit`、接口治理元数据和 HRMS 写操作审计。
- 前端 Vben HRMS 员工模块同步接入编辑抽屉，修正员工分页响应、员工状态枚举、转正/离职 DTO 字段名和后端契约一致性。
- 工程化忽略本地 H2 运行产物 `server/data/`，避免开发数据文件进入提交候选。
- 产品版本递增至 `v0.0.65`。

## v0.0.63 - 2026-07-03

- 接口治理聚合报告新增 `qualityScore` 与 `qualityDimensions`，按安全、审计、契约、生命周期、目录、文档六个维度输出企业级接口治理评分。
- 评分维度吸收 Backstage、Gravitee、Kong、APISIX、Tyk、Spectral、oasdiff 等开源项目的接口目录、API 管理、网关策略和 OpenAPI 契约治理思路，便于 Jenkins 与前端接口管理页做趋势跟踪。
- 前端接口资源 API 类型同步新增治理评分字段，后续页面可直接展示评分条、维度卡或发布阈值。
- 产品版本递增至 `v0.0.63`。

## v0.0.62 - 2026-07-03

- 接口资源清单、CSV 导出、Manifest 和 Manifest 指纹新增 `operationAuditProtected`，写接口是否纳入操作审计成为可追踪接口契约。
- 接口治理新增 `WRITE_API_WITHOUT_OPERATION_AUDIT` 阻断规则，非公开写接口缺少操作审计覆盖时会作为 ERROR 阻断发布门禁。
- 操作审计拦截器抽出统一审计路径判断，接口治理与运行时拦截器复用同一套覆盖口径；除 `/api/system/**`、`/api/hr/**` 外，认证令牌刷新、退出登录和时区设置也进入操作审计。
- 产品版本递增至 `v0.0.62`。

## v0.0.61 - 2026-07-03

- 操作审计拦截器覆盖范围从系统写接口扩展到 HRMS 写接口，`/api/hr/**` 的 `POST`、`PUT`、`PATCH`、`DELETE` 请求会统一写入 `sys_operation_log`。
- 新增 HRMS 写操作审计测试，验证新增岗位后可通过 TraceId 在操作日志中定位路径、模块、操作名称和权限码。
- 后端工程化文档同步明确 HRMS 生命周期事件不替代操作日志，HRMS 写接口已进入统一审计闭环。
- 产品版本递增至 `v0.0.61`。

## v0.0.60 - 2026-07-03

- 接口治理新增 `PUBLIC_API_ACCESS_POLICY_REASON_MISSING` 阻断规则，公开接口即使已声明 `@ApiAccessPolicy(ApiAuthType.PUBLIC)`，也必须填写 `reason` 说明开放原因、调用方和安全补偿措施。
- 治理规则目录同步输出新规则的严重级别、分类、阻断状态和修复建议，便于 Jenkins、接口管理页和人工巡检复用同一套解释。
- 补充公开接口开放原因缺失的 TDD 用例，确保接口目录不会只登记公开策略而缺少审计理由。
- 产品版本递增至 `v0.0.60`。

## v0.0.59 - 2026-07-03

- 后端接口治理聚合报告新增 `actionItems`，将 Manifest 基线、Diff 归档、治理规则违规和 HRMS 前端接入转成可跟踪动作项，便于 Jenkins 和后续接口管理页直接展示待办。
- `recommendedActions` 移除已过期的 `DESIGN_HRMS_PHASE_ONE`，改为 `IMPLEMENT_HRMS_ROSTER_FRONTEND`，对齐当前 HRMS 已完成一期设计并进入花名册前端接入阶段的实际状态。
- 接口治理报告测试补充 `actionItems` 契约断言，确保动作项包含优先级、来源类型、来源编码、负责人、模块、接口、阻断状态、状态和验证方式。
- 产品版本递增至 `v0.0.59`。

## v0.0.58 - 2026-07-03

- HRMS Phase 1E 开始落地花名册导入后端能力，新增 CSV 模板下载、上传导入、导入批次列表/详情和错误行查询接口。
- 花名册导入成功行复用正式员工新增链路，会写入员工主数据、初始任职记录和入职生命周期事件；失败行落入 `hr_roster_import_error`，保留行号、字段、错误原因和原始数据 JSON。
- 新增 `hr:roster:list`、`hr:roster:import` 权限和授权树节点，上传导入接口接入 `@RepeatSubmit`、Sa-Token 权限和接口治理元数据。
- 接口治理总数更新至 76 个，OpenAPI、Manifest、治理报告和测试断言同步到 `v0.0.58`。
- 产品版本递增至 `v0.0.58`。

## v0.0.57 - 2026-07-03

- 登录页右侧继续收敛为企业级主表单，不再展示统一认证、锁定保护、审计追踪等说明项，避免登录卡片臃肿。
- 飞书、企业 SSO、手机号登录入口统一为 icon-only 圆形按钮，保留 tooltip 与无障碍标签；二维码登录保留右上角折角入口。
- 登录 E2E 补充紧凑登录方式断言，验证三方入口无可见文字、二维码角标跳转路径和 icon 圆角。
- 产品版本递增至 `v0.0.57`。

## v0.0.56 - 2026-07-03

- HRMS 员工合同管理继续补齐流程能力，新增 `GET /api/hr/contracts/expiring` 即将到期合同查询和 `POST /api/hr/contracts/{contractId}/terminate` 合同终止接口。
- 合同到期查询默认 30 天窗口，最大 365 天，按结束日期升序返回，避免无边界扫描；终止接口校验终止日期不能早于合同开始日期。
- 新增 `hr:contract:terminate` 权限和授权树节点，合同终止接口接入 `@RepeatSubmit`、Sa-Token 权限和接口治理元数据。
- 接口治理总数更新至 71 个，OpenAPI、Manifest、治理报告和测试断言同步到 `v0.0.56`。
- 产品版本递增至 `v0.0.56`。

## v0.0.55 - 2026-07-03

- HRMS Phase 1D 开始落地员工合同管理核心接口，新增员工合同列表、新增、编辑能力，复用既有 `hr_employee_contract` 表。
- 新增 `hr:contract:list`、`hr:contract:create`、`hr:contract:update` 权限和授权树节点，合同写接口接入 `@RepeatSubmit` 与接口治理元数据。
- 员工合同新增合同编号唯一校验、合同类型/状态枚举校验、结束日期不能早于开始日期校验，返回员工编号、姓名和合同关键字段，便于员工详情页聚合展示。
- 接口治理总数更新至 69 个，OpenAPI、Manifest、治理报告和测试断言同步到 `v0.0.55`。
- 产品版本递增至 `v0.0.55`。

## v0.0.54 - 2026-07-03

- HRMS Phase 1C 开始落地，新增 `GET /api/hr/employees/{id}/lifecycle-events` 员工生命周期时间线接口，使用独立权限 `hr:employee:lifecycle`。
- 员工生命周期事件按事件日期、创建时间和事件 ID 倒序返回，并将事件明细 JSON 解析为结构化 `detail`，便于前端详情页展示入职、调岗、转正、离职全过程。
- 权限初始化和授权树补齐员工生命周期查询节点，确保 Sa-Token 权限、菜单授权、接口治理和超级管理员授权闭环。
- 复核 Backstage、Gravitee、Kong、APISIX、Frappe HRMS、OrangeHRM 等开源项目状态，继续将 API Catalog、API Management 和 HRMS 员工生命周期理念沉淀到后端工程化路线。
- 产品版本递增至 `v0.0.54`。

## v0.0.53 - 2026-07-02

- 登录页按 ONES/1S 品牌规范继续升级，保留 Vben `AuthPageLayout` 与 `AuthenticationLogin` 组件体系，左侧承载完整 1S 核心视觉、能力节点和品牌理念。
- 右侧登录卡片精简为品牌标题、账号密码表单、登录按钮和紧凑企业登录方式，不再堆叠长说明、状态卡和文字型登录方式按钮。
- 二维码登录入口调整到登录卡右上角折角位置，底部三方与辅助登录收敛为圆形 icon 入口，贴近主流企业登录页模式。
- 企业登录方式按飞书优先设计，飞书支持 `App ID + redirect_uri` 自动生成授权地址，也支持直接配置授权 URL；未配置时只提示配置，不发起伪登录。
- 企业 SSO 入口保持配置驱动，只有配置 `VITE_GLOB_AUTH_SSO_URL` 后才展示，扫码登录和手机号登录以紧凑入口保留。
- 登录页品牌图标和 1S 主视觉新增白天/夜间两套资源，并跟随 Vben 主题自动切换。
- 中英文认证文案补充登录按钮文案，避免品牌登录页出现默认化按钮语义。
- 产品版本递增至 `v0.0.53`。

## v0.0.52 - 2026-07-02

- HRMS Phase 1B 开始落地，新增员工调岗、转正、离职接口，补齐 `hr:employee:transfer`、`hr:employee:regularize`、`hr:employee:resign` 权限。
- 员工调岗会关闭当前任职记录并新增任职记录；调岗、转正、离职都会写入生命周期事件，保留变更前后快照和原因。
- HRMS 员工生命周期新增状态与日期校验，离职员工不能继续调岗，转正/调岗/离职日期不能早于入职日期。
- 产品版本递增至 `v0.0.52`。

## v0.0.51 - 2026-07-02

- HRMS Phase 1A 开始落地，新增 `hr_` 前缀的一期基础表迁移，覆盖岗位、职级、员工主数据、任职记录、生命周期事件、合同和花名册导入批次/错误行。
- 后端新增 HRMS 岗位、职级、员工基础接口，新增员工时同事务写入员工主数据、首条任职记录和入职生命周期事件。
- 新增 HRMS 独立错误码号段、Swagger 分组、权限初始化和授权树挂载，确保接口治理、Sa-Token 权限和超级管理员授权闭环。
- 产品版本递增至 `v0.0.51`。

## v0.0.50 - 2026-07-02

- 新增 `docs/architecture/hrms-phase-one-design.md`，将 HRMS 从调研建议推进为一期企业级设计稿。
- HRMS 一期设计明确采用“主数据 + 合同 + 花名册”方案，覆盖业务范围、表结构草案、接口清单、权限码、事务一致性、敏感数据、审计、前端页面和 Flyway 迁移计划。
- 复核 Frappe HR、OrangeHRM、IceHrm、OpenHRMS 最新公开仓库状态，并将参考结论沉淀到设计文档。
- 产品版本递增至 `v0.0.50`。

## v0.0.49 - 2026-07-02

- 后端接口治理聚合报告新增 `referenceBenchmarks`，结构化输出 Backstage、Gravitee、Kong、APISIX、Tyk、Frappe HR、OrangeHRM、IceHrm 等参考基准和可借鉴点。
- 后端接口治理聚合报告新增 `recommendedActions`，按治理错误、治理警告、权限缺口、Manifest Gate 和 HRMS 一期设计输出机器可读整改动作。
- 前端接口资源 API 类型同步新增参考基准与推荐动作字段，为后续接口管理页展示报告动作清单打底。
- 补充接口治理报告测试断言，确保 Jenkins 和前端后续消费参考基准、推荐动作时有稳定契约。
- 产品版本递增至 `v0.0.49`。

## v0.0.48 - 2026-07-02

- 登录页继续按 Vben 认证布局升级为更正式的 ONES / 1S 企业级入口，右侧新增品牌登录卡片、状态项图标和安全锁定提示。
- 移除登录表单中的演示型“快速选择账号”下拉，E2E 改为显式输入 `admin / admin123`，让登录流程更接近正式账号密码认证。
- 左侧 1S 系统核心视觉取消圆形裁切，完整展示品牌发光舞台，并优化认证布局容器、暗色主题背景和卡片层次。
- 产品版本递增至 `v0.0.48`。

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
