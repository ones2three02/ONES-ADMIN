#!/usr/bin/env bash
set -euo pipefail

PRODUCT_VERSION="$(tr -d '[:space:]' < VERSION)"

if [[ ! "$PRODUCT_VERSION" =~ ^v[0-9]+[.][0-9]+[.][0-9]+$ ]]; then
  echo "VERSION must use vMAJOR.MINOR.PATCH format, got: $PRODUCT_VERSION"
  exit 1
fi

MAVEN_VERSION="${PRODUCT_VERSION#v}-SNAPSHOT"
VERSION_CORE="${PRODUCT_VERSION#v}"
IFS='.' read -r VERSION_MAJOR VERSION_MINOR VERSION_PATCH <<<"$VERSION_CORE"
PREVIOUS_PRODUCT_VERSION=""
PREVIOUS_MAVEN_VERSION=""

if [ "$VERSION_PATCH" -gt 0 ]; then
  PREVIOUS_PRODUCT_VERSION="v$VERSION_MAJOR.$VERSION_MINOR.$((VERSION_PATCH - 1))"
  PREVIOUS_MAVEN_VERSION="$VERSION_MAJOR.$VERSION_MINOR.$((VERSION_PATCH - 1))-SNAPSHOT"
fi

require_match() {
  local label="$1"
  local pattern="$2"
  shift 2

  if ! grep -R -n -E "$pattern" "$@" >/dev/null; then
    echo "Version guard failed: $label"
    echo "Expected pattern: $pattern"
    exit 1
  fi
}

require_no_match() {
  local label="$1"
  local pattern="$2"
  shift 2

  if grep -R -n -E "$pattern" "$@" >/dev/null; then
    echo "Version guard failed: $label"
    grep -R -n -E "$pattern" "$@" || true
    exit 1
  fi
}

require_match "README product version" "当前产品版本：\`$PRODUCT_VERSION\`" README.md
require_match "CHANGELOG entry" "^## $PRODUCT_VERSION - " docs/CHANGELOG.md
require_match "Maven snapshot version" "<version>$MAVEN_VERSION</version>" server/pom.xml
require_match "application.yml default version" "ONES_ADMIN_VERSION:$PRODUCT_VERSION" server/src/main/resources/application.yml
require_match "OpenAPI default version" "ones[.]version:$PRODUCT_VERSION" server/src/main/java/com/ones/admin/config/OpenApiConfig.java
require_match "API resource default version" "ones[.]version:$PRODUCT_VERSION" server/src/main/java/com/ones/admin/system/ApiResourceService.java
require_match "OpenAPI version assertion" "info[.]version.*$PRODUCT_VERSION|value\\(\"$PRODUCT_VERSION\"\\)" server/src/test/java/com/ones/admin/common/ApiInfrastructureTest.java
require_match "API resource version assertions" "value\\(\"$PRODUCT_VERSION\"\\)" server/src/test/java/com/ones/admin/system/ApiResourceControllerTest.java
require_match "HRMS design version note" "$PRODUCT_VERSION" docs/architecture/hrms-phase-one-design.md
require_match "backend audit version note" "$PRODUCT_VERSION" docs/architecture/backend-engineering-audit.md

if [ -n "$PREVIOUS_PRODUCT_VERSION" ]; then
  require_no_match \
    "previous product version residue" \
    "$PREVIOUS_PRODUCT_VERSION|$PREVIOUS_MAVEN_VERSION" \
    README.md VERSION server/pom.xml server/src/main/resources/application.yml server/src/main/java server/src/test/java
fi
