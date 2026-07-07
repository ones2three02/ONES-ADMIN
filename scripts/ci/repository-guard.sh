#!/usr/bin/env bash
set -euo pipefail

scan_tracked_files() {
  local label="$1"
  local pattern="$2"
  shift 2

  set +e
  git grep -n -E "$pattern" -- "$@"
  local grep_status="$?"
  set -e

  if [ "$grep_status" -eq 0 ]; then
    echo "Repository guard failed: $label"
    exit 1
  fi

  if [ "$grep_status" -ne 1 ]; then
    echo "Repository guard scan failed: $label"
    exit "$grep_status"
  fi
}

scan_tracked_files \
  "private intranet address" \
  '(^|[^0-9A-Za-z_.-])((10[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3})|(192[.]168[.][0-9]{1,3}[.][0-9]{1,3})|(172[.](1[6-9]|2[0-9]|3[0-1])[.][0-9]{1,3}[.][0-9]{1,3}))(:[0-9]{2,5})?($|[^0-9A-Za-z_.-])' \
  . ':(exclude)docs/local/**' ':(exclude)server/config/**'

scan_tracked_files \
  "hard-coded secret assignment" \
  '([Pp]assword|PASSWORD|SECRET|SECRET_KEY|ACCESS_KEY|access-key|secret-key)[[:space:]]*[:=][[:space:]]*[^${[:space:]'\''"`<>][^[:space:]'\''"`<>]+' \
  '*.env' '*.env.*' '*.yml' '*.yaml' '*.properties' '*.toml' \
  ':(exclude)docs/local/**' ':(exclude)server/config/**' ':(exclude)server/src/test/resources/**'
