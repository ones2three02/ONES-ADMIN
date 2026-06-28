<script lang="ts" setup>
import type { SystemOverviewApi } from '#/api/system/overview';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Alert, Button, Card, Skeleton, Statistic } from 'antdv-next';

import { getSystemOverview } from '#/api/system/overview';

defineOptions({ name: 'SystemOverview' });

const overview = ref<SystemOverviewApi.SystemOverview>();
const loading = ref(false);
const loadError = ref('');

const metrics = computed(() => {
  if (!overview.value) {
    return [];
  }
  return [
    {
      icon: 'lucide:users',
      label: '用户总数',
      value: overview.value.userCount,
    },
    {
      icon: 'lucide:user-check',
      label: '启用用户',
      value: overview.value.enabledUserCount,
    },
    {
      icon: 'lucide:shield-check',
      label: '角色数量',
      value: overview.value.roleCount,
    },
    {
      icon: 'lucide:route',
      label: '菜单节点',
      value: overview.value.menuCount,
    },
    {
      icon: 'lucide:building-2',
      label: '部门数量',
      value: overview.value.deptCount,
    },
  ];
});

const generatedAt = computed(() => {
  if (!overview.value?.generatedAt) {
    return '';
  }
  return new Date(overview.value.generatedAt).toLocaleString('zh-CN', {
    hour12: false,
  });
});

async function loadOverview() {
  loading.value = true;
  loadError.value = '';
  try {
    overview.value = await getSystemOverview();
  } catch {
    loadError.value = '系统概览加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadOverview();
});
</script>

<template>
  <Page auto-content-height title="系统概览">
    <div class="p-4">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <div class="text-lg font-medium">ONES-ADMIN</div>
          <div class="text-muted-foreground mt-1 text-sm">
            数据来自当前后端数据库，更新时间：{{ generatedAt || '-' }}
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
        class="mb-4"
        show-icon
        type="error"
        :message="loadError"
      />

      <Skeleton v-if="loading && !overview" active />

      <div v-else class="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <Card v-for="item in metrics" :key="item.label" variant="borderless">
          <div class="flex items-start justify-between gap-3">
            <Statistic :title="item.label" :value="item.value" />
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon :icon="item.icon" class="size-5" />
            </div>
          </div>
        </Card>
      </div>
    </div>
  </Page>
</template>
