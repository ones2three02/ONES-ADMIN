<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SystemAuditApi } from '#/api';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlob } from '@vben/utils';

import { Button, Card, message, Modal, Statistic, TabPane, Tabs, Tag } from 'antdv-next';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  cleanupExpiredAuditLogs,
  exportLoginLogs,
  exportOperationLogs,
  getAuditRetention,
  getLoginLogList,
  getOperationLogList,
} from '#/api';

import {
  useLoginLogColumns,
  useLoginLogFormSchema,
  useOperationLogColumns,
  useOperationLogFormSchema,
} from './data';

defineOptions({ name: 'SystemAudit' });

const activeTab = ref('login');
const retention = ref<SystemAuditApi.AuditRetentionSummary>();
const loadingRetention = ref(false);
const cleanupLoading = ref(false);
const latestLoginQuery = ref<Partial<SystemAuditApi.LoginLogQuery>>({});
const latestOperationQuery = ref<Partial<SystemAuditApi.OperationLogQuery>>({});

const [LoginGrid, loginGridApi] = useVbenVxeGrid({
  formOptions: {
    fieldMappingTime: [['createdAt', ['startTime', 'endTime'], 'YYYY-MM-DDTHH:mm:ss']],
    schema: useLoginLogFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    autoResize: true,
    columns: useLoginLogColumns(),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          latestLoginQuery.value = formValues;
          return await getLoginLogList({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<SystemAuditApi.LoginLog>,
});

const [OperationGrid, operationGridApi] = useVbenVxeGrid({
  formOptions: {
    fieldMappingTime: [['createdAt', ['startTime', 'endTime'], 'YYYY-MM-DDTHH:mm:ss']],
    schema: useOperationLogFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    autoResize: true,
    columns: useOperationLogColumns(),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          latestOperationQuery.value = formValues;
          return await getOperationLogList({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<SystemAuditApi.OperationLog>,
});

const totalExpiredCount = computed(() => {
  return (
    (retention.value?.expiredLoginLogCount ?? 0) +
    (retention.value?.expiredOperationLogCount ?? 0)
  );
});

const metrics = computed(() => [
  {
    icon: 'lucide:log-in',
    title: '登录日志保留',
    suffix: '天',
    value: retention.value?.loginLogRetentionDays ?? 0,
  },
  {
    icon: 'lucide:scroll-text',
    title: '操作日志保留',
    suffix: '天',
    value: retention.value?.operationLogRetentionDays ?? 0,
  },
  {
    icon: 'lucide:archive-x',
    title: '过期登录日志',
    suffix: '条',
    value: retention.value?.expiredLoginLogCount ?? 0,
  },
  {
    icon: 'lucide:shield-alert',
    title: '过期操作日志',
    suffix: '条',
    value: retention.value?.expiredOperationLogCount ?? 0,
  },
]);

async function loadRetention() {
  loadingRetention.value = true;
  try {
    retention.value = await getAuditRetention();
  } catch (error: any) {
    message.error(error?.message || '审计保留策略加载失败');
  } finally {
    loadingRetention.value = false;
  }
}

function onRefresh() {
  loadRetention();
  if (activeTab.value === 'login') {
    loginGridApi.query();
  } else {
    operationGridApi.query();
  }
}

async function onExportLoginLogs() {
  const hide = message.loading('正在导出登录日志...', 0);
  try {
    const blob = await exportLoginLogs(latestLoginQuery.value);
    downloadFileFromBlob({
      fileName: 'ones-login-logs.csv',
      source: blob,
    });
    message.success('导出成功');
  } catch (error: any) {
    message.error(error?.message || '导出失败，请稍后重试');
  } finally {
    hide();
  }
}

async function onExportOperationLogs() {
  const hide = message.loading('正在导出操作日志...', 0);
  try {
    const blob = await exportOperationLogs(latestOperationQuery.value);
    downloadFileFromBlob({
      fileName: 'ones-operation-logs.csv',
      source: blob,
    });
    message.success('导出成功');
  } catch (error: any) {
    message.error(error?.message || '导出失败，请稍后重试');
  } finally {
    hide();
  }
}

function onCleanupExpiredLogs() {
  Modal.confirm({
    content:
      '将删除已超过保留周期的登录日志和操作日志。该操作不可恢复，请确认已完成必要审计留档。',
    okButtonProps: {
      danger: true,
    },
    okText: '确认清理',
    title: '清理过期审计日志',
    async onOk() {
      cleanupLoading.value = true;
      try {
        const result = await cleanupExpiredAuditLogs();
        message.success(
          `清理完成：登录日志 ${result.deletedLoginLogCount} 条，操作日志 ${result.deletedOperationLogCount} 条`,
        );
        await loadRetention();
        loginGridApi.query();
        operationGridApi.query();
      } catch (error: any) {
        message.error(error?.message || '清理失败，请稍后重试');
      } finally {
        cleanupLoading.value = false;
      }
    },
  });
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString('zh-CN', {
    hour12: false,
  });
}

onMounted(() => {
  loadRetention();
});
</script>

<template>
  <Page auto-content-height title="审计日志">
    <div class="flex size-full flex-col gap-4 p-4" data-testid="audit-log-page">
      <div class="flex items-center justify-between gap-3">
        <div>
          <div class="flex items-center gap-2 text-lg font-medium">
            <IconifyIcon class="size-5 text-primary" icon="lucide:shield-check" />
            审计日志
          </div>
          <div class="text-muted-foreground mt-1 text-sm">
            集中查看登录与操作审计记录，并维护过期日志清理闭环。
          </div>
        </div>
        <div class="flex flex-wrap justify-end gap-2">
          <Button :loading="loadingRetention" @click="onRefresh">
            <template #icon>
              <IconifyIcon icon="lucide:refresh-cw" />
            </template>
            刷新
          </Button>
          <Button
            danger
            :disabled="totalExpiredCount === 0"
            :loading="cleanupLoading"
            v-access:code="['system:audit:retention']"
            @click="onCleanupExpiredLogs"
          >
            <template #icon>
              <IconifyIcon icon="lucide:archive-x" />
            </template>
            清理过期日志
          </Button>
        </div>
      </div>

      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <Card
          v-for="item in metrics"
          :key="item.title"
          :loading="loadingRetention"
          variant="borderless"
        >
          <div class="flex items-start justify-between gap-3">
            <Statistic :title="item.title" :value="item.value" :suffix="item.suffix" />
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon :icon="item.icon" class="size-5" />
            </div>
          </div>
        </Card>
      </div>

      <Card variant="borderless">
        <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <div class="rounded border border-border p-3">
            <div class="text-muted-foreground text-xs">登录日志过期阈值</div>
            <div class="mt-1 text-sm font-medium">
              {{ formatDateTime(retention?.loginLogExpireBefore) }}
            </div>
          </div>
          <div class="rounded border border-border p-3">
            <div class="text-muted-foreground text-xs">操作日志过期阈值</div>
            <div class="mt-1 text-sm font-medium">
              {{ formatDateTime(retention?.operationLogExpireBefore) }}
            </div>
          </div>
          <div class="rounded border border-border p-3">
            <div class="text-muted-foreground text-xs">清理状态</div>
            <div class="mt-1">
              <Tag :color="totalExpiredCount > 0 ? 'warning' : 'success'">
                {{ totalExpiredCount > 0 ? '存在过期日志' : '无需清理' }}
              </Tag>
            </div>
          </div>
          <div class="rounded border border-border p-3">
            <div class="text-muted-foreground text-xs">防重复提交</div>
            <div class="mt-1">
              <Tag color="processing">30 秒</Tag>
            </div>
          </div>
        </div>
      </Card>

      <Tabs v-model:activeKey="activeTab" class="audit-log-tabs min-h-0 flex-1">
        <TabPane key="login" tab="登录日志">
          <LoginGrid table-title="登录日志">
            <template #toolbar-tools>
              <Button v-access:code="['system:audit:login-log']" @click="onExportLoginLogs">
                <IconifyIcon icon="lucide:download" class="size-4" />
                导出 CSV
              </Button>
            </template>
          </LoginGrid>
        </TabPane>
        <TabPane key="operation" tab="操作日志">
          <OperationGrid table-title="操作日志">
            <template #toolbar-tools>
              <Button
                v-access:code="['system:audit:operation-log']"
                @click="onExportOperationLogs"
              >
                <IconifyIcon icon="lucide:download" class="size-4" />
                导出 CSV
              </Button>
            </template>
          </OperationGrid>
        </TabPane>
      </Tabs>
    </div>
  </Page>
</template>

<style scoped>
.audit-log-tabs {
  display: flex;
  flex-direction: column;
}

.audit-log-tabs :deep(.ant-tabs-content-holder),
.audit-log-tabs :deep(.ant-tabs-content),
.audit-log-tabs :deep(.ant-tabs-tabpane) {
  min-height: 0;
  flex: 1;
}

.audit-log-tabs :deep(.ant-tabs-content) {
  height: 100%;
}
</style>
