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

const brandPrinciples = [
  {
    description: '所有模块在同一体系下协同运作',
    icon: 'lucide:boxes',
    title: 'ONE SYSTEM',
  },
  {
    description: '服务稳定交付，能力沉淀复用',
    icon: 'lucide:layers-3',
    title: 'ONE SERVICE',
  },
  {
    description: '安全内建，守护核心业务系统',
    icon: 'lucide:shield-check',
    title: 'ONE SAFE',
  },
  {
    description: '以秒级响应支撑高效执行',
    icon: 'lucide:timer',
    title: 'ONE SECOND',
  },
];

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
        <div class="flex items-center gap-3">
          <img
            alt="ONES-ADMIN"
            class="size-10 rounded-lg object-cover"
            src="/brand/ones-1s-app-icon-192.png"
          />
          <div>
            <div class="text-lg font-medium">ONES-ADMIN</div>
            <div class="text-muted-foreground mt-1 text-sm">
              数据来自当前后端数据库，更新时间：{{ generatedAt || '-' }}
            </div>
          </div>
        </div>
        <Button :loading="loading" @click="loadOverview">
          <template #icon>
            <IconifyIcon icon="lucide:refresh-cw" />
          </template>
          刷新
        </Button>
      </div>

      <div class="mb-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Card
          v-for="item in brandPrinciples"
          :key="item.title"
          variant="borderless"
        >
          <div class="flex items-start gap-3">
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon :icon="item.icon" class="size-5" />
            </div>
            <div>
              <div class="font-medium">{{ item.title }}</div>
              <div class="text-muted-foreground mt-1 text-sm">
                {{ item.description }}
              </div>
            </div>
          </div>
        </Card>
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
