# HRMS 企业级人力系统调研与一期范围建议

更新时间：2026-07-03

本文是 HRMS 设计前的调研输入。`v0.0.50` 已补充正式一期设计稿，见 [HRMS 一期企业级设计方案](hrms-phase-one-design.md)。

## 1. 调研来源

本轮使用 agent-reach 的 GitHub CLI 路由与网页检索，重点参考以下开源或成熟产品资料：

| 项目/产品 | 参考价值 | 对 ONES-ADMIN 的启发 |
| --- | --- | --- |
| [Frappe HR](https://docs.frappe.io/hr/introduction) / [frappe/hrms](https://github.com/frappe/hrms) | 覆盖员工、组织、考勤、排班、请假、绩效、招聘、培训、员工生命周期、薪酬、税务、报表等完整 HRMS 模块 | HRMS 应按员工生命周期拆模块，不应只做员工表 CRUD；员工、部门、岗位、入转调离和请假考勤应形成一致主数据 |
| [OrangeHRM Starter](https://orangehrm.com/orangehrm-starter-open-source-software) / [orangehrm/orangehrm](https://github.com/orangehrm/orangehrm) | 强调 HR Administration、Employee Management、Reporting & Analytics、Recruitment、Onboarding、Leave、Time Tracking | 企业级一期至少需要员工档案、组织架构、花名册、报表和可扩展的招聘/入职边界 |
| [IceHrm](https://icehrm.com/) / [gamonoid/icehrm](https://github.com/gamonoid/icehrm) | 中小企业 HRIS 的实用模块：员工信息、请假、考勤、招聘、薪酬报表、文档、审批链 | 可以借鉴“先实用、后复杂”的路线，请假和考勤可作为二期，薪酬先只保留接口边界 |
| [Odoo Employees](https://www.odoo.com/app/employees) | 强调员工档案、组织层级、招聘、绩效、入职计划、考勤、休假、签署等一体化体验 | 员工档案应成为 HRMS 入口，员工详情页要能聚合合同、岗位、审批人、附件和生命周期事件 |

## 2. HRMS 模块地图

企业级 HRMS 建议拆成以下业务域：

| 业务域 | 说明 | 一期建议 |
| --- | --- | --- |
| 组织与岗位 | 公司、部门、岗位、职级、汇报关系、组织树 | 必做，复用现有 `sys_dept` 并补岗位/职级边界 |
| 员工主数据 | 员工编号、姓名、联系方式、证件、入职日期、用工状态、直属上级、所属部门岗位 | 必做，作为 HRMS 核心资产 |
| 员工生命周期 | 入职、转正、调岗、离职、再入职、状态变更记录 | 必做，避免只改当前值没有历史 |
| 合同与任职 | 合同编号、合同类型、开始/结束日期、试用期、续签提醒、附件 | 推荐一期纳入，企业 HR 常用且能体现严肃性 |
| 花名册与导入导出 | 员工列表、筛选、Excel 导入导出、字段模板、错误行报告 | 推荐一期纳入，方便真实落地 |
| 请假与考勤 | 假期类型、假期余额、请假申请、出勤记录、异常考勤 | 二期，规则复杂，建议在主数据稳定后做 |
| 招聘与入职 | 职位需求、候选人、面试、Offer、入职任务 | 二期或三期，和外部招聘渠道/审批流程耦合较多 |
| 薪酬与绩效 | 薪资结构、薪资项、绩效周期、考核记录 | 后置，合规和权限要求高，不能一期仓促实现 |
| 员工自助 | 个人资料、申请、证明下载、移动端 | 后置，依赖流程和权限成熟度 |

## 3. 一期范围候选

### 方案 A：最小主数据

范围：组织、岗位、员工档案、入转调离。

优点：最快开始编码，风险低。  
不足：缺少合同和导入导出，离真实 HR 使用还有距离。  
适合：先验证菜单、权限、表结构和页面风格。

### 方案 B：主数据 + 合同 + 花名册

范围：组织、岗位、员工档案、员工生命周期、合同管理、花名册导入导出。

优点：企业级基础最稳，能覆盖 HR 日常维护和审计留痕；不会过早进入考勤/薪酬复杂规则。  
不足：比方案 A 多合同与导入导出，需要更完整的数据校验和错误报告。  
推荐：作为 ONES-ADMIN HRMS 一期默认方案。

### 方案 C：主数据 + 合同 + 考勤薪酬基础

范围：方案 B 加请假、考勤、薪酬基础模型。

优点：业务完整度高，演示效果强。  
不足：范围明显变大，考勤和薪酬规则容易牵涉地区、班次、假期余额和合规口径，一期容易失焦。  
适合：已有明确考勤/薪酬规则并接受较长开发周期。

## 4. 推荐一期边界

建议采用方案 B：

- 新增 `hr` 业务域，避免继续把所有业务塞进 `system` 包。
- 后端包建议采用 `com.ones.admin.hr`，下设 `employee`、`position`、`contract`、`lifecycle`、`importing` 等子域。
- 数据库表建议以 `hr_` 前缀命名，和 `sys_` 系统表清晰隔离。
- 权限码建议以 `hr:*:*` 命名，例如 `hr:employee:list`、`hr:employee:create`、`hr:contract:update`。
- 所有 HRMS 写接口必须接入 `@RepeatSubmit`、操作审计、接口治理元数据和明确 `operationId`。
- 员工敏感字段需要分级展示，身份证号、手机号、合同附件等字段后续应接入数据权限和脱敏策略。
- 前端页面必须沿用 Vben 系统管理页风格：查询表单 + VxeGrid 表格 + 抽屉/弹窗表单 + 详情页，不设计独立 HR SaaS 风格。

## 5. 待确认问题

正式设计前需要确认一个关键问题：

> HRMS 一期是否按“方案 B：主数据 + 合同 + 花名册”推进？

当前默认按方案 B 形成一期设计稿，内容包括模块边界、表结构草案、接口清单、权限点、页面清单、审计要求、测试策略和分期计划，见 [HRMS 一期企业级设计方案](hrms-phase-one-design.md)。

## 6. 接口治理报告联动

从 `v0.0.49` 起，`/api/system/api-resources/governance/report` 会在 `recommendedActions` 中输出 `DESIGN_HRMS_PHASE_ONE` 动作，提示 HRMS 一期正式设计需要承接本调研结论。

这样做的目的不是把 HRMS 立即写进系统接口，而是让 Jenkins、接口管理页和人工巡检能持续看到“下一步企业支撑能力”的设计责任，避免 HRMS 停留在一次性调研文档里。`v0.0.50` 已用正式设计稿承接该动作。

## 7. v0.0.54 复核补充

本轮继续使用 agent-reach 的 GitHub CLI 路由和网页检索，复核时间为 2026-07-03。

| 项目 | 当前公开状态 | 对 v0.0.54 的落地启发 |
| --- | --- | --- |
| [frappe/hrms](https://github.com/frappe/hrms) | GitHub 显示为 Open Source HR and Payroll Software，2026-07-02 仍在更新，Star 约 8.1k | Frappe HRMS 强调员工与薪酬、人事流程一体化，ONES-ADMIN 需要把员工生命周期从“写入事件”推进到“可查询时间线”，作为详情页和审计的基础 |
| [orangehrm/orangehrm](https://github.com/orangehrm/orangehrm) | GitHub 描述为 comprehensive HRM System，2026-06-30 仍在更新，Star 约 1.0k | OrangeHRM 的核心价值在员工管理和报表，ONES-ADMIN 应保证员工档案不仅展示当前状态，还能追溯入职、调岗、转正、离职历史 |
| [backstage/backstage](https://github.com/backstage/backstage) | GitHub 描述为 building developer portals，2026-07-02 仍在更新，Star 约 33.7k | Backstage 的 API Catalog 思路继续验证接口资产需要 owner、生命周期和可发现性；本次 HRMS 新接口必须同步接入接口治理元数据 |
| [gravitee-io/gravitee-api-management](https://github.com/gravitee-io/gravitee-api-management) | GitHub 描述为 OpenSource API Management，2026-07-02 仍在更新 | Gravitee 的 API Management 思路说明接口需要发布、订阅和生命周期治理；HRMS 新接口继续走 Manifest/Gate 体系，不做“隐藏接口” |
| [Kong/kong](https://github.com/Kong/kong) / [apache/apisix](https://github.com/apache/apisix) | GitHub 均保持高活跃，Kong 约 43.7k Star，APISIX 约 16.8k Star | 网关项目强调运行时策略与接口目录一致；HRMS 生命周期时间线接口必须有 Sa-Token 权限码、权限初始化和接口目录元数据闭环 |

结论：`v0.0.54` 不扩大到合同、考勤、薪酬等大范围模块，而是优先补齐“员工生命周期时间线查询”。这个能力能把 `v0.0.51-v0.0.52` 已写入的生命周期事件转化为可审计、可展示、可被前端详情页消费的企业级能力。

## 8. v0.0.55 合同管理落地补充

2026-07-03 通过 agent-reach GitHub/dev 路由复核 [frappe/hrms](https://github.com/frappe/hrms) 当前仍为活跃的开源 HRMS 参考项目。ONES-ADMIN 本次不复制其实现，只继续吸收“员工档案作为中心资产，合同与生命周期聚合到员工详情”的产品边界。

结论：`v0.0.55` 优先落地员工维度的合同列表、新增、编辑三个核心接口，不一次性扩展合同终止、到期提醒、签署流程和附件文件元数据。这样能先让员工详情页具备合同聚合能力，同时保持后端接口、权限、校验和接口治理闭环可控。

## 9. v0.0.56 合同流程补充

2026-07-03 通过 agent-reach GitHub/dev 路由复核 [orangehrm/orangehrm](https://github.com/orangehrm/orangehrm) 的员工合同边界，公开代码中存在 `EmploymentContractAPI` 与 `EmpContract`，合同挂在员工 PIM 域下，并以开始日期、结束日期和附件为核心信息。

结论：`v0.0.56` 在员工合同核心 CRUD 之后补齐合同终止和即将到期查询，既保持员工档案聚合边界，也为 HR 前台页面的“合同到期提醒”筛选提供后端能力。附件元数据和签署流程仍后置，避免一期合同域过早膨胀。

## 10. v0.0.58 花名册导入补充

2026-07-03 通过 agent-reach GitHub/dev 路由复核 [frappe/hrms](https://github.com/frappe/hrms) 的 Roster 相关公开代码线索。ONES-ADMIN 本次不复制其实现，只吸收“批量操作必须有可追踪批次、错误可回看、成功数据进入正式员工主数据链路”的边界。

结论：`v0.0.58` 先落地 CSV 模板下载、上传导入、导入批次和错误行查询，不急于引入 Excel 解析依赖。这样一期可以在低复杂度下实现可审计、可回滚排查的批量入职入口；导入成功行复用员工新增服务，避免绕开员工编号唯一性、部门/岗位/职级校验、任职记录和生命周期事件。
