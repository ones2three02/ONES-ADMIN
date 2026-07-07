# ONES-ADMIN 分支与 Jenkins 规划

## 分支模型

ONES-ADMIN 使用 `main + develop + 短生命周期分支` 的企业级分支模型。

| 分支 | 生命周期 | 用途 | Jenkins 行为 |
| --- | --- | --- | --- |
| `main` | 长期 | 生产稳定分支，只接收已验证版本 | 构建生产制品，生产部署建议人工确认 |
| `develop` | 长期 | 日常集成分支，作为开发环境的主干 | 当前阶段自动验证；部署待环境与凭据策略确认后接入 |
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

当前仓库已在 v0.0.129 新增根目录 `Jenkinsfile`，在 v0.0.130 补齐 `scripts/ci/version-guard.sh` 版本一致性门禁，在 v0.0.131 新增 `scripts/ci/verify.sh` 统一本地与 Jenkins 的阶段入口，在 v0.0.132 新增 `scripts/ci/build-metadata.sh` 输出构建元数据，在 v0.0.133 新增 `scripts/ci/verification-summary.sh` 输出机器可读验证摘要，在 v0.0.134 新增接口治理报告归档阶段，在 v0.0.135 新增数据库迁移审计报告归档阶段，在 v0.0.136 新增发布证据包归档阶段，并在 v0.0.137 新增环境配置审计报告归档阶段。这个流水线是第一阶段验证门禁，只负责安装前端锁定依赖、测试、类型检查、构建、版本一致性检查、版本残留扫描、敏感信息扫描、构建元数据归档、环境配置审计报告归档、数据库迁移审计报告归档、接口治理报告归档、验证摘要归档和发布证据归档；仓库扫描规则沉淀在 `scripts/ci/repository-guard.sh`。当前流水线不包含任何部署动作，也不写入数据库、Redis、RabbitMQ、MinIO 等中间件连接信息。

### feature/* 或 Pull Request

- 后端构建节点配置 JDK 21+；Maven Enforcer 会在 `validate` 阶段提前拦截错误 JDK 或 Maven 版本。
- 后端：`./mvnw test`
- 前端：`pnpm -F @vben/playground run typecheck`
- 前端：`pnpm -F @vben/playground run build`
- 不部署，只作为质量门禁。

### develop

- 执行 `Jenkinsfile` 中的验证门禁。
- 后端使用 JDK 21+ 和项目内 `./mvnw`，避免 Jenkins 节点默认 Java 版本漂移。
- 执行前端锁定依赖安装、Playground 包级单测、根级单测、类型检查、后端测试和前端构建。
- 执行版本一致性检查、版本残留扫描和敏感信息扫描，避免版本号漂移、旧版本号或本地中间件凭据进入仓库。
- 生成 `.ci-artifacts/build-metadata.json` 并作为 Jenkins 构建产物归档，用于追踪产品版本、Git 提交、分支和工具链版本；该文件不记录中间件地址、账号密码、Git 远端 URL 或 Jenkins 内部 URL。
- 生成 `.ci-artifacts/environment-config-report.json` 并作为 Jenkins 构建产物归档，用于留存后端与前端环境变量清单、生产必填项、占位值、本地默认值和敏感项审计结论；该阶段只扫描可提交配置模板，不读取 ignored 本地敏感文件，不输出真实密码或 Secret。
- 生成 `.ci-artifacts/database-migration-report.json` 并作为 Jenkins 构建产物归档，用于留存 Flyway 迁移版本连续性、命名规范、旧 `schema.sql` 禁用、破坏性 SQL 审批和迁移脚本指纹状态；该阶段只扫描仓库文件，不连接真实数据库。
- 生成 `.ci-artifacts/api-governance-report.json` 并作为 Jenkins 构建产物归档，用于留存接口治理聚合报告、质量分、发布准备度、Manifest 指纹和治理动作项。
- 生成 `.ci-artifacts/verification-summary.json` 并作为 Jenkins 构建产物归档，用于汇总轻量门禁、环境配置治理、数据库迁移治理、接口治理、后端测试报告数量、测试结果统计和前端构建产物状态。
- 生成 `.ci-artifacts/release-evidence.json` 并作为 Jenkins 构建产物归档，用于汇总构建来源、环境配置治理、数据库迁移治理、接口治理、测试和前端构建证据，输出 `READY_FOR_ARTIFACT_PROMOTION` 或 `BLOCKED` 结论；该阶段只做证据汇总，不部署、不连接真实中间件。
- 本地可直接执行 `bash scripts/ci/verify.sh all` 复现 Jenkins 当前门禁。
- 当前阶段不自动部署开发环境；后续接入部署前，需要先确认服务器地址、凭据管理、制品路径、回滚策略和人工审批边界。
- 后续接入部署后，优先调用接口治理聚合报告、最新快照门禁干跑和 Manifest 发布快照接口，将当前接口契约固化为下一次发布的对比基线。
- 接口治理聚合报告 `/api/system/api-resources/governance/report` 会一次性返回 summary、governance、rules、manifest 和 latestGate，适合作为 Jenkins 报告和前端接口管理页的数据源。
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
