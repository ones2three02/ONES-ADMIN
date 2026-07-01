# ONES-ADMIN 分支与 Jenkins 规划

## 分支模型

ONES-ADMIN 使用 `main + develop + 短生命周期分支` 的企业级分支模型。

| 分支 | 生命周期 | 用途 | Jenkins 行为 |
| --- | --- | --- | --- |
| `main` | 长期 | 生产稳定分支，只接收已验证版本 | 构建生产制品，生产部署建议人工确认 |
| `develop` | 长期 | 日常集成分支，作为开发环境的主干 | 自动构建、测试、部署开发环境 |
| `feature/*` | 短期 | 单个功能或模块开发 | 运行构建、测试、静态检查，不自动部署生产 |
| `release/*` | 短期 | 预发布稳定分支，例如 `release/0.2.0` | 构建候选版本，部署测试/预发环境 |
| `hotfix/*` | 短期 | 生产紧急修复，例如 `hotfix/login-lock` | 构建、测试，通过后合并 `main` 和 `develop` |

## 合并规则

1. 功能开发从 `develop` 拉分支：`feature/模块名-功能名`。
2. 功能完成后合并回 `develop`。
3. 准备发版时从 `develop` 拉 `release/x.y.z`。
4. `release/x.y.z` 验证通过后合并到 `main`，并打版本标签 `vX.Y.Z`。
5. `main` 的生产修复从 `main` 拉 `hotfix/*`，修复后同时合并回 `main` 和 `develop`。

## 版本号规则

- ONES-ADMIN 产品版本使用 `vMAJOR.MINOR.PATCH` 格式，例如 `v0.0.1`。
- 每批可交付变更都必须同步更新根目录 `VERSION` 和 `docs/CHANGELOG.md`。
- 后端 Maven 版本与产品版本保持一致，但不带 `v`，开发分支使用 `-SNAPSHOT`，例如 `0.0.1-SNAPSHOT`。
- OpenAPI 展示版本读取后端配置 `ones.version`，默认与根目录 `VERSION` 保持一致。
- 前端 `web/package.json` 当前保留 Vben 上游框架版本，不作为 ONES-ADMIN 产品版本来源。
- 兼容性修复和小优化递增 PATCH；新增模块能力递增 MINOR；破坏性变更递增 MAJOR。

## Jenkins 推荐流水线

### feature/* 或 Pull Request

- 后端构建节点配置 JDK 21+；Maven Enforcer 会在 `validate` 阶段提前拦截错误 JDK 或 Maven 版本。
- 后端：`./mvnw test`
- 前端：`pnpm -F @vben/playground run typecheck`
- 前端：`pnpm -F @vben/playground run build`
- 不部署，只作为质量门禁。

### develop

- 执行完整构建与测试。
- 后端使用 JDK 21+ 和项目内 `./mvnw`，避免 Jenkins 节点默认 Java 版本漂移。
- 生成开发环境制品。
- 自动部署到开发环境。
- 适合连接开发数据库、开发 Redis、开发 RabbitMQ。
- 建议部署后调用接口治理门禁、最新快照门禁干跑和 Manifest 发布快照接口，将当前接口契约固化为下一次发布的对比基线。
- Manifest Gate 返回 `checks` 机器可读检查项，Jenkins 报告应优先按 `checkCode`、`passed`、`blocking`、`remediation` 输出门禁明细，避免解析中文 `reasons`。

### release/*

- 执行完整构建与测试。
- 生成候选制品。
- 自动部署到测试/预发环境。
- 禁止合并新的大功能，只允许缺陷修复、配置调整、文档补充。

### main

- 执行完整构建与测试。
- 生成生产制品。
- 生产部署建议设置 Jenkins 人工确认步骤。
- 合并到 `main` 后建议打标签，例如 `v0.1.0`。

### hotfix/*

- 从 `main` 创建。
- 只修复生产紧急问题。
- 验证通过后合并回 `main`，并同步回 `develop`。

## 保护规则建议

建议在 GitHub 仓库设置分支保护：

| 分支 | 保护建议 |
| --- | --- |
| `main` | 禁止直接 push；必须通过 PR；必须通过 Jenkins；至少 1 人审核；允许管理员按需绕过 |
| `develop` | 禁止直接 push；必须通过 Jenkins；可按团队阶段决定是否强制审核 |

## 提交规范

提交信息默认使用 Conventional Commits：

- `feat: 新增功能`
- `fix: 修复问题`
- `refactor: 重构实现`
- `docs: 更新文档`
- `test: 补充测试`
- `chore: 调整工程配置`

## 当前初始化状态

- `main`：当前稳定基线，包含已完成的 Vben 前端、Spring Boot 后端、Sa-Token 登录、权限、菜单、用户、角色、部门基础能力。
- `develop`：从当前 `main` 创建，作为后续日常开发集成分支。
