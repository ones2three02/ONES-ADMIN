# 登录页 Design QA

## 对比目标与标准化

- Source visual truth: `/var/folders/tm/gbhh0ggd3bjgbpylhsqfg2x00000gn/T/codex-clipboard-c9d03d6e-d123-47b9-91af-5f1408812461.png`
- Implementation screenshot: `docs/design/login-page-pixel-perfect-1536x1024.png`
- Full-view comparison: `docs/design/login-page-comparison-1536x1024.png`
- Focused login-card comparison: `docs/design/login-card-comparison-478x696.png`
- Focused 3D-visual comparison: `docs/design/login-visual-comparison-850x490.png`
- Source pixels: `1535 × 1024`
- Implementation pixels: `1536 × 1024`
- CSS viewport: `1536 × 1024`
- Device scale factor: `1`
- Normalization: 源图比目标视口少 1px；叠图仅裁掉实现截图最右侧 1px，使左右对比区域均为 `1535 × 1024`，未缩放、未改变密度。
- State: 简体中文、浅色主题、用户名 `admin`、运行时测试密码、勾选“记住账号”。测试密码仅在截图脚本运行时填入，未写入源码或仓库配置。

## Findings

- 无 P0 / P1 / P2 问题。
- [P3] 4K 重建资产比模糊源截图更锐利、对比度略高。
  - Location: 左侧 3D 企业协同生态图、品牌图标与社交图标。
  - Evidence: 聚焦对比中，结构、节点、连线和占位一致；实现侧边缘更清晰，源图的极淡地面网格更明显。
  - Impact: 不影响构图、识别或交互，且符合用户明确提出的 4K 清晰度要求。
  - Fix: 无需回退到模糊资产；将极淡地面网格差异保留为非阻断润色项。

## 必查保真面

- Fonts and typography: 中文系统字体层级、字号、字重、行高、字段标签与按钮文字已对齐；英文切换后标题和卡片文案完整显示，无截断与横向溢出。
- Spacing and layout rhythm: `1536 × 1024` 下双栏比例、`478 × 696` 卡片、30px 卡片内边距、46px 输入框、按钮、分隔线、社交入口与底部版权位置已对齐；`1440 × 900`、`1366 × 768`、`390 × 844` 均无页面级溢出。
- Colors and visual tokens: 浅蓝灰画布、白色卡片、深蓝文字、蓝色主按钮、灰蓝辅助文本和阴影层级与参考一致；默认浅色不覆盖已有用户主题，主题按钮可切换暗色并恢复。
- Image quality and asset fidelity: 主品牌、3D 平台、飞书和企业微信均为独立高分辨率 PNG，未使用 emoji、CSS 图形、占位图或手绘 SVG；透明边缘无可见白边。
- Copy and content: 中文主状态逐字匹配“新一代企业协同管理平台”“欢迎回来”“用户名”“记住账号”“其他登录方式”“安全加密登录，保障数据安全”；中英文切换可用。
- Icons: 用户、密码、眼睛、语言、主题、安全、能力与第三方入口图标齐全；社交图标和参考图同为飞书、企业微信、Google 品牌语义。
- Accessibility and interactions: 输入框具备 label、aria-label 和 autocomplete；社交按钮具备可见文字、aria-label、hover/focus；语言菜单、主题切换和三项未配置提示均已实际操作验证。

## Comparison history

### Iteration 1 — blocked

- Findings: 卡片偏右约 17px、内边距 40px 而非约 30px；字段标签横排；飞书图标未渲染；版权顺序与参考不一致。
- Fixes: 卡片局部左移、内边距校准为 30px、字段强制纵向排列、替换真实飞书资产、版权改为“Copyright → 公司 → ICP”。
- Post-fix evidence: `docs/design/login-page-comparison-1536x1024.png`。

### Iteration 2 — blocked

- Findings: 移动整个右侧面板导致分栏线偏移；字段缺少用户/锁图标；“账号”未逐字匹配“用户名”；企业微信图标风格不符；`1366 × 768` 卡片存在 24px 内部滚动。
- Fixes: 只移动卡片、不移动面板；加入真实 Lucide 字段图标；文案改为“用户名”；加入独立企业微信高分辨率资产；低高度断点压缩 24px 垂直空间。
- Post-fix evidence: `docs/design/login-card-comparison-478x696.png`，并实测 `1366 × 768` 卡片 `clientHeight 608 / scrollHeight 610`（2px 边框差，无内容溢出）。

### Iteration 3 — passed

- Full-view evidence: `docs/design/login-page-comparison-1536x1024.png`。
- Focused evidence: `docs/design/login-card-comparison-478x696.png` 与 `docs/design/login-visual-comparison-850x490.png`。
- Result: 无可执行 P0 / P1 / P2 差异；仅保留符合 4K 清晰度目标的 P3 锐度差异。

## Functional evidence

- 视口探针：`1536 × 1024`、`1440 × 900`、`1366 × 768`、`390 × 844` 均为 `scrollWidth === viewport width`、`scrollHeight === viewport height`；移动端隐藏大型主视觉并保留完整登录卡。
- 主题：等待切换动画后，根节点从浅色切换到暗色，再可恢复。
- 语言：菜单包含简体中文与 English；切换 English 后 `Welcome Back` 与英文主标题完整可见，页面宽度仍为 1536px。
- 第三方入口：飞书、企业微信和 Google 未配置时分别显示明确提示，不伪造成功登录。
- Browser-rendered console: 不触发登录请求的视觉与交互探针无 console error。

## Final result

final result: passed
