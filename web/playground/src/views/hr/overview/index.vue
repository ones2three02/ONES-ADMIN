<script lang="ts" setup>
import type { HrOverviewApi } from '#/api';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { preferences } from '@vben/preferences';

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
      title: $t('hr.overview.metricEmployeeCount'),
      value: overview.value.employeeCount,
    },
    {
      icon: 'lucide:user-check',
      title: $t('hr.overview.metricActiveEmployeeCount'),
      value: overview.value.activeEmployeeCount,
    },
    {
      icon: 'lucide:badge-check',
      title: $t('hr.overview.metricProbationEmployeeCount'),
      value: overview.value.probationEmployeeCount,
    },
    {
      icon: 'lucide:file-clock',
      title: $t('hr.overview.metricExpiringContractCount'),
      value: overview.value.expiringContractCount,
    },
    {
      icon: 'lucide:files',
      title: $t('hr.overview.metricExpiringDocumentCount'),
      value: overview.value.expiringDocumentCount,
    },
    {
      icon: 'lucide:calendar-check',
      title: $t('hr.overview.metricProbationDueCount'),
      value: overview.value.probationDueCount,
    },
  ];
});

const generatedAt = computed(() => {
  if (!overview.value?.generatedAt) {
    return '-';
  }
  return new Date(overview.value.generatedAt).toLocaleString(preferences.app.locale, {
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
    loadError.value = $t('hr.overview.loadError');
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
          <div class="text-lg font-medium">{{ $t('hr.overview.title') }}</div>
          <div class="text-muted-foreground mt-1 text-sm">
            {{ $t('hr.overview.description', { time: generatedAt }) }}
          </div>
        </div>
        <Button :loading="loading" @click="loadOverview">
          <template #icon>
            <IconifyIcon icon="lucide:refresh-cw" />
          </template>
          {{ $t('common.refresh') }}
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
        <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
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
          <Card variant="borderless" :title="$t('hr.overview.employmentStatusStats')">
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

          <Card variant="borderless" :title="$t('hr.overview.departmentStats')">
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

          <Card variant="borderless" :title="$t('hr.overview.lifecycleEventStats')">
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
            <Statistic
              :title="$t('hr.overview.departmentCount')"
              :value="overview.departmentCount"
            />
          </Card>
          <Card variant="borderless">
            <Statistic
              :title="$t('hr.overview.activeContractCount')"
              :value="overview.activeContractCount"
            />
          </Card>
          <Card variant="borderless">
            <Statistic
              :title="$t('hr.overview.expiredDocumentCount')"
              :value="overview.expiredDocumentCount"
            />
          </Card>
          <Card variant="borderless">
            <Statistic
              :title="$t('hr.overview.resignedEmployeeCount')"
              :value="overview.resignedEmployeeCount"
            />
          </Card>
        </div>
      </template>
    </div>
  </Page>
</template>
