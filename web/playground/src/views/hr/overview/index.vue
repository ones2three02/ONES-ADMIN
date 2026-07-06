<script lang="ts" setup>
import type { HrOverviewApi } from '#/api';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Alert, Button, Card, Empty, Skeleton, Statistic, Tag } from 'antdv-next';

import { getHrOverview } from '#/api';
import { $t } from '#/locales';

import {
  getHrDictOption,
  getHrDictOptions,
  HR_EMPLOYMENT_STATUS_DICT,
} from '../dict-options';

defineOptions({ name: 'HrOverview' });

const overview = ref<HrOverviewApi.Overview>();
const loading = ref(false);
const loadError = ref('');

const metrics = computed(() => {
  if (!overview.value) {
    return [];
  }
  return [
    {
      icon: 'lucide:users',
      title: '员工总数',
      value: overview.value.employeeCount,
    },
    {
      icon: 'lucide:user-check',
      title: '在职员工',
      value: overview.value.activeEmployeeCount,
    },
    {
      icon: 'lucide:badge-check',
      title: '试用期员工',
      value: overview.value.probationEmployeeCount,
    },
    {
      icon: 'lucide:file-clock',
      title: '30天内到期合同',
      value: overview.value.expiringContractCount,
    },
    {
      icon: 'lucide:calendar-check',
      title: '30天内待转正',
      value: overview.value.probationDueCount,
    },
  ];
});

const generatedAt = computed(() => {
  if (!overview.value?.generatedAt) {
    return '-';
  }
  return new Date(overview.value.generatedAt).toLocaleString('zh-CN', {
    hour12: false,
  });
});

const totalLifecycleEvents = computed(() =>
  sumMetricValues(overview.value?.lifecycleEventStats ?? []),
);

function sumMetricValues(items: HrOverviewApi.MetricItem[]) {
  return items.reduce((total, item) => total + item.value, 0);
}

function percent(value: number, total: number) {
  if (total <= 0) {
    return '0%';
  }
  return `${Math.round((value / total) * 100)}%`;
}

function statusColor(code: string) {
  return getHrDictOption(HR_EMPLOYMENT_STATUS_DICT, code)?.color ?? 'default';
}

async function loadOverview() {
  loading.value = true;
  loadError.value = '';
  try {
    overview.value = await getHrOverview();
  } catch {
    loadError.value = '人力概览加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await getHrDictOptions(HR_EMPLOYMENT_STATUS_DICT);
  await loadOverview();
});
</script>

<template>
  <Page auto-content-height :title="$t('hr.overview.title')">
    <div class="flex size-full flex-col gap-4 p-4">
      <div class="flex items-center justify-between gap-3">
        <div>
          <div class="text-lg font-medium">人力概览</div>
          <div class="text-muted-foreground mt-1 text-sm">
            数据来自 HRMS 当前主数据、合同和生命周期事件，更新时间：{{ generatedAt }}
          </div>
        </div>
        <Button :loading="loading" @click="loadOverview">
          <template #icon>
            <IconifyIcon icon="lucide:refresh-cw" />
          </template>
          刷新
        </Button>
      </div>

      <Alert
        v-if="loadError"
        show-icon
        type="error"
        :message="loadError"
      />

      <Skeleton v-if="loading && !overview" active />

      <template v-else-if="overview">
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          <Card v-for="item in metrics" :key="item.title" variant="borderless">
            <div class="flex items-start justify-between gap-3">
              <Statistic :title="item.title" :value="item.value" />
              <div class="rounded bg-muted p-2 text-primary">
                <IconifyIcon :icon="item.icon" class="size-5" />
              </div>
            </div>
          </Card>
        </div>

        <div class="grid gap-4 xl:grid-cols-3">
          <Card variant="borderless" title="员工状态分布">
            <div class="flex flex-col gap-3">
              <div
                v-for="item in overview.employmentStatusStats"
                :key="item.code"
                class="flex items-center justify-between gap-3"
              >
                <div class="flex items-center gap-2">
                  <Tag :color="statusColor(item.code)">{{ item.name }}</Tag>
                  <span class="text-muted-foreground text-sm">
                    {{ percent(item.value, overview.employeeCount) }}
                  </span>
                </div>
                <span class="font-medium">{{ item.value }}</span>
              </div>
            </div>
          </Card>

          <Card variant="borderless" title="部门员工分布">
            <div v-if="overview.departmentStats.length" class="flex flex-col gap-3">
              <div
                v-for="item in overview.departmentStats"
                :key="item.code"
                class="flex items-center justify-between gap-3"
              >
                <span class="truncate">{{ item.name }}</span>
                <div class="flex items-center gap-2">
                  <span class="text-muted-foreground text-sm">
                    {{ percent(item.value, overview.employeeCount) }}
                  </span>
                  <span class="font-medium">{{ item.value }}</span>
                </div>
              </div>
            </div>
            <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </Card>

          <Card variant="borderless" title="近30天生命周期事件">
            <div class="flex flex-col gap-3">
              <div
                v-for="item in overview.lifecycleEventStats"
                :key="item.code"
                class="flex items-center justify-between gap-3"
              >
                <span>{{ item.name }}</span>
                <div class="flex items-center gap-2">
                  <span class="text-muted-foreground text-sm">
                    {{ percent(item.value, totalLifecycleEvents) }}
                  </span>
                  <span class="font-medium">{{ item.value }}</span>
                </div>
              </div>
            </div>
          </Card>
        </div>

        <div class="grid gap-4 lg:grid-cols-3">
          <Card variant="borderless">
            <Statistic title="部门数量" :value="overview.departmentCount" />
          </Card>
          <Card variant="borderless">
            <Statistic title="有效合同" :value="overview.activeContractCount" />
          </Card>
          <Card variant="borderless">
            <Statistic title="已离职员工" :value="overview.resignedEmployeeCount" />
          </Card>
        </div>
      </template>
    </div>
  </Page>
</template>
