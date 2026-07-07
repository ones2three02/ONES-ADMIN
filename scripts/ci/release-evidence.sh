#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_FILE="${1:-$ROOT_DIR/.ci-artifacts/release-evidence.json}"

mkdir -p "$(dirname "$OUTPUT_FILE")"

if ! command -v node >/dev/null 2>&1; then
  echo "Required command not found: node" >&2
  exit 1
fi

export ROOT_DIR
export OUTPUT_FILE

node <<'NODE'
const fs = require('node:fs');
const path = require('node:path');

const rootDir = process.env.ROOT_DIR;
const outputFile = process.env.OUTPUT_FILE;
const productVersion = fs.readFileSync(path.join(rootDir, 'VERSION'), 'utf8').trim();
const generatedAtUtc = new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');

function artifact(relativePath) {
  return path.join(rootDir, relativePath);
}

function readJson(relativePath) {
  const filePath = artifact(relativePath);
  if (!fs.existsSync(filePath)) {
    return null;
  }

  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function artifactStatus(code, relativePath, exists, version, versionMatches, passed, details = {}) {
  return {
    code,
    path: relativePath,
    exists,
    version: version ?? null,
    versionMatches,
    passed,
    details,
  };
}

function gate(code, passed, message, remediation) {
  return {
    code,
    passed,
    message,
    remediation,
  };
}

const buildMetadataPath = '.ci-artifacts/build-metadata.json';
const environmentConfigReportPath = '.ci-artifacts/environment-config-report.json';
const databaseMigrationReportPath = '.ci-artifacts/database-migration-report.json';
const frontendLayoutReportPath = '.ci-artifacts/frontend-layout-report.json';
const apiGovernanceReportPath = '.ci-artifacts/api-governance-report.json';
const verificationSummaryPath = '.ci-artifacts/verification-summary.json';

const buildMetadata = readJson(buildMetadataPath);
const environmentConfigReport = readJson(environmentConfigReportPath);
const databaseMigrationReport = readJson(databaseMigrationReportPath);
const frontendLayoutReport = readJson(frontendLayoutReportPath);
const apiGovernanceReport = readJson(apiGovernanceReportPath);
const verificationSummary = readJson(verificationSummaryPath);

const buildMetadataVersionMatches = buildMetadata?.product?.version === productVersion;
const environmentConfigVersionMatches = environmentConfigReport?.product?.version === productVersion;
const databaseMigrationVersionMatches = databaseMigrationReport?.product?.version === productVersion;
const frontendLayoutVersionMatches = frontendLayoutReport?.product?.version === productVersion;
const apiGovernanceVersionMatches = apiGovernanceReport?.applicationVersion === productVersion;
const verificationSummaryVersionMatches = verificationSummary?.product?.version === productVersion;

const buildMetadataPassed = Boolean(buildMetadata)
  && buildMetadataVersionMatches
  && typeof buildMetadata.git?.commit === 'string'
  && /^[a-f0-9]{40}$/.test(buildMetadata.git.commit)
  && typeof buildMetadata.git?.shortCommit === 'string'
  && buildMetadata.git.shortCommit.length >= 7;
const databaseMigrationPassed = Boolean(databaseMigrationReport)
  && databaseMigrationVersionMatches
  && databaseMigrationReport.governance?.passed === true
  && databaseMigrationReport.migration?.contiguous === true
  && databaseMigrationReport.migration?.legacySchemaSqlExists === false
  && Array.isArray(databaseMigrationReport.migration?.destructiveViolations)
  && databaseMigrationReport.migration.destructiveViolations.length === 0;
const environmentConfigPassed = Boolean(environmentConfigReport)
  && environmentConfigVersionMatches
  && environmentConfigReport.governance?.passed === true
  && Number(environmentConfigReport.configuration?.totalItemCount ?? 0) > 0
  && Number(environmentConfigReport.configuration?.uniqueEnvCount ?? 0) > 0
  && environmentConfigReport.scanPolicy?.readsIgnoredSensitiveSources === false;
const frontendLayoutPassed = Boolean(frontendLayoutReport)
  && frontendLayoutVersionMatches
  && frontendLayoutReport.governance?.passed === true
  && Number(frontendLayoutReport.governance?.errorCount ?? 1) === 0
  && frontendLayoutReport.layout?.splitLayoutExists === true
  && Number(frontendLayoutReport.layout?.splitLayoutUserCount ?? 0) >= 3
  && frontendLayoutReport.scanPolicy?.readsIgnoredSensitiveSources === false;
const apiGovernancePassed = Boolean(apiGovernanceReport)
  && apiGovernanceVersionMatches
  && apiGovernanceReport.governance?.passed === true
  && Number(apiGovernanceReport.qualityScore) >= 95
  && typeof apiGovernanceReport.manifest?.checksum === 'string'
  && /^[a-f0-9]{64}$/.test(apiGovernanceReport.manifest.checksum);
const verificationSummaryPassed = Boolean(verificationSummary)
  && verificationSummaryVersionMatches
  && verificationSummary.overall?.passed === true
  && verificationSummary.overall?.failedGateCount === 0;

const statuses = [
  artifactStatus(
    'BUILD_METADATA',
    buildMetadataPath,
    Boolean(buildMetadata),
    buildMetadata?.product?.version,
    buildMetadataVersionMatches,
    buildMetadataPassed,
    {
      branch: buildMetadata?.git?.branch ?? null,
      commit: buildMetadata?.git?.commit ?? null,
      shortCommit: buildMetadata?.git?.shortCommit ?? null,
      dirty: buildMetadata?.git?.dirty ?? null,
      toolchain: buildMetadata?.toolchain ?? null,
    },
  ),
  artifactStatus(
    'ENVIRONMENT_CONFIG_REPORT',
    environmentConfigReportPath,
    Boolean(environmentConfigReport),
    environmentConfigReport?.product?.version,
    environmentConfigVersionMatches,
    environmentConfigPassed,
    {
      totalItemCount: environmentConfigReport?.configuration?.totalItemCount ?? null,
      uniqueEnvCount: environmentConfigReport?.configuration?.uniqueEnvCount ?? null,
      productionRequiredCount: environmentConfigReport?.configuration?.productionRequiredCount ?? null,
      localDefaultCount: environmentConfigReport?.configuration?.localDefaultCount ?? null,
      placeholderDefaultCount: environmentConfigReport?.configuration?.placeholderDefaultCount ?? null,
      governancePassed: environmentConfigReport?.governance?.passed ?? null,
      errorCount: environmentConfigReport?.governance?.errorCount ?? null,
      readsIgnoredSensitiveSources: environmentConfigReport?.scanPolicy?.readsIgnoredSensitiveSources ?? null,
    },
  ),
  artifactStatus(
    'DATABASE_MIGRATION_REPORT',
    databaseMigrationReportPath,
    Boolean(databaseMigrationReport),
    databaseMigrationReport?.product?.version,
    databaseMigrationVersionMatches,
    databaseMigrationPassed,
    {
      fileCount: databaseMigrationReport?.migration?.fileCount ?? null,
      latestVersion: databaseMigrationReport?.migration?.latestVersion ?? null,
      contiguous: databaseMigrationReport?.migration?.contiguous ?? null,
      governancePassed: databaseMigrationReport?.governance?.passed ?? null,
      errorCount: databaseMigrationReport?.governance?.errorCount ?? null,
    },
  ),
  artifactStatus(
    'FRONTEND_LAYOUT_REPORT',
    frontendLayoutReportPath,
    Boolean(frontendLayoutReport),
    frontendLayoutReport?.product?.version,
    frontendLayoutVersionMatches,
    frontendLayoutPassed,
    {
      splitLayoutUserCount: frontendLayoutReport?.layout?.splitLayoutUserCount ?? null,
      autoHeightGridFileCount: frontendLayoutReport?.layout?.autoHeightGridFileCount ?? null,
      governancePassed: frontendLayoutReport?.governance?.passed ?? null,
      errorCount: frontendLayoutReport?.governance?.errorCount ?? null,
      warningCount: frontendLayoutReport?.governance?.warningCount ?? null,
    },
  ),
  artifactStatus(
    'API_GOVERNANCE_REPORT',
    apiGovernanceReportPath,
    Boolean(apiGovernanceReport),
    apiGovernanceReport?.applicationVersion,
    apiGovernanceVersionMatches,
    apiGovernancePassed,
    {
      qualityScore: apiGovernanceReport?.qualityScore ?? null,
      governancePassed: apiGovernanceReport?.governance?.passed ?? null,
      releaseStatus: apiGovernanceReport?.releaseReadiness?.status ?? null,
      manifestChecksum: apiGovernanceReport?.manifest?.checksum ?? null,
    },
  ),
  artifactStatus(
    'VERIFICATION_SUMMARY',
    verificationSummaryPath,
    Boolean(verificationSummary),
    verificationSummary?.product?.version,
    verificationSummaryVersionMatches,
    verificationSummaryPassed,
    {
      gateCount: Array.isArray(verificationSummary?.gates) ? verificationSummary.gates.length : null,
      failedGateCount: verificationSummary?.overall?.failedGateCount ?? null,
      backendTests: verificationSummary?.artifacts?.backendTests?.tests ?? null,
      frontendBuildFiles: verificationSummary?.artifacts?.frontendBuild?.fileCount ?? null,
    },
  ),
];

const releaseGates = [
  gate(
    'ALL_REQUIRED_ARTIFACTS_PRESENT',
    statuses.every((status) => status.exists),
    '发布证据包必须包含构建元数据、环境配置报告、数据库迁移报告、前端布局治理报告、接口治理报告和验证摘要',
    '先执行 build-metadata、environment-config-report、database-migration-report、frontend-layout-report、api-governance-report 和 verification-summary 阶段',
  ),
  gate(
    'ALL_ARTIFACT_VERSIONS_MATCH',
    statuses.every((status) => status.versionMatches),
    '所有 CI 产物版本必须与根目录 VERSION 一致',
    '重新生成版本不一致的 CI 产物，避免交付证据串到旧版本',
  ),
  gate(
    'BUILD_SOURCE_TRACEABLE',
    buildMetadataPassed,
    '构建来源必须能追溯到明确 Git 提交和工具链版本',
    '重新生成 build-metadata，并确认 Git 提交、分支和工具链信息可读',
  ),
  gate(
    'ENVIRONMENT_CONFIG_GOVERNED',
    environmentConfigPassed,
    '环境配置治理必须通过',
    '处理生产必填配置缺失、敏感默认值或环境变量清单异常问题',
  ),
  gate(
    'DATABASE_MIGRATION_GOVERNED',
    databaseMigrationPassed,
    '数据库迁移治理必须通过',
    '修复 Flyway 命名、连续版本、旧 schema.sql 或未审批破坏性 SQL 问题',
  ),
  gate(
    'FRONTEND_LAYOUT_GOVERNED',
    frontendLayoutPassed,
    '前端布局治理必须通过',
    '修复左右分栏旧布局类、共享分栏表格高度或布局报告版本不一致问题',
  ),
  gate(
    'API_GOVERNANCE_READY',
    apiGovernancePassed,
    '接口治理报告必须通过且质量分不低于 95',
    '处理接口治理错误、Manifest 指纹异常或发布准备度阻断项',
  ),
  gate(
    'VERIFICATION_SUMMARY_PASSED',
    verificationSummaryPassed,
    '验证摘要必须通过所有门禁',
    '重新运行失败阶段并生成 verification-summary',
  ),
];

const releaseReady = releaseGates.every((item) => item.passed);
const report = {
  product: {
    name: 'ONES-ADMIN',
    version: productVersion,
  },
  generatedAtUtc,
  evidence: {
    artifacts: statuses,
    source: {
      branch: buildMetadata?.git?.branch ?? null,
      commit: buildMetadata?.git?.commit ?? null,
      shortCommit: buildMetadata?.git?.shortCommit ?? null,
      dirty: buildMetadata?.git?.dirty ?? null,
    },
    quality: {
      backendTests: verificationSummary?.artifacts?.backendTests ?? null,
      frontendBuild: verificationSummary?.artifacts?.frontendBuild ?? null,
      environmentConfig: verificationSummary?.artifacts?.environmentConfigReport ?? null,
      databaseMigration: verificationSummary?.artifacts?.databaseMigrationReport ?? null,
      frontendLayout: verificationSummary?.artifacts?.frontendLayoutReport ?? null,
      apiGovernance: verificationSummary?.artifacts?.apiGovernanceReport ?? null,
    },
  },
  releaseDecision: {
    ready: releaseReady,
    status: releaseReady ? 'READY_FOR_ARTIFACT_PROMOTION' : 'BLOCKED',
    failedGateCount: releaseGates.filter((item) => !item.passed).length,
    gates: releaseGates,
    nextAction: releaseReady
      ? '归档本证据包，并在需要部署时进入人工审批、环境配置校验和回滚预案确认'
      : '先处理失败门禁，再重新生成发布证据包',
  },
};

fs.writeFileSync(outputFile, `${JSON.stringify(report, null, 2)}\n`);

if (!releaseReady) {
  console.error(`Release evidence failed: ${report.releaseDecision.failedGateCount} gate(s) failed.`);
  process.exit(1);
}

console.log(`Release evidence written to ${outputFile}`);
NODE

node -e "JSON.parse(require('node:fs').readFileSync(process.argv[1], 'utf8'))" "$OUTPUT_FILE"
