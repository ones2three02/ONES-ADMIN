<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SystemApiResourceApi } from '#/api';

import { computed, h, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Alert, Button, Card, Skeleton, Statistic, Tag } from 'antdv-next';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getApiResourceGovernanceReport,
  getApiResourceList,
} from '#/api';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './table';

defineOptions({ name: 'SystemApiResources' });

const governanceReport =
  ref<SystemApiResourceApi.ApiResourceGovernanceReport>();
const loadingOverview = ref(false);
const loadError = ref('');

const summary = computed(() => governanceReport.value?.summary);
const governance = computed(() => governanceReport.value?.governance);
const releaseReadiness = computed(() => governanceReport.value?.releaseReadiness);
const releaseGate = computed(() => governanceReport.value?.latestGate.gate);
const gateChecks = computed(() => releaseGate.value?.checks.slice(0, 4) ?? []);
const governanceRules = computed(
  () => governanceReport.value?.rules.rules.slice(0, 6) ?? [],
);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getApiResourceList({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: {
      keyField: 'apiKey',
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<SystemApiResourceApi.ApiResource>,
});

const governanceStatus = computed(() => {
  if (!governance.value) {
    return {
      color: 'processing',
      text: '加载中',
    };
  }
  if (governance.value.errorCount > 0) {
    return {
      color: 'error',
      text: '治理阻断',
    };
  }
  if (governance.value.warningCount > 0) {
    return {
      color: 'warning',
      text: '存在警告',
    };
  }
  return {
    color: 'success',
    text: '治理通过',
  };
});

const releaseReadinessStatus = computed(() => {
  const readiness = releaseReadiness.value;
  if (!readiness) {
    return {
      color: 'processing',
      text: '加载中',
    };
  }
  if (readiness.status === 'READY') {
    return {
      color: 'success',
      text: '发布就绪',
    };
  }
  if (readiness.status === 'READY_WITH_WARNINGS') {
    return {
      color: 'warning',
      text: '带警告',
    };
  }
  if (readiness.status === 'BASELINE_REQUIRED') {
    return {
      color: 'warning',
      text: '基线待归档',
    };
  }
  if (readiness.status === 'MANUAL_REVIEW_REQUIRED') {
    return {
      color: 'warning',
      text: '需复核',
    };
  }
  if (readiness.status === 'BLOCKED') {
    return {
      color: 'error',
      text: '发布阻断',
    };
  }
  return {
    color: 'processing',
    text: '待检查',
  };
});

const metrics = computed(() => [
  {
    icon: 'lucide:route',
    title: '接口总数',
    value: summary.value?.total ?? 0,
  },
  {
    icon: 'lucide:file-pen-line',
    title: '写操作',
    value: summary.value?.writeOperationCount ?? 0,
  },
  {
    icon: 'lucide:shield-alert',
    title: '权限缺失',
    value: summary.value?.permissionMissingCount ?? 0,
  },
  {
    icon: 'lucide:archive-x',
    title: '废弃接口',
    value: summary.value?.deprecatedCount ?? 0,
  },
]);

const gateStatus = computed(() => {
  if (!releaseGate.value) {
    return {
      color: 'processing',
      text: '加载中',
    };
  }
  if (releaseGate.value.passed) {
    return {
      color: 'success',
      text: '发布通过',
    };
  }
  if (releaseGate.value.requiredManualReview) {
    return {
      color: 'warning',
      text: '需要复核',
    };
  }
  return {
    color: 'error',
    text: '发布阻断',
  };
});

const topViolations = computed(() => {
  return governance.value?.violations.slice(0, 3) ?? [];
});
const ownerStats = computed(() => summary.value?.owners.slice(0, 4) ?? []);
const audienceStats = computed(() => summary.value?.audiences.slice(0, 4) ?? []);
const governanceRuleSummaries = computed(
  () => governance.value?.ruleSummaries.slice(0, 4) ?? [],
);
const governanceCategorySummaries = computed(
  () => governance.value?.categorySummaries ?? [],
);

async function loadOverview() {
  loadingOverview.value = true;
  loadError.value = '';
  try {
    governanceReport.value = await getApiResourceGovernanceReport();
  } catch {
    loadError.value = '接口治理报告加载失败，请稍后重试';
  } finally {
    loadingOverview.value = false;
  }
}

function onRefresh() {
  loadOverview();
  gridApi.query();
}

function renderGovernanceTag() {
  return h(
    Tag,
    {
      color: governanceStatus.value.color,
    },
    () => governanceStatus.value.text,
  );
}

function getSeverityColor(severity: string) {
  if (severity === 'ERROR') {
    return 'error';
  }
  if (severity === 'WARN') {
    return 'warning';
  }
  return 'processing';
}

onMounted(() => {
  loadOverview();
});
</script>

<template>
  <Page auto-content-height title="接口管理">
    <div class="p-4" data-testid="api-resource-page">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <div class="flex items-center gap-2 text-lg font-medium">
            <IconifyIcon class="size-5 text-primary" icon="lucide:network" />
            接口管理
          </div>
          <div class="text-muted-foreground mt-1 text-sm">
            统一查看接口资源清单、权限策略、生命周期和发布治理状态。
          </div>
        </div>
        <Button :loading="loadingOverview" @click="onRefresh">
          <template #icon>
            <IconifyIcon icon="lucide:refresh-cw" />
          </template>
          刷新
        </Button>
      </div>

      <Alert
        v-if="loadError"
        class="mb-4"
        show-icon
        type="error"
        :message="loadError"
      />

      <div class="mb-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
        <Card variant="borderless">
          <div class="flex items-start justify-between gap-3">
            <Statistic title="治理状态" :value="governanceStatus.text" />
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon icon="lucide:shield-check" class="size-5" />
            </div>
          </div>
          <div class="mt-3">
            <component :is="renderGovernanceTag" />
          </div>
        </Card>

        <Card variant="borderless">
          <div class="flex items-start justify-between gap-3">
            <Statistic title="发布就绪" :value="releaseReadinessStatus.text" />
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon icon="lucide:rocket" class="size-5" />
            </div>
          </div>
          <div class="mt-3 flex items-center gap-2">
            <Tag :color="releaseReadinessStatus.color">
              {{ releaseReadiness?.priority ?? 'P1' }}
            </Tag>
            <span class="text-muted-foreground truncate text-xs">
              {{ releaseReadiness?.nextActionTitle || '无阻断动作' }}
            </span>
          </div>
        </Card>

        <Card
          v-for="item in metrics"
          :key="item.title"
          variant="borderless"
        >
          <div class="flex items-start justify-between gap-3">
            <Statistic :title="item.title" :value="item.value" />
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon :icon="item.icon" class="size-5" />
            </div>
          </div>
        </Card>
      </div>

      <div
        v-if="governanceReport"
        class="mb-4 grid gap-4 xl:grid-cols-2"
      >
        <Card variant="borderless">
          <div class="mb-4 flex items-start justify-between gap-3">
            <div>
              <div class="flex items-center gap-2 font-medium">
                <IconifyIcon class="size-4 text-primary" icon="lucide:users" />
                目录分布
              </div>
              <div class="text-muted-foreground mt-1 text-xs">
                按负责人和主要调用方归集接口资产，便于权限授权、联调和版本责任追踪。
              </div>
            </div>
            <Tag color="processing">
              {{ summary?.modules.length ?? 0 }} 个模块
            </Tag>
          </div>

          <div class="grid gap-4 lg:grid-cols-2">
            <div>
              <div class="text-muted-foreground mb-2 text-xs">接口负责人</div>
              <div class="space-y-2">
                <div
                  v-for="owner in ownerStats"
                  :key="owner.owner"
                  class="rounded border border-border p-3"
                >
                  <div class="flex items-center justify-between gap-3">
                    <span class="truncate text-sm font-medium">
                      {{ owner.owner }}
                    </span>
                    <Tag color="blue">{{ owner.total }} 个</Tag>
                  </div>
                  <div class="text-muted-foreground mt-2 flex flex-wrap gap-3 text-xs">
                    <span>写操作 {{ owner.writeOperationCount }}</span>
                    <span>高风险 {{ owner.highRiskCount }}</span>
                    <span>权限缺失 {{ owner.permissionMissingCount }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <div class="text-muted-foreground mb-2 text-xs">主要调用方</div>
              <div class="space-y-2">
                <div
                  v-for="audience in audienceStats"
                  :key="audience.audience"
                  class="rounded border border-border p-3"
                >
                  <div class="flex items-center justify-between gap-3">
                    <span class="truncate text-sm font-medium">
                      {{ audience.audience }}
                    </span>
                    <Tag color="cyan">{{ audience.total }} 个</Tag>
                  </div>
                  <div class="text-muted-foreground mt-2 flex flex-wrap gap-3 text-xs">
                    <span>权限接口 {{ audience.permissionCount }}</span>
                    <span>公开接口 {{ audience.publicCount }}</span>
                    <span>写操作 {{ audience.writeOperationCount }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Card>

        <Card variant="borderless">
          <div class="mb-4 flex items-start justify-between gap-3">
            <div>
              <div class="flex items-center gap-2 font-medium">
                <IconifyIcon
                  class="size-4 text-primary"
                  icon="lucide:clipboard-check"
                />
                治理分布
              </div>
              <div class="text-muted-foreground mt-1 text-xs">
                按规则和治理分类汇总违规结果，给 CI 门禁、接口评审和迭代排期提供统一口径。
              </div>
            </div>
            <Tag :color="governanceStatus.color">
              {{ governance?.violationCount ?? 0 }} 个问题
            </Tag>
          </div>

          <div
            v-if="governanceCategorySummaries.length === 0"
            class="rounded border border-dashed border-border p-4"
          >
            <div class="flex items-center gap-2 text-sm font-medium">
              <IconifyIcon class="size-4 text-success" icon="lucide:circle-check" />
              当前无治理违规
            </div>
            <div class="text-muted-foreground mt-2 text-xs">
              现有接口目录、权限策略、生命周期和文档元数据均满足发布门禁要求。
            </div>
          </div>

          <div v-else class="grid gap-4 lg:grid-cols-2">
            <div>
              <div class="text-muted-foreground mb-2 text-xs">分类汇总</div>
              <div class="space-y-2">
                <div
                  v-for="category in governanceCategorySummaries"
                  :key="category.category"
                  class="rounded border border-border p-3"
                >
                  <div class="flex items-center justify-between gap-3">
                    <span class="truncate text-sm font-medium">
                      {{ category.category }}
                    </span>
                    <Tag :color="category.errorCount > 0 ? 'error' : 'warning'">
                      {{ category.violationCount }} 个
                    </Tag>
                  </div>
                  <div class="text-muted-foreground mt-2 flex gap-3 text-xs">
                    <span>阻断 {{ category.errorCount }}</span>
                    <span>警告 {{ category.warningCount }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <div class="text-muted-foreground mb-2 text-xs">规则命中</div>
              <div class="space-y-2">
                <div
                  v-for="rule in governanceRuleSummaries"
                  :key="rule.ruleCode"
                  class="rounded border border-border p-3"
                >
                  <div class="mb-2 flex items-center justify-between gap-2">
                    <div class="flex min-w-0 items-center gap-2">
                      <Tag :color="getSeverityColor(rule.severity)">
                        {{ rule.severity }}
                      </Tag>
                      <span class="truncate text-xs font-medium">
                        {{ rule.ruleCode }}
                      </span>
                    </div>
                    <span class="text-muted-foreground text-xs">
                      {{ rule.count }} 次
                    </span>
                  </div>
                  <div class="line-clamp-2 text-sm">{{ rule.description }}</div>
                </div>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <div
        v-if="governanceReport"
        class="mb-4 grid gap-4 xl:grid-cols-2"
      >
        <Card variant="borderless">
          <div class="mb-4 flex items-start justify-between gap-3">
            <div>
              <div class="flex items-center gap-2 font-medium">
                <IconifyIcon
                  class="size-4 text-primary"
                  icon="lucide:shield-check"
                />
                发布门禁
              </div>
              <div class="text-muted-foreground mt-1 text-xs">
                对接口治理、破坏性变更和人工复核要求进行统一放行判断。
              </div>
            </div>
            <Tag :color="gateStatus.color">{{ gateStatus.text }}</Tag>
          </div>

          <div class="mb-4 grid gap-3 sm:grid-cols-3">
            <div class="rounded border border-border p-3">
              <div class="text-muted-foreground text-xs">当前版本</div>
              <div class="mt-1 text-sm font-medium">
                {{ releaseGate?.currentVersion || governanceReport.applicationVersion }}
              </div>
            </div>
            <div class="rounded border border-border p-3">
              <div class="text-muted-foreground text-xs">破坏性变更</div>
              <div class="mt-1 text-sm font-medium">
                {{ releaseGate?.breakingChangeCount ?? 0 }}
              </div>
            </div>
            <div class="rounded border border-border p-3">
              <div class="text-muted-foreground text-xs">人工复核</div>
              <div class="mt-1 text-sm font-medium">
                {{ releaseGate?.requiredManualReview ? '需要' : '不需要' }}
              </div>
            </div>
            <div class="rounded border border-border p-3">
              <div class="text-muted-foreground text-xs">阻断检查</div>
              <div class="mt-1 text-sm font-medium">
                {{ releaseReadiness?.blockingCheckCount ?? 0 }}
              </div>
            </div>
            <div class="rounded border border-border p-3">
              <div class="text-muted-foreground text-xs">打开动作</div>
              <div class="mt-1 text-sm font-medium">
                {{ releaseReadiness?.openActionCount ?? 0 }}
              </div>
            </div>
            <div class="rounded border border-border p-3">
              <div class="text-muted-foreground text-xs">基线状态</div>
              <div class="mt-1 text-sm font-medium">
                {{ releaseReadiness?.baselineAvailable ? '已归档' : '待归档' }}
              </div>
            </div>
          </div>

          <div class="mb-4 rounded border border-border p-3">
            <div class="mb-2 flex items-center justify-between gap-3">
              <span class="text-sm font-medium">
                {{ releaseReadiness?.nextActionTitle || '发布动作已闭环' }}
              </span>
              <Tag :color="releaseReadinessStatus.color">
                {{ releaseReadiness?.status || 'UNKNOWN' }}
              </Tag>
            </div>
            <div class="text-muted-foreground line-clamp-2 text-xs">
              {{ releaseReadiness?.message }}
            </div>
          </div>

          <div class="space-y-3">
            <div
              v-for="check in gateChecks"
              :key="check.checkCode"
              class="rounded border border-border p-3"
            >
              <div class="mb-2 flex items-center justify-between gap-2">
                <Tag :color="getSeverityColor(check.severity)">
                  {{ check.severity }}
                </Tag>
                <span class="text-muted-foreground truncate text-xs">
                  {{ check.checkCode }}
                </span>
              </div>
              <div class="text-sm font-medium">{{ check.message }}</div>
              <div class="text-muted-foreground mt-2 line-clamp-2 text-xs">
                {{ check.remediation }}
              </div>
            </div>
          </div>
        </Card>

        <Card variant="borderless">
          <div class="mb-4 flex items-start justify-between gap-3">
            <div>
              <div class="flex items-center gap-2 font-medium">
                <IconifyIcon
                  class="size-4 text-primary"
                  icon="lucide:list-checks"
                />
                治理规则
              </div>
              <div class="text-muted-foreground mt-1 text-xs">
                规则编码与后端校验结果保持一致，便于 CI、前端和人工巡检统一解释。
              </div>
            </div>
            <Tag color="processing">
              {{ governanceReport.rules.rules.length }} 条
            </Tag>
          </div>

          <div class="space-y-3">
            <div
              v-for="rule in governanceRules"
              :key="rule.ruleCode"
              class="rounded border border-border p-3"
            >
              <div class="mb-2 flex items-center justify-between gap-2">
                <div class="flex min-w-0 items-center gap-2">
                  <Tag :color="getSeverityColor(rule.severity)">
                    {{ rule.severity }}
                  </Tag>
                  <span class="truncate text-xs font-medium">
                    {{ rule.ruleCode }}
                  </span>
                </div>
                <Tag v-if="rule.blocking" color="error">阻断</Tag>
              </div>
              <div class="text-sm font-medium">{{ rule.description }}</div>
              <div class="text-muted-foreground mt-2 line-clamp-2 text-xs">
                {{ rule.remediation }}
              </div>
            </div>
          </div>
        </Card>
      </div>

      <Card v-if="topViolations.length > 0" class="mb-4" variant="borderless">
        <div class="mb-3 flex items-center gap-2 font-medium">
          <IconifyIcon class="size-4 text-warning" icon="lucide:triangle-alert" />
          治理提示
        </div>
        <div class="grid gap-3 xl:grid-cols-3">
          <div
            v-for="violation in topViolations"
            :key="`${violation.ruleCode}-${violation.method}-${violation.path}`"
            class="rounded border border-border p-3"
          >
            <div class="mb-2 flex items-center justify-between gap-2">
              <Tag :color="violation.severity === 'ERROR' ? 'error' : 'warning'">
                {{ violation.severity }}
              </Tag>
              <span class="text-muted-foreground truncate text-xs">
                {{ violation.ruleCode }}
              </span>
            </div>
            <div class="text-sm font-medium">{{ violation.message }}</div>
            <div class="text-muted-foreground mt-2 line-clamp-2 text-xs">
              {{ violation.remediation }}
            </div>
          </div>
        </div>
      </Card>

      <Skeleton v-if="loadingOverview && !governanceReport" active />

      <Grid v-else table-title="接口资源清单">
        <template #toolbar-tools>
          <Button @click="onRefresh">
            <template #icon>
              <IconifyIcon icon="lucide:refresh-cw" />
            </template>
            {{ $t('common.refresh') }}
          </Button>
        </template>
      </Grid>
    </div>
  </Page>
</template>
