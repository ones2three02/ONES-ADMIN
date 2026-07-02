**Findings**
- 无 P0/P1/P2 问题。
  位置：`/auth/login` 桌面暗色登录页。
  证据：参考图强调 ONES / 1S 系统核心视觉、企业级品牌入口、登录表单容器和安全能力表达；当前实现保留 Vben 认证布局，并将右侧升级为正式登录卡片，左侧改为完整 1S 核心舞台。
  影响：登录页品牌感、表单正式感和企业级安全入口表达提升，没有破坏现有登录提交流程。
  修复：无阻塞项。

**Open Questions**
- 本次按桌面暗色态完成落地和验证。浅色态仍沿用同一布局和品牌资产，可在后续版本继续精修独立浅色 1S 视觉资产。

**Implementation Checklist**
- 保留 Vben `AuthPageLayout`、`AuthenticationLogin`、工具栏、国际化和表单提交流程。
- 右侧新增品牌登录卡片，包含 ONES-ADMIN 锁定、`SECURE ACCESS`、安全登录标题、认证/锁定/审计三项状态和安全锁定提示。
- 移除演示型“快速选择账号”下拉，登录 E2E 改为显式输入 `admin / admin123`。
- 左侧 1S 视觉取消圆形裁切，改为完整发光舞台并使用柔边遮罩融入背景。
- 装饰轨道和登录卡片光晕不再撑大滚动宽度，桌面视口横向溢出检查为 0。

**Verification**
- `typecheck`：通过。
- `build`：通过；仅出现依赖包自身 Rolldown pure annotation 与 BigInt target 兼容警告，构建成功。
- `server ./mvnw test`：通过，46 个测试全部成功。
- `playwright auth-login.spec.ts`：通过，4 个登录页 E2E 用例全部成功。
- 浏览器截图：通过，输出 `docs/design/login-page-v0.0.48-implementation.png`。
- DOM 可见性检查：`安全登录`、`ONES / 1S`、`1S 执行核心`、`ONE SYSTEM`、`连续失败将触发临时锁定` 均可见。
- 演示入口检查：`快速选择账号` 不再出现。
- 文本/布局溢出检查：`overflowCount=0`。

source visual truth path: `/var/folders/tm/gbhh0ggd3bjgbpylhsqfg2x00000gn/T/codex-clipboard-5267a29f-279a-4cc7-bf15-a49ce6cd6d42.png`
implementation screenshot path: `docs/design/login-page-v0.0.48-implementation.png`
viewport: `1366x768`
state: `暗色模式，/auth/login，未登录`
final result: passed
