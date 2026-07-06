<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SystemFileApi } from '#/api';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, message, Modal, Statistic, Upload } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  deleteSystemFile,
  getFileRetention,
  getFileMetadataList,
  purgeExpiredDeletedFiles,
  uploadSystemFile,
} from '#/api';

import { useColumns, useGridFormSchema } from './data';

defineOptions({ name: 'SystemFile' });

const rows = ref<SystemFileApi.FileMetadata[]>([]);
const retention = ref<SystemFileApi.FileRetentionSummary>();
const purgeLoading = ref(false);
const uploadLoading = ref(false);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    fieldMappingTime: [['createdAt', ['startTime', 'endTime'], 'YYYY-MM-DDTHH:mm:ss']],
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
          const result = await getFileMetadataList({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          rows.value = result.items;
          return result;
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
  } as VxeTableGridOptions<SystemFileApi.FileMetadata>,
});

const metrics = computed(() => [
  {
    icon: 'lucide:files',
    title: '当前页文件',
    value: rows.value.length,
  },
  {
    icon: 'lucide:circle-check',
    title: '可用文件',
    value: rows.value.filter((item) => item.status === 'ACTIVE').length,
  },
  {
    icon: 'lucide:link-2',
    title: '业务引用',
    value: rows.value.filter((item) => item.businessType || item.businessId).length,
  },
  {
    icon: 'lucide:archive',
    title: '已删除',
    value: rows.value.filter((item) => item.status === 'DELETED').length,
  },
  {
    icon: 'lucide:archive-x',
    title: '待清理',
    value: retention.value?.expiredDeletedFileCount ?? 0,
  },
]);

function onRefresh() {
  gridApi.query();
  refreshRetention();
}

async function refreshRetention() {
  try {
    retention.value = await getFileRetention();
  } catch {
    retention.value = undefined;
  }
}

async function handleCustomUpload(options: any) {
  const { file, onError, onSuccess } = options;
  const uploadFile = file as File;
  uploadLoading.value = true;
  const hide = message.loading('正在上传文件...', 0);
  try {
    await uploadSystemFile(uploadFile);
    message.success('上传成功');
    onSuccess?.();
    onRefresh();
  } catch (error: any) {
    message.error(error?.message || '上传失败，请稍后重试');
    onError?.(error);
  } finally {
    hide();
    uploadLoading.value = false;
  }
}

function onDelete(row: SystemFileApi.FileMetadata) {
  Modal.confirm({
    content: `确认删除文件「${row.originalName}」吗？已被业务引用的文件将由后端拒绝删除。`,
    okButtonProps: { danger: true },
    okText: '确认删除',
    title: '删除文件',
    async onOk() {
      await deleteSystemFile(row.id);
      message.success('删除成功');
      onRefresh();
    },
  });
}

function formatSize(size?: number) {
  if (size === undefined || size === null) {
    return '0 B';
  }
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function onPurgeExpiredFiles() {
  const count = retention.value?.expiredDeletedFileCount ?? 0;
  Modal.confirm({
    content:
      count > 0
        ? `确认物理清理 ${count} 个过期已删除文件吗？清理后仅保留元数据审计记录。`
        : '当前没有达到保留期的已删除文件。',
    okButtonProps: { danger: count > 0 },
    okText: count > 0 ? '确认清理' : '知道了',
    title: '清理过期文件',
    async onOk() {
      if (count <= 0) {
        return;
      }
      purgeLoading.value = true;
      try {
        const result = await purgeExpiredDeletedFiles();
        message.success(
          `已清理 ${result.purgedFileCount} 个文件，释放 ${formatSize(result.purgedFileSizeBytes)}`,
        );
        onRefresh();
      } finally {
        purgeLoading.value = false;
      }
    },
  });
}

refreshRetention();
</script>

<template>
  <Page auto-content-height title="文件管理">
    <div class="flex size-full flex-col gap-4 p-4" data-testid="system-file-page">
      <div class="flex items-center justify-between gap-3">
        <div>
          <div class="flex items-center gap-2 text-lg font-medium">
            <IconifyIcon class="size-5 text-primary" icon="lucide:folder-open" />
            文件管理
          </div>
          <div class="text-muted-foreground mt-1 text-sm">
            管理系统上传文件的元数据、存储状态和业务引用关系。
          </div>
        </div>
        <div class="flex flex-wrap justify-end gap-2">
          <Button @click="onRefresh">
            <template #icon>
              <IconifyIcon icon="lucide:refresh-cw" />
            </template>
            刷新
          </Button>
          <Button
            :loading="purgeLoading"
            danger
            v-access:code="['system:file:purge']"
            @click="onPurgeExpiredFiles"
          >
            <template #icon>
              <IconifyIcon icon="lucide:archive-x" />
            </template>
            清理过期
          </Button>
          <Upload
            :custom-request="handleCustomUpload"
            :show-upload-list="false"
            v-access:code="['system:file:upload']"
          >
            <Button type="primary" :loading="uploadLoading">
              <template #icon>
                <IconifyIcon icon="lucide:upload" />
              </template>
              上传文件
            </Button>
          </Upload>
        </div>
      </div>

      <div class="grid gap-4 md:grid-cols-5">
        <Card v-for="item in metrics" :key="item.title" variant="borderless">
          <div class="flex items-start justify-between gap-3">
            <Statistic :title="item.title" :value="item.value" />
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon :icon="item.icon" class="size-5" />
            </div>
          </div>
        </Card>
      </div>

      <div class="min-h-[420px] flex-1">
        <Grid table-title="文件资产">
          <template #action="{ row }">
            <VbenTableAction
              :actions="[
                {
                  text: '删除',
                  icon: 'lucide:trash-2',
                  danger: true,
                  auth: ['system:file:delete'],
                  disabled: row.status !== 'ACTIVE',
                  onClick: () => onDelete(row),
                },
              ]"
              align="center"
            />
          </template>
        </Grid>
      </div>
    </div>
  </Page>
</template>
