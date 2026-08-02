# ONES-ADMIN Login Page Pixel-Perfect Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变现有认证 Store/API/Token 链路的前提下，将 `/auth/login` 重构为参考图在 `1536 × 1024` 下的 4K 清晰度像素级复刻。

**Architecture:** 保留 `AuthPageLayout`、`AuthenticationLogin` 和现有 `authStore` 边界，重写认证布局的可视结构与登录页局部模板/CSS。3D 生态图和品牌标识使用独立高分辨率资产，界面文字、图标、表单与装饰由 DOM/CSS/现有 Iconify 图标库渲染。

**Tech Stack:** Vue 3、TypeScript、Vben Form、Tailwind/CSS、Iconify、Pinia、Playwright、Vite、pnpm 11。

## Global Constraints

- 视觉唯一真值：`/var/folders/tm/gbhh0ggd3bjgbpylhsqfg2x00000gn/T/codex-clipboard-c9d03d6e-d123-47b9-91af-5f1408812461.png`。
- 主验收状态：`1536 × 1024`、浅色、简体中文、账号密码已由测试填入、记住账号勾选、未登录且无错误。
- 位图源尺寸至少为最大显示尺寸的 2 倍；核心 3D 主视觉目标尺寸不低于 `2360 × 1360`，不得有损二次压缩。
- 不使用整页截图作为页面背景，不使用 CSS 图形或手工 SVG 代替图片资产与品牌图标。
- 删除滑块验证码和二维码折角；保留二维码路由、后端限流/锁定以及完整认证链路。
- “记住账号”只保存用户名；禁止硬编码或持久化密码。
- 不新增依赖，除非现有图标库和图像处理工具无法完成，并在安装前检查锁文件兼容性。
- 不修改后端、数据库、全站设计 Token 或生产流水线。

---

## File Structure

- Create: `web/playground/public/brand/ones-brand-mark.png` — 参考图同款透明品牌标识，供页头和登录卡复用。
- Create: `web/playground/public/brand/ones-3d-platform-4k.png` — 4K 清晰度 3D 生态主视觉。
- Modify: `web/packages/effects/layouts/src/authentication/authentication.vue` — 外层画布、双栏、左侧文案/能力/指标和桌面响应式结构。
- Modify: `web/packages/effects/layouts/src/authentication/form.vue` — 右侧背景、卡片尺寸、位置、圆角和阴影。
- Modify: `web/packages/effects/layouts/src/authentication/ones-visual-stage.vue` — 只呈现 4K 主视觉资产，去掉当前 CSS 仿制舞台。
- Modify: `web/packages/effects/layouts/src/authentication/toolbar.vue` — 参考图同款语言/主题工具栏，只保留所需控件。
- Modify: `web/playground/src/layouts/auth.vue` — 收敛当前认证布局传参到新视觉需要的内容。
- Modify: `web/playground/src/views/_core/authentication/login.vue` — 移除滑块，重建卡内品牌头和第三方登录区，保留提交/OAuth 逻辑。
- Modify: `web/packages/locales/src/langs/zh-CN/authentication.json` — 补齐 Google 和安全提示文案。
- Modify: `web/packages/locales/src/langs/en-US/authentication.json` — 同步英文文案，避免切换后溢出。
- Modify: `web/playground/__tests__/e2e/common/auth.ts` — 登录辅助函数不再操作滑块。
- Modify: `web/playground/__tests__/e2e/auth-login.spec.ts` — 新视觉结构、交互和无滑块回归测试。
- Modify: `design-qa.md` — 新参考图、同视口截图、缺陷和最终通过结论。
- Create: `docs/design/login-page-pixel-perfect-1536x1024.png` — 最终同视口实现截图。

---

### Task 1: 锁定新登录页行为验收基线

**Files:**
- Modify: `web/playground/__tests__/e2e/common/auth.ts:5-56`
- Modify: `web/playground/__tests__/e2e/auth-login.spec.ts:1-134`

**Interfaces:**
- Consumes: `/auth/login` 路由、`input[name='username']`、`input[name='password']`、`button[aria-label='login']`。
- Produces: 不依赖滑块的 `authLogin(page: Page)`，以及新页面稳定选择器 `data-testid="login-shell|brand-visual|login-card|social-login-list"`。

- [ ] **Step 1: 修改登录测试辅助函数，移除滑块操作**

删除 `completeSliderCaptcha` 导出和 `authLogin` 内的调用，保留字段填充、点击登录和首页跳转断言：

```ts
export async function authLogin(page: Page) {
  const usernameInput = page.locator(`input[name='username']`);
  const passwordInput = page.locator(`input[name='password']`);
  await expect(usernameInput).toBeVisible();
  await expect(passwordInput).toBeVisible();
  await usernameInput.fill('admin');
  await passwordInput.fill('admin123');
  await page.getByRole('button', { name: 'login' }).click();
  await expect(page).toHaveURL(/\/dashboard\/overview/);
}
```

- [ ] **Step 2: 将旧暗色稿断言改成参考图结构断言**

测试必须至少覆盖：欢迎标题、四项能力、四项指标、三个带文本的第三方入口、安全提示、没有滑块、没有二维码折角、字段 autocomplete、登录卡圆角和无横向溢出。

```ts
await expect(page.getByTestId('login-shell')).toBeVisible();
await expect(page.getByTestId('brand-visual')).toBeVisible();
await expect(page.getByTestId('login-card')).toBeVisible();
await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible();
await expect(page.getByText('新一代企业协同管理平台')).toBeVisible();
await expect(page.getByTestId('social-login-list').getByRole('button')).toHaveCount(3);
await expect(page.locator(`div[name='captcha']`)).toHaveCount(0);
await expect(page.locator('.enterprise-login-qrcode-corner')).toHaveCount(0);
expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(1536);
```

- [ ] **Step 3: 更新失败登录测试**

移除 `completeSliderCaptcha(page)`，直接提交错误密码，继续断言“用户名或密码错误”和“追踪ID”。

- [ ] **Step 4: 运行测试确认新基线先失败**

Run: `cd web/playground && pnpm exec playwright test __tests__/e2e/auth-login.spec.ts --project=chromium`

Expected: FAIL，至少提示 `data-testid="login-shell"` 或新标题/第三方入口不存在；不能因测试语法或服务器启动失败而失败。

- [ ] **Step 5: 提交测试基线**

```bash
git add web/playground/__tests__/e2e/common/auth.ts web/playground/__tests__/e2e/auth-login.spec.ts
git commit -m "test: 更新登录页像素级复刻验收基线"
```

### Task 2: 制作并验证 4K 品牌视觉资产

**Files:**
- Create: `web/playground/public/brand/ones-brand-mark.png`
- Create: `web/playground/public/brand/ones-3d-platform-4k.png`
- Modify: `web/packages/effects/layouts/src/authentication/ones-visual-stage.vue:1-246`

**Interfaces:**
- Consumes: 参考图、现有 `web/playground/public/brand/ones-3d-platform.png`。
- Produces: `/brand/ones-brand-mark.png` 和 `/brand/ones-3d-platform-4k.png`；`OnesVisualStage` 组件继续保持无 props 接口。

- [ ] **Step 1: 从参考图制作独立资产**

使用 Image Gen 图像编辑流程，以参考图和现有 3D PNG 为内容真值，只做高分辨率重建，不改变节点数量、节点文字、构图、视角、配色和透明边缘。品牌标识必须匹配参考图中的四瓣蓝色图形，不使用当前大号 `1S` 图标替代。

- [ ] **Step 2: 检查资产清晰度与透明通道**

Run:

```bash
sips -g pixelWidth -g pixelHeight -g hasAlpha web/playground/public/brand/ones-brand-mark.png web/playground/public/brand/ones-3d-platform-4k.png
```

Expected: 主视觉宽度 `>=2360`、高度 `>=1360`、`hasAlpha: yes`；品牌标识边缘在原始尺寸查看时无白底、锯齿或文字残留。

- [ ] **Step 3: 用真实图片替换 CSS 仿制舞台**

`OnesVisualStage` 只保留语义容器和图片：

```vue
<template>
  <figure class="ones-visual-stage" data-testid="brand-visual">
    <img
      alt="ONES 1S 企业协同生态"
      decoding="async"
      fetchpriority="high"
      height="1360"
      src="/brand/ones-3d-platform-4k.png"
      width="2360"
    />
  </figure>
</template>
```

样式使用 `object-fit: contain`、透明背景和明确的桌面/移动尺寸，不增加滤镜模糊。

- [ ] **Step 4: 运行类型检查**

Run: `cd web && pnpm check:type`

Expected: PASS，无 Vue 模板或 TypeScript 错误。

- [ ] **Step 5: 提交视觉资产**

```bash
git add web/playground/public/brand/ones-brand-mark.png web/playground/public/brand/ones-3d-platform-4k.png web/packages/effects/layouts/src/authentication/ones-visual-stage.vue
git commit -m "feat: 新增登录页4K品牌视觉资产"
```

### Task 3: 重构认证画布与左侧品牌区域

**Files:**
- Modify: `web/packages/effects/layouts/src/authentication/authentication.vue:1-620`
- Modify: `web/packages/effects/layouts/src/authentication/form.vue:1-168`
- Modify: `web/packages/effects/layouts/src/authentication/toolbar.vue:1-49`
- Modify: `web/playground/src/layouts/auth.vue:1-117`

**Interfaces:**
- Consumes: `OnesVisualStage`、`preferences`、`usePreferences()`、`Copyright`、语言/主题 Toggle。
- Produces: `data-testid="login-shell"` 的外层画布、58/42 双栏、左侧品牌/能力/指标和右侧 `RouterView` 卡片容器。

- [ ] **Step 1: 收敛 AuthPageLayout 传参**

删除旧的八节点 `brandFeatures`、四原则 `brandPrinciples` 和 slogan 图片传参；保留 `appName`、logo、标题、副标题、toolbar 与 copyright 的既有公共接口，页面实际品牌图固定使用 `/brand/ones-brand-mark.png`。

- [ ] **Step 2: 重建认证根画布**

根节点增加 `data-testid="login-shell"`，在 `1536 × 1024` 下实现：8px 外边距、约 28px 大圆角、58.3%/41.7% 双栏、外层浅灰蓝和内部浅色背景。主视口不能出现滚动条。

- [ ] **Step 3: 重建顶部栏与左侧正文**

按照参考图顺序输出品牌、`ONES / 1S`、主副标题、四项能力、`OnesVisualStage` 和四项指标。能力图标使用现有 Iconify 图标：`lucide:sparkles`、`lucide:workflow`、`lucide:chart-no-axes-column-increasing`、`lucide:shield-check`。

- [ ] **Step 4: 重建工具栏**

只呈现语言和主题切换；隐藏颜色和布局切换。容器匹配参考图的独立语言胶囊与圆形主题按钮，保留键盘焦点可见性。

- [ ] **Step 5: 对齐右侧表单容器**

`AuthenticationFormView` 在主视口中将卡片限制到约 `478 × 696`，位置约为右栏水平居中、顶部 192px；圆角约 20px，使用白色半透明背景和柔和蓝灰阴影。移除当前内层装饰边和顶端高光线。

- [ ] **Step 6: 增加响应式规则**

`1440 × 900` 和 `1366 × 768` 使用 `clamp()` 压缩间距；小于 `1280px` 隐藏左侧大型主视觉并让登录卡居中；移动端卡片无横向溢出。

- [ ] **Step 7: 运行 E2E，确认布局测试继续因登录卡内容失败而非画布失败**

Run: `cd web/playground && pnpm exec playwright test __tests__/e2e/auth-login.spec.ts --project=chromium`

Expected: `login-shell`、左侧标题、能力与指标断言 PASS；欢迎标题或第三方登录断言仍 FAIL。

- [ ] **Step 8: 提交布局重构**

```bash
git add web/packages/effects/layouts/src/authentication web/playground/src/layouts/auth.vue
git commit -m "feat: 重构登录页浅色双栏画布"
```

### Task 4: 重构登录卡并保留认证行为

**Files:**
- Modify: `web/playground/src/views/_core/authentication/login.vue:1-520`
- Modify: `web/packages/locales/src/langs/zh-CN/authentication.json:73-105`
- Modify: `web/packages/locales/src/langs/en-US/authentication.json:73-105`

**Interfaces:**
- Consumes: `AuthenticationLogin`、`authStore.authLogin(params)`、Feishu/SSO 配置、`IconifyIcon`、`message`。
- Produces: `data-testid="login-card"` 与 `data-testid="social-login-list"`；用户名/密码字段、记住账号、忘记密码、三种第三方入口。

- [ ] **Step 1: 移除滑块字段和失败重置逻辑**

`formSchema` 只保留用户名和密码。`onSubmit` 简化为：

```ts
async function onSubmit(params: Recordable<any>) {
  await authStore.authLogin(params);
}
```

- [ ] **Step 2: 重建登录卡标题区**

标题插槽输出品牌标识、`ONES-ADMIN`、`h2` 欢迎标题和副标题；删除二维码折角。根节点增加 `data-testid="login-card"`。

- [ ] **Step 3: 重建第三方登录区**

飞书调用 `handleFeishuLogin`，企业微信调用 `handleSsoLogin`，Google 调用新增 `handleGoogleLogin` 并显示“尚未配置”提示。图标使用现有 Iconify 官方品牌图标，三项都包含可见文字、`aria-label`、hover/focus 状态。

- [ ] **Step 4: 对齐字段与主按钮**

通过局部 scoped CSS 对齐标签、46px 输入框、10px 圆角、蓝色主按钮、记住账号与忘记密码的间距。不得影响其他认证页面或全局表单组件。

- [ ] **Step 5: 补齐中英文文案**

新增或调整：Google、Google 未配置提示、安全加密提示；中文主状态必须与参考图逐字一致，英文必须完整显示且不溢出。

- [ ] **Step 6: 运行登录页 E2E**

Run: `cd web/playground && pnpm exec playwright test __tests__/e2e/auth-login.spec.ts --project=chromium`

Expected: 所有登录页 E2E PASS，包括成功登录、错误 trace id、无滑块和第三方入口数量。

- [ ] **Step 7: 运行类型检查**

Run: `cd web && pnpm check:type`

Expected: PASS。

- [ ] **Step 8: 提交登录卡重构**

```bash
git add web/playground/src/views/_core/authentication/login.vue web/packages/locales/src/langs/zh-CN/authentication.json web/packages/locales/src/langs/en-US/authentication.json
git commit -m "feat: 完成登录卡像素级复刻"
```

### Task 5: 同视口视觉 QA、回归与交付

**Files:**
- Create: `docs/design/login-page-pixel-perfect-1536x1024.png`
- Modify: `design-qa.md`
- Modify as needed: Task 2–4 中已列出的视觉文件

**Interfaces:**
- Consumes: 参考图、运行中的 `/auth/login`、Playwright/Chromium。
- Produces: 同视口截图和 `final result: passed` 的 `design-qa.md`。

- [ ] **Step 1: 启动本地前端并打开真实页面**

Run: `cd web && pnpm dev -- --port 5555`

在 Chromium 打开 `http://localhost:5555/auth/login`，切换浅色、简体中文，填入测试账号和密码，勾选记住账号。

- [ ] **Step 2: 按主视口截图**

固定 viewport `1536 × 1024`，截图保存到 `docs/design/login-page-pixel-perfect-1536x1024.png`。

- [ ] **Step 3: 执行阻塞式设计 QA**

同时打开参考图和实现截图，逐项比较：外框、分界、页头、字体层级、四项能力、3D 图、指标卡、登录卡、字段、第三方入口、版权、颜色和阴影。所有 P0/P1/P2 必须修复；用户要求一模一样，因此肉眼可见的 P3 也继续修复。

- [ ] **Step 4: 更新 design-qa.md**

写明 reference path、implementation path、viewport、state、逐项 findings 和 `final result: passed`。若截图或真实浏览器验证无法完成，结果必须写 `blocked`，不得宣称完成。

- [ ] **Step 5: 运行最终验证**

Run:

```bash
cd web && pnpm check:type
cd web && pnpm build
cd web/playground && pnpm exec playwright test __tests__/e2e/auth-login.spec.ts --project=chromium
```

Expected: 三项全部 PASS；构建若只有既有依赖警告，需要在交付中明确区分警告与失败。

- [ ] **Step 6: 检查改动范围与敏感信息**

Run:

```bash
git diff --check
git status --short
rg -n "admin123|password\s*[:=]" web/playground/src web/playground/public/brand docs/design design-qa.md
```

Expected: `git diff --check` 无输出；源码和新增资产/文档中没有硬编码测试密码。

- [ ] **Step 7: 提交视觉验收证据**

```bash
git add docs/design/login-page-pixel-perfect-1536x1024.png design-qa.md
git commit -m "test: 完成登录页像素级视觉验收"
```
