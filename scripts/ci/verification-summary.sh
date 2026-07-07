#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_FILE="${1:-$ROOT_DIR/.ci-artifacts/verification-summary.json}"

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
const { spawnSync } = require('node:child_process');

const rootDir = process.env.ROOT_DIR;
const outputFile = process.env.OUTPUT_FILE;
const productVersion = fs.readFileSync(path.join(rootDir, 'VERSION'), 'utf8').trim();
const generatedAtUtc = new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');

function runCheck(code, command, args) {
  const result = spawnSync(command, args, {
    cwd: rootDir,
    encoding: 'utf8',
  });

  return {
    code,
    command: [command, ...args].join(' '),
    exitCode: result.status ?? 1,
    passed: result.status === 0,
  };
}

function readJsonIfExists(filePath) {
  if (!fs.existsSync(filePath)) {
    return null;
  }

  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function walkFiles(dirPath) {
  if (!fs.existsSync(dirPath)) {
    return [];
  }

  return fs.readdirSync(dirPath, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      return walkFiles(absolutePath);
    }
    if (entry.isFile()) {
      return [absolutePath];
    }
    return [];
  });
}

function readBackendTestSummary() {
  const reportDir = path.join(rootDir, 'server', 'target', 'surefire-reports');
  const files = fs.existsSync(reportDir)
    ? fs.readdirSync(reportDir)
        .filter((fileName) => fileName.endsWith('.xml'))
        .map((fileName) => path.join(reportDir, fileName))
    : [];

  const totals = {
    reportFileCount: files.length,
    tests: 0,
    failures: 0,
    errors: 0,
    skipped: 0,
  };

  for (const file of files) {
    const xml = fs.readFileSync(file, 'utf8');
    const suite = xml.match(/<testsuite\b[^>]*>/);
    if (!suite) {
      continue;
    }

    for (const key of ['tests', 'failures', 'errors', 'skipped']) {
      const match = suite[0].match(new RegExp(`${key}="(\\d+)"`));
      if (match) {
        totals[key] += Number(match[1]);
      }
    }
  }

  return {
    ...totals,
    passed: totals.reportFileCount > 0 && totals.failures === 0 && totals.errors === 0,
  };
}

function readFrontendBuildSummary() {
  const distDir = path.join(rootDir, 'web', 'playground', 'dist');
  const files = walkFiles(distDir);
  const totalBytes = files.reduce((sum, file) => sum + fs.statSync(file).size, 0);

  return {
    distDir: path.relative(rootDir, distDir),
    exists: fs.existsSync(distDir),
    indexHtmlExists: fs.existsSync(path.join(distDir, 'index.html')),
    fileCount: files.length,
    totalBytes,
    passed: fs.existsSync(path.join(distDir, 'index.html')) && files.length > 0,
  };
}

function readApiGovernanceReportSummary() {
  const reportPath = path.join(rootDir, '.ci-artifacts', 'api-governance-report.json');
  const report = readJsonIfExists(reportPath);

  if (!report) {
    return {
      reportPath: path.relative(rootDir, reportPath),
      exists: false,
      version: null,
      qualityScore: null,
      governancePassed: false,
      manifestChecksum: null,
      releaseStatus: null,
      passed: false,
    };
  }

  const manifestChecksum = report.manifest?.checksum ?? null;
  const governancePassed = report.governance?.passed === true;
  const versionMatches = report.applicationVersion === productVersion;
  const qualityScore = Number(report.qualityScore);
  const checksumValid = typeof manifestChecksum === 'string' && /^[a-f0-9]{64}$/.test(manifestChecksum);

  return {
    reportPath: path.relative(rootDir, reportPath),
    exists: true,
    version: report.applicationVersion ?? null,
    qualityScore,
    governancePassed,
    manifestChecksum,
    releaseStatus: report.releaseReadiness?.status ?? null,
    passed: versionMatches && governancePassed && qualityScore >= 95 && checksumValid,
  };
}

function readDatabaseMigrationReportSummary() {
  const reportPath = path.join(rootDir, '.ci-artifacts', 'database-migration-report.json');
  const report = readJsonIfExists(reportPath);

  if (!report) {
    return {
      reportPath: path.relative(rootDir, reportPath),
      exists: false,
      version: null,
      fileCount: 0,
      latestVersion: null,
      contiguous: false,
      governancePassed: false,
      errorCount: null,
      destructiveViolations: [],
      legacySchemaSqlExists: null,
      passed: false,
    };
  }

  const versionMatches = report.product?.version === productVersion;
  const governancePassed = report.governance?.passed === true;
  const fileCount = Number(report.migration?.fileCount ?? 0);
  const latestVersion = report.migration?.latestVersion ?? null;
  const contiguous = report.migration?.contiguous === true;
  const legacySchemaSqlExists = report.migration?.legacySchemaSqlExists === true;
  const destructiveViolations = Array.isArray(report.migration?.destructiveViolations)
    ? report.migration.destructiveViolations
    : [];
  const allChecksumsValid = Array.isArray(report.migration?.files)
    && report.migration.files.length === fileCount
    && report.migration.files.every((migration) => typeof migration.checksum === 'string'
      && /^[a-f0-9]{64}$/.test(migration.checksum));

  return {
    reportPath: path.relative(rootDir, reportPath),
    exists: true,
    version: report.product?.version ?? null,
    fileCount,
    latestVersion,
    contiguous,
    governancePassed,
    errorCount: report.governance?.errorCount ?? null,
    destructiveViolations,
    legacySchemaSqlExists,
    passed: versionMatches
      && governancePassed
      && fileCount > 0
      && contiguous
      && !legacySchemaSqlExists
      && destructiveViolations.length === 0
      && allChecksumsValid,
  };
}

function readEnvironmentConfigReportSummary() {
  const reportPath = path.join(rootDir, '.ci-artifacts', 'environment-config-report.json');
  const report = readJsonIfExists(reportPath);

  if (!report) {
    return {
      reportPath: path.relative(rootDir, reportPath),
      exists: false,
      version: null,
      governancePassed: false,
      errorCount: null,
      warningCount: null,
      totalItemCount: 0,
      uniqueEnvCount: 0,
      readsIgnoredSensitiveSources: null,
      passed: false,
    };
  }

  const versionMatches = report.product?.version === productVersion;
  const governancePassed = report.governance?.passed === true;
  const totalItemCount = Number(report.configuration?.totalItemCount ?? 0);
  const uniqueEnvCount = Number(report.configuration?.uniqueEnvCount ?? 0);
  const readsIgnoredSensitiveSources = report.scanPolicy?.readsIgnoredSensitiveSources === true;

  return {
    reportPath: path.relative(rootDir, reportPath),
    exists: true,
    version: report.product?.version ?? null,
    governancePassed,
    errorCount: report.governance?.errorCount ?? null,
    warningCount: report.governance?.warningCount ?? null,
    totalItemCount,
    uniqueEnvCount,
    readsIgnoredSensitiveSources,
    passed: versionMatches
      && governancePassed
      && totalItemCount > 0
      && uniqueEnvCount > 0
      && !readsIgnoredSensitiveSources,
  };
}

function readReleaseEvidenceSummary() {
  const reportPath = path.join(rootDir, '.ci-artifacts', 'release-evidence.json');
  const report = readJsonIfExists(reportPath);

  if (!report) {
    return {
      reportPath: path.relative(rootDir, reportPath),
      exists: false,
      version: null,
      ready: false,
      status: null,
      failedGateCount: null,
      gateCount: 0,
      passed: false,
    };
  }

  const versionMatches = report.product?.version === productVersion;
  if (!versionMatches) {
    return {
      reportPath: path.relative(rootDir, reportPath),
      exists: false,
      version: report.product?.version ?? null,
      ready: false,
      status: 'STALE',
      failedGateCount: null,
      gateCount: 0,
      requiredArtifactsPresent: false,
      passed: false,
    };
  }

  const ready = report.releaseDecision?.ready === true;
  const failedGateCount = Number(report.releaseDecision?.failedGateCount ?? 1);
  const gateCount = Array.isArray(report.releaseDecision?.gates)
    ? report.releaseDecision.gates.length
    : 0;
  const artifacts = Array.isArray(report.evidence?.artifacts) ? report.evidence.artifacts : [];
  const requiredArtifacts = new Set([
    'API_GOVERNANCE_REPORT',
    'BUILD_METADATA',
    'DATABASE_MIGRATION_REPORT',
    'ENVIRONMENT_CONFIG_REPORT',
    'VERIFICATION_SUMMARY',
  ]);
  const artifactCodes = new Set(artifacts.map((artifact) => artifact.code));
  const requiredArtifactsPresent = [...requiredArtifacts].every((code) => artifactCodes.has(code));

  return {
    reportPath: path.relative(rootDir, reportPath),
    exists: true,
    version: report.product?.version ?? null,
    ready,
    status: report.releaseDecision?.status ?? null,
    failedGateCount,
    gateCount,
    requiredArtifactsPresent,
    passed: versionMatches && ready && failedGateCount === 0 && gateCount > 0 && requiredArtifactsPresent,
  };
}

const buildMetadataPath = path.join(rootDir, '.ci-artifacts', 'build-metadata.json');
const buildMetadata = readJsonIfExists(buildMetadataPath);
const buildMetadataVersionMatches = buildMetadata?.product?.version === productVersion;
const backendTests = readBackendTestSummary();
const frontendBuild = readFrontendBuildSummary();
const environmentConfigReport = readEnvironmentConfigReportSummary();
const databaseMigrationReport = readDatabaseMigrationReportSummary();
const apiGovernanceReport = readApiGovernanceReportSummary();
const releaseEvidence = readReleaseEvidenceSummary();

const checks = [
  {
    code: 'BUILD_METADATA_PRESENT',
    command: 'read .ci-artifacts/build-metadata.json',
    exitCode: buildMetadata ? 0 : 1,
    passed: Boolean(buildMetadata),
  },
  {
    code: 'BUILD_METADATA_VERSION_MATCH',
    command: 'compare VERSION and .ci-artifacts/build-metadata.json',
    exitCode: buildMetadataVersionMatches ? 0 : 1,
    passed: buildMetadataVersionMatches,
  },
  runCheck('GIT_DIFF_CHECK', 'git', ['diff', '--check']),
  runCheck('VERSION_GUARD', 'bash', ['scripts/ci/version-guard.sh']),
  runCheck('REPOSITORY_GUARD', 'bash', ['scripts/ci/repository-guard.sh']),
  {
    code: 'BACKEND_TEST_REPORTS_PRESENT',
    command: 'read server/target/surefire-reports/*.xml',
    exitCode: backendTests.passed ? 0 : 1,
    passed: backendTests.passed,
  },
  {
    code: 'FRONTEND_BUILD_ARTIFACT_PRESENT',
    command: 'read web/playground/dist/index.html',
    exitCode: frontendBuild.passed ? 0 : 1,
    passed: frontendBuild.passed,
  },
  {
    code: 'ENVIRONMENT_CONFIG_REPORT_PRESENT',
    command: 'read .ci-artifacts/environment-config-report.json',
    exitCode: environmentConfigReport.passed ? 0 : 1,
    passed: environmentConfigReport.passed,
  },
  {
    code: 'DATABASE_MIGRATION_REPORT_PRESENT',
    command: 'read .ci-artifacts/database-migration-report.json',
    exitCode: databaseMigrationReport.passed ? 0 : 1,
    passed: databaseMigrationReport.passed,
  },
  {
    code: 'API_GOVERNANCE_REPORT_PRESENT',
    command: 'read .ci-artifacts/api-governance-report.json',
    exitCode: apiGovernanceReport.passed ? 0 : 1,
    passed: apiGovernanceReport.passed,
  },
  {
    code: 'RELEASE_EVIDENCE_PRESENT',
    command: 'read .ci-artifacts/release-evidence.json',
    exitCode: releaseEvidence.exists ? (releaseEvidence.passed ? 0 : 1) : 0,
    passed: !releaseEvidence.exists || releaseEvidence.passed,
  },
];

const summary = {
  product: {
    name: 'ONES-ADMIN',
    version: productVersion,
  },
  generatedAtUtc,
  buildMetadata: buildMetadata
    ? {
        version: buildMetadata.product?.version ?? null,
        backendSnapshotVersion: buildMetadata.product?.backendSnapshotVersion ?? null,
        branch: buildMetadata.git?.branch ?? null,
        commit: buildMetadata.git?.commit ?? null,
        shortCommit: buildMetadata.git?.shortCommit ?? null,
        dirty: buildMetadata.git?.dirty ?? null,
      }
    : null,
  gates: checks,
  artifacts: {
    backendTests,
    frontendBuild,
    environmentConfigReport,
    databaseMigrationReport,
    apiGovernanceReport,
    releaseEvidence,
  },
  overall: {
    passed: checks.every((check) => check.passed),
    failedGateCount: checks.filter((check) => !check.passed).length,
  },
};

fs.writeFileSync(outputFile, `${JSON.stringify(summary, null, 2)}\n`);

if (!summary.overall.passed) {
  console.error(`Verification summary failed: ${summary.overall.failedGateCount} gate(s) failed.`);
  process.exit(1);
}

console.log(`Verification summary written to ${outputFile}`);
NODE

node -e "JSON.parse(require('node:fs').readFileSync(process.argv[1], 'utf8'))" "$OUTPUT_FILE"
