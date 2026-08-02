# Job Request（前端工程师岗位招聘申请单）

- **申请单编号**: REQ-20260731-FE01
- **发送者**: org-manager-01
- **接收者**: hr-manager-01
- **关联项目 ID**: PRJ-20260731-LOGIN-PIXEL
- **任务优先级**: high
- **申请部门**: Architecture & Engineering
- **发起 Agent**: org-manager-01

## 一、招聘基本信息

| 项目 | 内容 |
| :--- | :--- |
| **招聘岗位** | Frontend Engineer（前端工程师） |
| **编制类型** | 临时项目调用（Consultant，项目期间保持 active） |
| **直属上级** | project-manager-01 / Project Manager |
| **所属项目** | PRJ-20260731-LOGIN-PIXEL / ONES-ADMIN 登录页像素级复刻 |
| **拟分配 Agent ID** | frontend-engineer-01 |

## 二、招聘原因（Rationale）

项目需要在 Vue 3、Vite、TypeScript、Vben 与 Ant Design Vue 技术栈中完成登录页像素级复刻，并保持现有认证 Store/API 主链路、响应式行为、国际化、可访问性和视觉验证闭环。经核对 `.ai-organization/registry/agent-registry.yaml`，当前活跃成员中无人持有 `frontend-development` 能力，存在明确能力缺口，依据组织政策启动招聘。

## 三、能力与资格要求（Required Capabilities）

- **必备能力**: `frontend-development`
- **经验设定要求**:
  - 熟悉 Vue 3、Vite、TypeScript、现代 CSS、响应式布局与组件状态管理。
  - 能在不绕过现有 `authStore.authLogin → loginApi → POST /api/auth/login → completeLogin` 链路的前提下实现登录交互。
  - 能依据固定参考图与 `1536 × 1024` 主验收视口开展截图、叠图和 Playwright 视觉/功能验证。
  - 理解局部页面样式与全站设计系统的授权边界，不擅自修改后端接口、权限模型或全局视觉规范。
  - 能主动执行重复提交保护、错误与 TraceId 展示、资源降级和敏感信息保护。

## 四、HR 部门审核意见

- **审核人**: hr-manager-01
- **审核结果**: 同意招聘
- **审核意见**: 能力缺口真实存在，岗位、能力标签与 `role-registry.yaml`、`capability-registry.yaml` 一致；直属上级和项目范围明确，同意进入三候选人寻访与结构化面试流程。

## 五、执行日志（Execution Log）

```yaml
timestamp: "2026-07-31 23:10:22"
agent_id: "hr-manager-01"
action: "create_job_request"
target: ".ai-organization/projects/PRJ-20260731-LOGIN-PIXEL/job-request-frontend.md"
rationale: "登记 frontend-development 能力缺口并批准前端工程师招聘"
status: "success"
```
