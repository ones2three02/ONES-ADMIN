#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_FILE="${1:-$ROOT_DIR/.ci-artifacts/database-migration-report.json}"

mkdir -p "$(dirname "$OUTPUT_FILE")"

if ! command -v node >/dev/null 2>&1; then
  echo "Required command not found: node" >&2
  exit 1
fi

export ROOT_DIR
export OUTPUT_FILE

node <<'NODE'
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');

const rootDir = process.env.ROOT_DIR;
const outputFile = process.env.OUTPUT_FILE;
const productVersion = fs.readFileSync(path.join(rootDir, 'VERSION'), 'utf8').trim();
const generatedAtUtc = new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');
const migrationDir = path.join(rootDir, 'server', 'src', 'main', 'resources', 'db', 'migration');
const legacySchemaFile = path.join(rootDir, 'server', 'src', 'main', 'resources', 'schema.sql');
const migrationNamePattern = /^V([1-9][0-9]*)__([a-z0-9]+(?:_[a-z0-9]+)*)\.sql$/;
const destructiveSqlPattern =
  /\b(drop\s+table|truncate\s+table|delete\s+from|alter\s+table\s+[^;]+\s+drop\s+(column\s+)?)\b/gi;
const destructiveApprovalMarker = 'ONES-MIGRATION-APPROVED-DESTRUCTIVE';

function relative(filePath) {
  return path.relative(rootDir, filePath).replaceAll(path.sep, '/');
}

function sha256(content) {
  return crypto.createHash('sha256').update(content).digest('hex');
}

function readMigrationFiles() {
  if (!fs.existsSync(migrationDir)) {
    return [];
  }

  return fs.readdirSync(migrationDir, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith('.sql'))
    .map((entry) => path.join(migrationDir, entry.name))
    .sort((left, right) => left.localeCompare(right));
}

function toMigration(filePath) {
  const fileName = path.basename(filePath);
  const content = fs.readFileSync(filePath);
  const sql = content.toString('utf8');
  const nameMatch = migrationNamePattern.exec(fileName);
  const destructiveMatches = [...sql.matchAll(destructiveSqlPattern)].map((match) => match[1].replace(/\s+/g, ' ').trim());
  const hasDestructiveSql = destructiveMatches.length > 0;
  const destructiveApproved = !hasDestructiveSql || sql.includes(destructiveApprovalMarker);

  return {
    fileName,
    path: relative(filePath),
    version: nameMatch ? Number(nameMatch[1]) : null,
    description: nameMatch ? nameMatch[2] : null,
    bytes: content.length,
    checksum: sha256(content),
    standardName: Boolean(nameMatch),
    destructiveSql: {
      detected: hasDestructiveSql,
      approved: destructiveApproved,
      matchCount: destructiveMatches.length,
      matches: [...new Set(destructiveMatches)],
    },
  };
}

function rule(code, category, severity, passed, message, remediation) {
  return {
    code,
    category,
    severity,
    passed,
    message,
    remediation,
  };
}

const migrationFiles = readMigrationFiles();
const migrations = migrationFiles.map(toMigration);
const versions = migrations
  .map((migration) => migration.version)
  .filter((version) => Number.isInteger(version))
  .sort((left, right) => left - right);
const expectedVersions = versions.length > 0
  ? Array.from({ length: versions.length }, (_, index) => index + 1)
  : [];
const duplicateVersions = versions.filter((version, index) => versions.indexOf(version) !== index);
const contiguous = versions.length > 0
  && versions.length === new Set(versions).size
  && expectedVersions.every((version, index) => versions[index] === version);
const namingViolations = migrations.filter((migration) => !migration.standardName).map((migration) => migration.fileName);
const destructiveViolations = migrations
  .filter((migration) => migration.destructiveSql.detected && !migration.destructiveSql.approved)
  .map((migration) => migration.fileName);
const directoryExists = fs.existsSync(migrationDir);
const legacySchemaSqlExists = fs.existsSync(legacySchemaFile);
const checksumValid = migrations.every((migration) => /^[a-f0-9]{64}$/.test(migration.checksum));

const rules = [
  rule(
    'MIGRATION_DIRECTORY_PRESENT',
    'DATABASE',
    'ERROR',
    directoryExists,
    'Flyway 迁移目录必须存在',
    '恢复 server/src/main/resources/db/migration，并将表结构变更统一放入该目录',
  ),
  rule(
    'MIGRATION_FILES_PRESENT',
    'DATABASE',
    'ERROR',
    migrations.length > 0,
    'Flyway 迁移脚本不能为空',
    '至少保留 V1__init_schema.sql 基线脚本，并用增量脚本承接后续表结构变更',
  ),
  rule(
    'LEGACY_SCHEMA_SQL_DISABLED',
    'DATABASE',
    'ERROR',
    !legacySchemaSqlExists,
    '禁止恢复旧 schema.sql 隐式建表入口',
    '删除 server/src/main/resources/schema.sql，所有 DDL 通过 Flyway 版本脚本管理',
  ),
  rule(
    'MIGRATION_FILE_NAMES_STANDARD',
    'DATABASE',
    'ERROR',
    namingViolations.length === 0,
    '迁移脚本必须使用 V版本号__小写下划线说明.sql 命名',
    '按 V1__init_schema.sql 格式重命名不符合规范的迁移脚本',
  ),
  rule(
    'MIGRATION_VERSIONS_CONSECUTIVE',
    'DATABASE',
    'ERROR',
    contiguous,
    '迁移版本必须从 1 开始连续递增且不重复',
    '补齐缺失版本或调整重复版本，禁止跳号和并行占用同一版本号',
  ),
  rule(
    'DESTRUCTIVE_SQL_APPROVED',
    'DATABASE',
    'ERROR',
    destructiveViolations.length === 0,
    '破坏性 SQL 必须带审批标记',
    '确需执行破坏性 SQL 时，在脚本中加入 ONES-MIGRATION-APPROVED-DESTRUCTIVE 并说明影响范围和回滚方案',
  ),
  rule(
    'MIGRATION_CHECKSUMS_VALID',
    'DATABASE',
    'ERROR',
    checksumValid,
    '迁移脚本必须生成 SHA-256 指纹',
    '检查迁移脚本是否可读，并保持报告生成脚本使用 SHA-256',
  ),
];

const report = {
  product: {
    name: 'ONES-ADMIN',
    version: productVersion,
  },
  generatedAtUtc,
  migration: {
    directory: relative(migrationDir),
    legacySchemaSql: relative(legacySchemaFile),
    legacySchemaSqlExists,
    fileCount: migrations.length,
    latestVersion: versions.at(-1) ?? null,
    actualVersions: versions,
    expectedVersions,
    contiguous,
    duplicateVersions: [...new Set(duplicateVersions)],
    namingViolations,
    destructiveViolations,
    checksumAlgorithm: 'SHA-256',
    files: migrations,
  },
  governance: {
    passed: rules.every((item) => item.passed),
    errorCount: rules.filter((item) => item.severity === 'ERROR' && !item.passed).length,
    rules,
  },
};

fs.writeFileSync(outputFile, `${JSON.stringify(report, null, 2)}\n`);

if (!report.governance.passed) {
  console.error(`Database migration governance failed: ${report.governance.errorCount} error(s).`);
  process.exit(1);
}

console.log(`Database migration report written to ${outputFile}`);
NODE

node -e "JSON.parse(require('node:fs').readFileSync(process.argv[1], 'utf8'))" "$OUTPUT_FILE"
