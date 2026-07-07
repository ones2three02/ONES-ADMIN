#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_FILE="${1:-$ROOT_DIR/.ci-artifacts/build-metadata.json}"

mkdir -p "$(dirname "$OUTPUT_FILE")"

read_required_command() {
  local command_name="$1"

  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command not found: $command_name" >&2
    exit 1
  fi
}

read_required_command git
read_required_command java
read_required_command node
read_required_command corepack

cd "$ROOT_DIR"

PRODUCT_VERSION="$(tr -d '[:space:]' < VERSION)"
MAVEN_VERSION="${PRODUCT_VERSION#v}-SNAPSHOT"
BUILD_TIME_UTC="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
GIT_COMMIT="$(git rev-parse HEAD)"
GIT_COMMIT_SHORT="$(git rev-parse --short HEAD)"
CURRENT_BRANCH="$(git branch --show-current 2>/dev/null || true)"
if [ -z "$CURRENT_BRANCH" ]; then
  CURRENT_BRANCH="${BRANCH_NAME:-${GIT_BRANCH:-}}"
  CURRENT_BRANCH="${CURRENT_BRANCH#origin/}"
fi
GIT_BRANCH="$CURRENT_BRANCH"
GIT_DIRTY="false"

if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  GIT_DIRTY="true"
fi

JAVA_VERSION="$(java -version 2>&1 | sed -n '1p')"
NODE_VERSION="$(node --version)"
COREPACK_VERSION="$(corepack --version)"
MAVEN_WRAPPER_VERSION="$(cd server && ./mvnw -v | sed -n '1p')"

export PRODUCT_VERSION
export MAVEN_VERSION
export BUILD_TIME_UTC
export GIT_COMMIT
export GIT_COMMIT_SHORT
export GIT_BRANCH
export GIT_DIRTY
export JAVA_VERSION
export NODE_VERSION
export COREPACK_VERSION
export MAVEN_WRAPPER_VERSION
export BUILD_NUMBER="${BUILD_NUMBER:-}"
export JOB_NAME="${JOB_NAME:-}"
export CI="${CI:-false}"
export OUTPUT_FILE

node <<'NODE'
const fs = require('node:fs');

const metadata = {
  product: {
    name: 'ONES-ADMIN',
    version: process.env.PRODUCT_VERSION,
    backendSnapshotVersion: process.env.MAVEN_VERSION,
  },
  git: {
    branch: process.env.GIT_BRANCH || null,
    commit: process.env.GIT_COMMIT,
    shortCommit: process.env.GIT_COMMIT_SHORT,
    dirty: process.env.GIT_DIRTY === 'true',
  },
  build: {
    timeUtc: process.env.BUILD_TIME_UTC,
    ci: process.env.CI === 'true',
    jenkins: {
      jobName: process.env.JOB_NAME || null,
      buildNumber: process.env.BUILD_NUMBER || null,
    },
  },
  toolchain: {
    java: process.env.JAVA_VERSION,
    node: process.env.NODE_VERSION,
    corepack: process.env.COREPACK_VERSION,
    mavenWrapper: process.env.MAVEN_WRAPPER_VERSION,
  },
};

fs.writeFileSync(process.env.OUTPUT_FILE, `${JSON.stringify(metadata, null, 2)}\n`);
NODE

node -e "JSON.parse(require('node:fs').readFileSync(process.argv[1], 'utf8'))" "$OUTPUT_FILE"

echo "Build metadata written to $OUTPUT_FILE"
