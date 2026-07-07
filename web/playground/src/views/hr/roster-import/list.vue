<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrRosterImportApi } from '#/api';

import { computed, onMounted, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, message, Statistic, Tag, Upload } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { downloadRosterTemplate, getRosterImportBatches, uploadRosterFile } from '#/api';
import { $t } from '#/locales';

import { useColumns } from './data';
import {
  getHrDictOption,
  getHrDictOptions,
  HR_ROSTER_IMPORT_STATUS_DICT,
} from '../dict-options';
import { errorMessageOf } from '../shared/error';
import { downloadHrBlobWithFeedback } from '../shared/file';
import { runHrUploadWithFeedback } from '../shared/upload';
import ErrorsModal from './modules/errors.vue';

const [Errors, errorsModalApi] = useVbenModal({
  connectedComponent: ErrorsModal,
});

const latestBatches = ref<HrRosterImportApi.ImportBatch[]>([]);
const totalBatchCount = ref(0);

type UploadRequestOptions = {
  file: Blob | File | string;
  onError?: (error: Error) => void;
  onSuccess?: (data?: unknown) => void;
};

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
      title: $t('hr.rosterImport.metricBatchCount'),
      value: totalBatchCount.value,
    },
    {
      icon: 'lucide:circle-check',
      title: $t('hr.rosterImport.metricCurrentPageSuccess'),
      value: currentPageBatches.reduce((total, item) => total + item.successCount, 0),
    },
    {
      icon: 'lucide:triangle-alert',
      title: $t('hr.rosterImport.metricCurrentPageFailed'),
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
  await downloadHrBlobWithFeedback(
    {
      errorMessage: $t('hr.rosterImport.templateDownloadError'),
      fileName: 'ones-hr-roster-template.csv',
      loadingMessage: $t('hr.rosterImport.templateDownloading'),
      successMessage: $t('hr.rosterImport.templateDownloadSuccess'),
    },
    downloadRosterTemplate,
  );
}

async function handleCustomUpload(options: UploadRequestOptions) {
  const { file, onSuccess, onError } = options;
  const uploadFile = file as File;
  if (!uploadFile.name.toLowerCase().endsWith('.csv')) {
    const error = new Error($t('hr.rosterImport.csvOnlyError'));
    message.error(errorMessageOf(error, $t('hr.rosterImport.csvOnlyError')));
    onError?.(error);
    return;
  }
  if (uploadFile.size > 2 * 1024 * 1024) {
    const error = new Error($t('hr.rosterImport.csvSizeLimitError'));
    message.error(errorMessageOf(error, $t('hr.rosterImport.csvSizeLimitError')));
    onError?.(error);
    return;
  }
  const result = await runHrUploadWithFeedback(
    {
      errorMessage: $t('hr.rosterImport.uploadError'),
      loadingMessage: $t('hr.rosterImport.uploading'),
      onError,
      onSuccess,
      successMessage: (batch) =>
        $t('hr.rosterImport.uploadSuccess', {
          failed: batch.failedCount,
          success: batch.successCount,
        }),
    },
    () => uploadRosterFile(uploadFile),
  );
  if (result.success) {
    onRefresh();
  }
}

function handleViewErrors(row: HrRosterImportApi.ImportBatch) {
  errorsModalApi.setData({ id: row.id }).open();
}

function statusTag(status?: string) {
  if (!status) {
    return { color: 'default', text: '-' };
  }
  const option = getHrDictOption(HR_ROSTER_IMPORT_STATUS_DICT, status);
  return {
    color: option?.color ?? 'default',
    text: option?.label ?? status,
  };
}

onMounted(async () => {
  await getHrDictOptions(HR_ROSTER_IMPORT_STATUS_DICT);
});
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
              :title="$t('hr.rosterImport.latestBatch')"
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

      <Card :title="$t('hr.rosterImport.importCsvTitle')" class="w-full" variant="borderless">
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
                {{ $t('hr.rosterImport.uploadDraggerText') }}
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
