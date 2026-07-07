#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TOOLCHAIN="$ROOT_DIR/scripts/ci/run-with-toolchain.sh"

usage() {
  cat <<'EOF'
Usage: scripts/ci/verify.sh <stage>

Stages:
  preflight              Check toolchain, formatting residue, and version consistency.
  build-metadata         Generate CI build metadata for Jenkins artifact archive.
  frontend-dependencies  Install frontend dependencies from the lockfile.
  frontend-unit          Run Playground package and root frontend unit tests.
  frontend-typecheck     Run Playground typecheck.
  backend-test           Run backend Maven tests.
  frontend-build         Build the Playground frontend.
  repository-guard       Run version and repository safety guards.
  all                    Run the full CI verification sequence.
EOF
}

run_with_toolchain() {
  "$TOOLCHAIN" "$@"
}

run_preflight() {
  run_with_toolchain bash -c "
    cd '$ROOT_DIR'
    java -version
    node --version
    corepack --version
    git diff --check
    bash scripts/ci/version-guard.sh
  "
}

run_build_metadata() {
  run_with_toolchain bash -c "
    cd '$ROOT_DIR'
    bash scripts/ci/build-metadata.sh
  "
}

run_frontend_dependencies() {
  run_with_toolchain bash -c "
    cd '$ROOT_DIR/web'
    corepack pnpm install --frozen-lockfile
  "
}

run_frontend_unit() {
  run_with_toolchain bash -c "
    cd '$ROOT_DIR/web'
    corepack pnpm -F @vben/playground run test:unit
    corepack pnpm test:unit
  "
}

run_frontend_typecheck() {
  run_with_toolchain bash -c "
    cd '$ROOT_DIR/web'
    corepack pnpm -F @vben/playground run typecheck
  "
}

run_backend_test() {
  run_with_toolchain bash -c "
    cd '$ROOT_DIR/server'
    ./mvnw test
  "
}

run_frontend_build() {
  run_with_toolchain bash -c "
    cd '$ROOT_DIR/web'
    corepack pnpm -F @vben/playground run build
  "
}

run_repository_guard() {
  run_with_toolchain bash -c "
    cd '$ROOT_DIR'
    bash scripts/ci/version-guard.sh
    bash scripts/ci/repository-guard.sh
  "
}

run_all() {
  run_preflight
  run_build_metadata
  run_frontend_dependencies
  run_frontend_unit
  run_frontend_typecheck
  run_backend_test
  run_frontend_build
  run_repository_guard
}

case "${1:-}" in
  preflight)
    run_preflight
    ;;
  build-metadata)
    run_build_metadata
    ;;
  frontend-dependencies)
    run_frontend_dependencies
    ;;
  frontend-unit)
    run_frontend_unit
    ;;
  frontend-typecheck)
    run_frontend_typecheck
    ;;
  backend-test)
    run_backend_test
    ;;
  frontend-build)
    run_frontend_build
    ;;
  repository-guard)
    run_repository_guard
    ;;
  all)
    run_all
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac
