<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrRosterImportApi } from '#/api';

import { computed, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, message, Statistic, Tag, Upload } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { downloadRosterTemplate, getRosterImportBatches, uploadRosterFile } from '#/api';
import { $t } from '#/locales';

import { useColumns } from './data';
import ErrorsModal from './modules/errors.vue';

const [Errors, errorsModalApi] = useVbenModal({
  connectedComponent: ErrorsModal,
});

const latestBatches = ref<HrRosterImportApi.ImportBatch[]>([]);
const totalBatchCount = ref(0);

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      ...(useColumns() as any[]),
      {
        align: 'center',
        field: 'action',
        fixed: 'right',
        title: $t('common.action'),
        width: 120,
        slots: { default: 'action' },
      },
    ],
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const result = await getRosterImportBatches({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
          });
          latestBatches.value = result.items;
          totalBatchCount.value = result.total;
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
      search: false,
      zoom: true,
    },
  } as VxeTableGridOptions<HrRosterImportApi.ImportBatch>,
});

const metrics = computed(() => {
  const currentPageBatches = latestBatches.value;
  return [
    {
      icon: 'lucide:files',
      title: '导入批次',
      value: totalBatchCount.value,
    },
    {
      icon: 'lucide:circle-check',
      title: '当前页成功',
      value: currentPageBatches.reduce((total, item) => total + item.successCount, 0),
    },
    {
      icon: 'lucide:triangle-alert',
      title: '当前页失败',
      value: currentPageBatches.reduce((total, item) => total + item.failedCount, 0),
    },
  ];
});

const latestBatch = computed(() => latestBatches.value[0]);
const latestBatchStatus = computed(() => statusTag(latestBatch.value?.status));

function onRefresh() {
  gridApi.query();
}

async function handleDownloadTemplate() {
  const hide = message.loading('正在下载模板...', 0);
  try {
    const blob = await downloadRosterTemplate();
    const url = window.URL.createObjectURL(new Blob([blob]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'ones-hr-roster-template.csv');
    document.body.appendChild(link);
    link.click();
    link.parentNode?.removeChild(link);
    message.success('下载成功');
  } catch (error) {
    message.error('下载模板失败');
  } finally {
    hide();
  }
}

async function handleCustomUpload(options: any) {
  const { file, onSuccess, onError } = options;
  const uploadFile = file as File;
  if (!uploadFile.name.toLowerCase().endsWith('.csv')) {
    message.error('仅支持 CSV 文件');
    onError?.(new Error('仅支持 CSV 文件'));
    return;
  }
  if (uploadFile.size > 2 * 1024 * 1024) {
    message.error('CSV 文件不能超过 2MB');
    onError?.(new Error('CSV 文件不能超过 2MB'));
    return;
  }
  const hide = message.loading('正在上传并解析花名册...', 0);
  try {
    const batch = await uploadRosterFile(uploadFile);
    message.success(`导入完成：成功 ${batch.successCount} 行，失败 ${batch.failedCount} 行`);
    onSuccess?.();
    onRefresh();
  } catch (error: any) {
    message.error(error.message || '导入失败，请检查文件格式');
    onError?.(error);
  } finally {
    hide();
  }
}

function handleViewErrors(row: HrRosterImportApi.ImportBatch) {
  errorsModalApi.setData({ id: row.id }).open();
}

function statusTag(status?: string) {
  const statusMap: Record<string, { color: string; text: string }> = {
    PARSING: { color: 'processing', text: '解析中' },
    PARTIAL_SUCCESS: { color: 'warning', text: '部分成功' },
    SUCCESS: { color: 'success', text: '导入成功' },
    VALIDATION_FAILED: { color: 'error', text: '校验失败' },
  };
  return status ? (statusMap[status] ?? { color: 'default', text: status }) : { color: 'default', text: '-' };
}
</script>
<template>
  <Page auto-content-height :title="$t('hr.rosterImport.title')">
    <Errors @success="onRefresh" />

    <div class="flex size-full flex-col gap-4">
      <div class="grid gap-4 lg:grid-cols-4">
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

        <Card variant="borderless">
          <div class="flex items-start justify-between gap-3">
            <Statistic
              title="最近批次"
              :value="latestBatch?.batchNo || '-'"
              :value-style="{ fontSize: '14px' }"
            />
            <Tag :color="latestBatchStatus.color">{{ latestBatchStatus.text }}</Tag>
          </div>
          <div class="text-muted-foreground mt-3 text-xs">
            {{ latestBatch?.completedAt || latestBatch?.createdAt || '-' }}
          </div>
        </Card>
      </div>

      <Card title="导入花名册 (CSV)" class="w-full" variant="borderless">
        <div class="flex flex-col gap-4 xl:flex-row xl:items-center">
          <div class="max-w-xl flex-1">
            <Upload.Dragger
              name="file"
              :multiple="false"
              :show-upload-list="false"
              :custom-request="handleCustomUpload"
              accept=".csv"
            >
              <p class="ant-upload-drag-icon mt-4 flex justify-center">
                <IconifyIcon icon="lucide:upload" class="size-10 text-primary" />
              </p>
              <p class="ant-upload-text px-4 pb-4">
                点击或拖拽 CSV 文件到此区域进行花名册上传导入
              </p>
            </Upload.Dragger>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button type="primary" @click="handleDownloadTemplate">
              <IconifyIcon icon="lucide:download" class="mr-2" />
              {{ $t('hr.rosterImport.downloadTemplate') }}
            </Button>
            <Button @click="onRefresh">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-2" />
              {{ $t('common.refresh') }}
            </Button>
          </div>
        </div>
      </Card>

      <div class="min-h-[300px] flex-1">
        <Grid :table-title="$t('hr.rosterImport.list')">
          <template #action="{ row }">
            <VbenTableAction
              :actions="[
                {
                  text: $t('hr.rosterImport.errors'),
                  icon: 'lucide:file-x-2',
                  danger: true,
                  disabled: row.failedCount === 0,
                  onClick: () => handleViewErrors(row),
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
