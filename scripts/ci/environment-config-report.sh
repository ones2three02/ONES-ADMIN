#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_FILE="${1:-$ROOT_DIR/.ci-artifacts/environment-config-report.json}"

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

const sensitivePattern = /(PASSWORD|SECRET|ACCESS_KEY|SECRET_KEY|TOKEN|STORE_SECURE_KEY|CLIENT_SECRET)/i;
const urlPattern = /^(https?:\/\/|jdbc:|\/)/;

function relative(filePath) {
  return path.relative(rootDir, filePath).replaceAll(path.sep, '/');
}

function readText(relativePath) {
  return fs.readFileSync(path.join(rootDir, relativePath), 'utf8');
}

function extractApplicationPlaceholders() {
  const filePath = 'server/src/main/resources/application.yml';
  const content = readText(filePath);
  const placeholders = [];
  const pattern = /\$\{([A-Z0-9_]+)(?::([^}]*))?}/g;
  let match;

  while ((match = pattern.exec(content)) !== null) {
    placeholders.push({
      source: filePath,
      envName: match[1],
      defaultValue: match[2] ?? null,
      sensitive: sensitivePattern.test(match[1]),
    });
  }

  return placeholders;
}

function extractEnvFileEntries(relativePath) {
  const filePath = path.join(rootDir, relativePath);
  if (!fs.existsSync(filePath)) {
    return [];
  }

  return fs.readFileSync(filePath, 'utf8')
    .split(/\r?\n/)
    .map((line, index) => ({ line: line.trim(), lineNumber: index + 1 }))
    .filter(({ line }) => line && !line.startsWith('#') && line.includes('='))
    .map(({ line, lineNumber }) => {
      const separatorIndex = line.indexOf('=');
      const envName = line.slice(0, separatorIndex).trim();
      const defaultValue = line.slice(separatorIndex + 1).trim();
      return {
        source: relativePath,
        lineNumber,
        envName,
        defaultValue,
        sensitive: sensitivePattern.test(envName),
      };
    });
}

function classifyDefault(defaultValue) {
  if (defaultValue === null || defaultValue === '') {
    return 'EMPTY';
  }
  if (/please-replace-me|应用的|corpId|clientId/i.test(defaultValue)) {
    return 'PLACEHOLDER';
  }
  if (/localhost|127\.0\.0\.1/.test(defaultValue)) {
    return 'LOCAL_DEFAULT';
  }
  if (urlPattern.test(defaultValue)) {
    return 'CONFIGURED_DEFAULT';
  }
  return 'STATIC_DEFAULT';
}

function requirementOf(entry) {
  const envName = entry.envName;
  if (envName === 'ONES_DB_PASSWORD'
    || envName === 'ONES_RABBITMQ_USERNAME'
    || envName === 'ONES_RABBITMQ_PASSWORD'
    || envName === 'ONES_MINIO_ACCESS_KEY'
    || envName === 'ONES_MINIO_SECRET_KEY'
    || envName === 'VITE_APP_STORE_SECURE_KEY') {
    return 'PRODUCTION_REQUIRED';
  }
  if (envName.startsWith('VITE_GLOB_AUTH_FEISHU_') || envName.startsWith('VITE_GLOB_AUTH_SSO_')) {
    return 'FEATURE_REQUIRED';
  }
  if (envName === 'ONES_DB_URL'
    || envName === 'ONES_DB_USERNAME'
    || envName === 'ONES_REDIS_HOST'
    || envName === 'ONES_REDIS_PORT'
    || envName === 'ONES_SECURITY_SESSION_STORAGE'
    || envName === 'ONES_REPEAT_SUBMIT_STORAGE') {
    return 'PRODUCTION_REVIEW';
  }
  return 'OPTIONAL';
}

function sanitizeDefault(defaultValue, sensitive) {
  if (sensitive) {
    if (defaultValue === null || defaultValue === '') {
      return null;
    }
    return '<redacted>';
  }
  return defaultValue;
}

function toItem(entry) {
  const defaultKind = classifyDefault(entry.defaultValue);
  return {
    envName: entry.envName,
    source: entry.source,
    lineNumber: entry.lineNumber ?? null,
    requirement: requirementOf(entry),
    sensitive: entry.sensitive,
    defaultKind,
    hasDefault: entry.defaultValue !== null && entry.defaultValue !== '',
    sanitizedDefault: sanitizeDefault(entry.defaultValue, entry.sensitive),
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

const items = [
  ...extractApplicationPlaceholders(),
  ...extractEnvFileEntries('web/playground/.env'),
  ...extractEnvFileEntries('web/playground/.env.development'),
  ...extractEnvFileEntries('web/playground/.env.production'),
].map(toItem);

const byEnvName = new Map();
for (const item of items) {
  if (!byEnvName.has(item.envName)) {
    byEnvName.set(item.envName, []);
  }
  byEnvName.get(item.envName).push(item);
}

const productionRequired = items.filter((item) => item.requirement === 'PRODUCTION_REQUIRED');
const featureRequired = items.filter((item) => item.requirement === 'FEATURE_REQUIRED');
const productionReview = items.filter((item) => item.requirement === 'PRODUCTION_REVIEW');
const sensitiveDefaults = items.filter((item) => item.sensitive && item.hasDefault && item.defaultKind !== 'PLACEHOLDER');
const unsafeCommittedSecrets = sensitiveDefaults.filter((item) => item.sanitizedDefault === '<redacted>');
const localDefaults = items.filter((item) => item.defaultKind === 'LOCAL_DEFAULT');
const placeholderDefaults = items.filter((item) => item.defaultKind === 'PLACEHOLDER');
const ignoredSensitiveSources = [
  'docs/local/',
  'server/config/application-local.yml',
  'server/src/main/resources/application-local.yml',
];

const requiredEnvNames = new Set([
  'ONES_DB_PASSWORD',
  'ONES_RABBITMQ_PASSWORD',
  'ONES_MINIO_SECRET_KEY',
  'VITE_APP_STORE_SECURE_KEY',
]);
const missingRequiredEnvDefinitions = [...requiredEnvNames].filter((envName) => !byEnvName.has(envName));
const serverVersionItem = items.find((item) => item.envName === 'ONES_ADMIN_VERSION');
const serverVersionDefaultMatches = serverVersionItem?.sanitizedDefault === productVersion;

const rules = [
  rule(
    'ENV_CONFIG_ITEMS_DISCOVERED',
    'CONFIGURATION',
    'ERROR',
    items.length > 0,
    '必须能从仓库配置中发现环境变量清单',
    '检查 server/src/main/resources/application.yml 和 web/playground/.env* 是否仍按环境变量管理配置',
  ),
  rule(
    'PRODUCT_VERSION_DEFAULT_MATCHES',
    'CONFIGURATION',
    'ERROR',
    serverVersionDefaultMatches,
    'ONES_ADMIN_VERSION 默认值必须与 VERSION 一致',
    '同步 server/src/main/resources/application.yml 中的 ONES_ADMIN_VERSION 默认值',
  ),
  rule(
    'PRODUCTION_REQUIRED_ITEMS_DECLARED',
    'CONFIGURATION',
    'ERROR',
    missingRequiredEnvDefinitions.length === 0,
    '生产必填配置项必须进入审计清单',
    '将缺失的生产必填配置项补充到 application.yml 或前端 env 模板',
  ),
  rule(
    'NO_COMMITTED_SENSITIVE_DEFAULTS',
    'SECURITY',
    'ERROR',
    unsafeCommittedSecrets.length === 0,
    '提交文件中不得出现真实敏感默认值',
    '移除提交文件中的密码、Secret、Access Key 等敏感值，改用环境变量或 ignored 本地配置',
  ),
  rule(
    'LOCAL_DEFAULTS_ARE_REVIEWABLE',
    'CONFIGURATION',
    'WARN',
    localDefaults.length > 0,
    '本地默认值必须被报告显式列出，便于上线前复核',
    '上线前由 Jenkins 或人工审批确认所有 LOCAL_DEFAULT 配置均被正式环境变量覆盖',
  ),
  rule(
    'PLACEHOLDER_VALUES_ARE_REVIEWABLE',
    'CONFIGURATION',
    'WARN',
    placeholderDefaults.length > 0,
    '占位默认值必须被报告显式列出，便于上线前替换',
    '上线前替换前端 Store 密钥、第三方登录应用标识等占位值',
  ),
];

const report = {
  product: {
    name: 'ONES-ADMIN',
    version: productVersion,
  },
  generatedAtUtc,
  scanPolicy: {
    readsIgnoredSensitiveSources: false,
    ignoredSensitiveSources,
    valuePolicy: '敏感默认值只输出 <redacted> 或 null，不输出真实值',
  },
  configuration: {
    totalItemCount: items.length,
    uniqueEnvCount: byEnvName.size,
    productionRequiredCount: productionRequired.length,
    productionReviewCount: productionReview.length,
    featureRequiredCount: featureRequired.length,
    sensitiveItemCount: items.filter((item) => item.sensitive).length,
    localDefaultCount: localDefaults.length,
    placeholderDefaultCount: placeholderDefaults.length,
    missingRequiredEnvDefinitions,
    sources: [...new Set(items.map((item) => item.source))],
    items: items.sort((left, right) => left.envName.localeCompare(right.envName) || left.source.localeCompare(right.source)),
  },
  governance: {
    passed: rules.filter((item) => item.severity === 'ERROR').every((item) => item.passed),
    warningCount: rules.filter((item) => item.severity === 'WARN' && !item.passed).length,
    errorCount: rules.filter((item) => item.severity === 'ERROR' && !item.passed).length,
    rules,
  },
};

fs.writeFileSync(outputFile, `${JSON.stringify(report, null, 2)}\n`);

if (!report.governance.passed) {
  console.error(`Environment config governance failed: ${report.governance.errorCount} error(s).`);
  process.exit(1);
}

console.log(`Environment config report written to ${outputFile}`);
NODE

node -e "JSON.parse(require('node:fs').readFileSync(process.argv[1], 'utf8'))" "$OUTPUT_FILE"
