<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrEmployeeApi } from '#/api';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, Statistic } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { exportExpiringEmployeeDocuments, getExpiringEmployeeDocuments } from '#/api';
import { $t } from '#/locales';

import {
  getHrDictOptions,
  HR_EMPLOYEE_DOCUMENT_TYPE_DICT,
} from '../dict-options';
import { downloadHrBlobWithFeedback, openHrFileWithFeedback } from '../shared/file';
import { isDueWithinDays } from '../shared/warning';
import { useColumns, useGridFormSchema } from './data';

defineOptions({ name: 'HrDocumentWarning' });

const rows = ref<HrEmployeeApi.EmployeeDocument[]>([]);
const currentWindowDays = ref(30);
const exportLoading = ref(false);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    autoResize: true,
    columns: useColumns(),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async (_, formValues) => {
          const days = Number(formValues?.days ?? 30);
          currentWindowDays.value = days;
          const result = await getExpiringEmployeeDocuments(days);
          rows.value = result;
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
  } as VxeTableGridOptions<HrEmployeeApi.EmployeeDocument>,
});

const metrics = computed(() => [
  {
    icon: 'lucide:files',
    title: $t('hr.documentWarning.metricTotal'),
    value: rows.value.length,
  },
  {
    icon: 'lucide:badge-alert',
    title: $t('hr.documentWarning.metricSevenDays'),
    value: rows.value.filter((item) => isDueWithinDays(item.expireDate, 7)).length,
  },
  {
    icon: 'lucide:calendar-clock',
    title: $t('hr.documentWarning.metricWindow'),
    value: currentWindowDays.value,
  },
]);

function onRefresh() {
  gridApi.query();
}

async function onViewDocument(row: HrEmployeeApi.EmployeeDocument) {
  await openHrFileWithFeedback(row, {
    errorMessage: $t('hr.documentWarning.fileOpenError'),
    loadingMessage: $t('hr.documentWarning.fileOpening'),
    unavailableMessage: $t('hr.documentWarning.fileOpenUnavailable'),
  });
}

async function onExportDocuments() {
  exportLoading.value = true;
  try {
    await downloadHrBlobWithFeedback(
      {
        errorMessage: $t('hr.documentWarning.exportError'),
        fileName: 'ones-hr-expiring-documents.csv',
        loadingMessage: $t('hr.documentWarning.exporting'),
        successMessage: $t('hr.documentWarning.exportSuccess'),
      },
      () => exportExpiringEmployeeDocuments(currentWindowDays.value),
    );
  } finally {
    exportLoading.value = false;
  }
}

getHrDictOptions(HR_EMPLOYEE_DOCUMENT_TYPE_DICT);
</script>

<template>
  <Page auto-content-height :title="$t('hr.documentWarning.title')">
    <div class="flex size-full flex-col gap-4 p-4">
      <div class="flex items-center justify-between gap-3">
        <div>
          <div class="flex items-center gap-2 text-lg font-medium">
            <IconifyIcon class="size-5 text-primary" icon="lucide:file-warning" />
            {{ $t('hr.documentWarning.title') }}
          </div>
          <div class="text-muted-foreground mt-1 text-sm">
            {{ $t('hr.documentWarning.description') }}
          </div>
        </div>
        <div class="flex flex-wrap justify-end gap-2">
          <Button @click="onRefresh">
            <template #icon>
              <IconifyIcon icon="lucide:refresh-cw" />
            </template>
            {{ $t('common.refresh') }}
          </Button>
          <Button :loading="exportLoading" @click="onExportDocuments">
            <template #icon>
              <IconifyIcon icon="lucide:download" />
            </template>
            {{ $t('hr.documentWarning.exportCsv') }}
          </Button>
        </div>
      </div>

      <div class="grid gap-4 md:grid-cols-3">
        <Card v-for="item in metrics" :key="item.title" variant="borderless">
          <div class="flex items-start justify-between gap-3">
            <Statistic :title="item.title" :value="item.value" />
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon :icon="item.icon" class="size-5" />
            </div>
          </div>
        </Card>
      </div>

      <div class="min-h-0 flex-1">
        <Grid :table-title="$t('hr.documentWarning.list')">
          <template #action="{ row }">
            <VbenTableAction
              :actions="[
                {
                  text: $t('hr.documentWarning.viewFile'),
                  icon: 'lucide:external-link',
                  disabled: !row.storedName,
                  onClick: () => onViewDocument(row),
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
