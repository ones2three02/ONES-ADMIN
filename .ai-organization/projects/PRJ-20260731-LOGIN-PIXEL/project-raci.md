# Project RACI Matrix (项目任务与责任追踪矩阵)

- **项目名称**: ONES-ADMIN 登录页像素级复刻
- **项目 ID**: PRJ-20260731-LOGIN-PIXEL
- **项目经理**: project-manager-01
- **更新日期**: 2026-07-31

---

## 一、项目团队成员与角色映射 (Team Mapping)

| 缩写 | 姓名/ID | 角色 | 所属部门 |
| :--- | :--- | :--- | :--- |
| **PM** | project-manager-01 | Project Manager | PMO |
| **FE** | frontend-engineer-01 | Frontend Engineer | Engineering |
| **QA** | qa-engineer-01 | QA Engineer | QA |
| **OM** | org-manager-01 | Organization Manager | Executive |

---

## 二、任务分解与责任矩阵 (WBS & RACI Matrix)

用以下状态标识完成情况：`[ ]` 未启动 / `[/]` 进行中 / `[x]` 已完成。

| 任务说明 (WBS Task) | PM | FE | QA | OM | 状态 |
| :--- | :---: | :---: | :---: | :---: | :---: |
| 1. 锁定新登录页行为验收基线（测试辅助函数与登录页 E2E 基线） | **A** | **R** | **C** | **I** | `[/]` |
| 2. 制作并验证 4K 品牌视觉资产（品牌标识与 3D 主视觉） | **A** | **R** | **C** | **I** | `[ ]` |
| 3. 重构认证画布与左侧品牌区域（双栏布局、工具栏、左侧主视觉） | **A** | **R** | **C** | **I** | `[ ]` |
| 4. 重构登录卡并保留认证行为（字段、记住账号、第三方入口、文案） | **A** | **R** | **C** | **I** | `[ ]` |
| 5. 最终视觉与功能验证（叠图、E2E、构建、design-qa 结论） | **A** | **C** | **R** | **I** | `[ ]` |

- **R (Responsible)**: 执行者（具体负责交付该任务的角色）。
- **A (Accountable)**: 终责人（对该任务结果负最终责任并批准验收）。
- **C (Consulted)**: 咨询人（提供输入、评审与反馈）。
- **I (Informed)**: 知会人（接收阶段结果与项目验收通知）。
