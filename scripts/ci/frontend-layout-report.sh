#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_FILE="${1:-$ROOT_DIR/.ci-artifacts/frontend-layout-report.json}"

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
const viewsDir = path.join(rootDir, 'web', 'playground', 'src', 'views');
const splitLayoutPath = path.join(viewsDir, 'shared', 'split-list-layout.vue');
const excludedPathParts = [
  `${path.sep}demos${path.sep}`,
  `${path.sep}examples${path.sep}`,
  `${path.sep}dashboard${path.sep}`,
  `${path.sep}_core${path.sep}`,
];

function relative(filePath) {
  return path.relative(rootDir, filePath).replaceAll(path.sep, '/');
}

function walkVueFiles(dirPath) {
  if (!fs.existsSync(dirPath)) {
    return [];
  }

  return fs.readdirSync(dirPath, { withFileTypes: true }).flatMap((entry) => {
    const absolutePath = path.join(dirPath, entry.name);
    if (entry.isDirectory()) {
      return walkVueFiles(absolutePath);
    }
    if (entry.isFile() && entry.name.endsWith('.vue')) {
      return [absolutePath];
    }
    return [];
  });
}

function lineOf(content, index) {
  return content.slice(0, index).split(/\r?\n/).length;
}

function findMatches(content, pattern) {
  const matches = [];
  for (const match of content.matchAll(pattern)) {
    matches.push({
      lineNumber: lineOf(content, match.index ?? 0),
      match: match[0],
    });
  }
  return matches;
}

function issue(code, severity, filePath, lineNumber, message, remediation, match = null) {
  return {
    code,
    severity,
    file: relative(filePath),
    lineNumber,
    match,
    message,
    remediation,
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

const vueFiles = walkVueFiles(viewsDir)
  .filter((filePath) => !excludedPathParts.some((part) => filePath.includes(part)));
const splitLayoutExists = fs.existsSync(splitLayoutPath);
const issues = [];
const splitLayoutUsers = [];
const vxeGridFiles = [];
const autoHeightGridFiles = [];

for (const filePath of vueFiles) {
  const content = fs.readFileSync(filePath, 'utf8');
  const usesSplitLayout = /<SplitListLayout\b/.test(content);
  const usesVxeGrid = /useVbenVxeGrid\s*\(/.test(content);

  if (usesSplitLayout) {
    splitLayoutUsers.push(relative(filePath));
  }
  if (usesVxeGrid) {
    vxeGridFiles.push(relative(filePath));
  }

  for (const match of findMatches(content, /\bclass=["'][^"']*\bw-[1-5]\/[1-6]\b[^"']*["']/g)) {
    issues.push(issue(
      'LEGACY_FRACTION_WIDTH_LAYOUT',
      'ERROR',
      filePath,
      match.lineNumber,
      '业务视图不得使用 Tailwind 分数宽度拼左右布局，容易导致宽表格挤压和响应式漂移',
      '左右分栏列表页请使用 SplitListLayout；普通单栏页面请使用 flex/grid 的语义容器并说明场景',
      match.match,
    ));
  }

  for (const match of findMatches(content, /\bclass=["'][^"']*\bml-4\b[^"']*["']/g)) {
    const aroundMatch = content
      .split(/\r?\n/)
      .slice(Math.max(0, match.lineNumber - 8), match.lineNumber + 7)
      .join('\n');

    if (/w-[1-5]\/[1-6]/.test(aroundMatch) || /Tree|Card|VbenVxeGrid|Split/.test(aroundMatch)) {
      issues.push(issue(
        'LEGACY_MARGIN_SPLIT_LAYOUT',
        'ERROR',
        filePath,
        match.lineNumber,
        '业务视图不得用 ml-4 拼接左右分栏间距，间距应由共享布局统一控制',
        '左右分栏列表页请使用 SplitListLayout 的 gap；普通按钮间距请改用组件级 gap 或更明确的局部类',
        match.match,
      ));
    }
  }

  if (usesSplitLayout) {
    if (!/autoResize\s*:\s*true/.test(content)) {
      issues.push(issue(
        'SPLIT_LAYOUT_GRID_MISSING_AUTO_RESIZE',
        'ERROR',
        filePath,
        1,
        '使用 SplitListLayout 的表格页必须启用 VXE autoResize，避免父容器尺寸变化后表格不同步',
        '在对应 useVbenVxeGrid 的 gridOptions 中补充 autoResize: true',
      ));
    }
    if (!/height\s*:\s*['"]100%['"]/.test(content)) {
      issues.push(issue(
        'SPLIT_LAYOUT_GRID_HEIGHT_NOT_FULL',
        'ERROR',
        filePath,
        1,
        '使用 SplitListLayout 的表格页必须使用 height: 100%，避免表格高度持续变小或不能填满工作区',
        '在对应 useVbenVxeGrid 的 gridOptions 中改为 height: "100%"',
      ));
    }
  }

  if (usesVxeGrid && /height\s*:\s*['"]auto['"]/.test(content)) {
    autoHeightGridFiles.push(relative(filePath));
  }
}

const errorIssues = issues.filter((item) => item.severity === 'ERROR');
const rules = [
  rule(
    'SPLIT_LIST_LAYOUT_COMPONENT_PRESENT',
    'FRONTEND_LAYOUT',
    'ERROR',
    splitLayoutExists,
    '后台左右分栏列表共享布局组件必须存在',
    '恢复 web/playground/src/views/shared/split-list-layout.vue',
  ),
  rule(
    'SPLIT_LIST_LAYOUT_IS_USED',
    'FRONTEND_LAYOUT',
    'ERROR',
    splitLayoutUsers.length >= 3,
    '已治理的员工、合同、系统用户等页面必须复用共享分栏布局',
    '将左右分栏列表页迁移到 SplitListLayout，避免复制页面级布局 CSS',
  ),
  rule(
    'NO_LEGACY_SPLIT_LAYOUT_CLASSES',
    'FRONTEND_LAYOUT',
    'ERROR',
    !issues.some((item) => item.code === 'LEGACY_FRACTION_WIDTH_LAYOUT' || item.code === 'LEGACY_MARGIN_SPLIT_LAYOUT'),
    '业务视图不得回退到 w-*/ml-* 拼接左右分栏布局',
    '使用 SplitListLayout 或语义化 flex/grid 容器替代旧布局',
  ),
  rule(
    'SPLIT_LAYOUT_TABLES_HAVE_STABLE_HEIGHT',
    'FRONTEND_LAYOUT',
    'ERROR',
    !issues.some((item) => item.code === 'SPLIT_LAYOUT_GRID_MISSING_AUTO_RESIZE' || item.code === 'SPLIT_LAYOUT_GRID_HEIGHT_NOT_FULL'),
    '共享分栏布局内的 VXE 表格必须有稳定高度和自动 resize',
    '在 gridOptions 中补齐 autoResize: true 和 height: "100%"',
  ),
  rule(
    'AUTO_HEIGHT_TABLES_REMAIN_REVIEWABLE',
    'FRONTEND_LAYOUT',
    'WARN',
    autoHeightGridFiles.length === 0,
    '仍存在 height: auto 的 VXE 表格页面，需要后续按页面复杂度逐步治理',
    '优先治理宽表格、带筛选表单、带固定操作列或嵌套容器的页面',
  ),
];

const report = {
  product: {
    name: 'ONES-ADMIN',
    version: productVersion,
  },
  generatedAtUtc,
  scanPolicy: {
    roots: [relative(viewsDir)],
    excludedPathParts: excludedPathParts.map((part) => part.replaceAll(path.sep, '/')),
    readsIgnoredSensitiveSources: false,
    ruleScope: '只扫描可提交前端视图源码；示例、演示、仪表盘和核心鉴权页不纳入业务布局硬门禁',
  },
  layout: {
    vueFileCount: vueFiles.length,
    vxeGridFileCount: vxeGridFiles.length,
    splitLayoutComponent: relative(splitLayoutPath),
    splitLayoutExists,
    splitLayoutUserCount: splitLayoutUsers.length,
    splitLayoutUsers,
    autoHeightGridFileCount: autoHeightGridFiles.length,
    autoHeightGridFiles,
    issueCount: issues.length,
    errorIssueCount: errorIssues.length,
    issues,
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
  console.error(`Frontend layout governance failed: ${report.governance.errorCount} error(s).`);
  process.exit(1);
}

console.log(`Frontend layout report written to ${outputFile}`);
NODE

node -e "JSON.parse(require('node:fs').readFileSync(process.argv[1], 'utf8'))" "$OUTPUT_FILE"
