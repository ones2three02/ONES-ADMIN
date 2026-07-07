#!/usr/bin/env bash
set -euo pipefail

DEFAULT_JAVA_HOME="${DEFAULT_JAVA_HOME:-/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home}"
DEFAULT_NODE_HOME="${DEFAULT_NODE_HOME:-/opt/homebrew/opt/node@22}"

if [ -z "${JAVA_HOME:-}" ] && [ -d "$DEFAULT_JAVA_HOME" ]; then
  export JAVA_HOME="$DEFAULT_JAVA_HOME"
fi

if [ -z "${NODE_HOME:-}" ] && [ -d "$DEFAULT_NODE_HOME" ]; then
  export NODE_HOME="$DEFAULT_NODE_HOME"
fi

if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [ -n "${NODE_HOME:-}" ]; then
  export PATH="$NODE_HOME/bin:$PATH"
fi

CI_BIN_DIR="${TMPDIR:-/tmp}/ones-admin-ci-bin"
mkdir -p "$CI_BIN_DIR"
cat >"$CI_BIN_DIR/pnpm" <<'EOF'
#!/usr/bin/env bash
exec corepack pnpm "$@"
EOF
chmod +x "$CI_BIN_DIR/pnpm"
export PATH="$CI_BIN_DIR:$PATH"

exec "$@"
