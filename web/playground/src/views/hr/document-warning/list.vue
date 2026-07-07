<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrEmployeeApi } from '#/api';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlob } from '@vben/utils';

import { Button, Card, message, Statistic } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { exportExpiringEmployeeDocuments, getExpiringEmployeeDocuments } from '#/api';
import { openSystemFile } from '#/api/system/file';
import { $t } from '#/locales';

import {
  getHrDictOptions,
  HR_EMPLOYEE_DOCUMENT_TYPE_DICT,
} from '../dict-options';
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
    columns: useColumns(),
    height: 'auto',
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

function errorMessageOf(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

async function onViewDocument(row: HrEmployeeApi.EmployeeDocument) {
  if (!row.storedName) {
    message.warning($t('hr.documentWarning.fileOpenUnavailable'));
    return;
  }
  try {
    await openSystemFile(row);
  } catch (error) {
    message.error(errorMessageOf(error, $t('hr.documentWarning.fileOpenError')));
  }
}

async function onExportDocuments() {
  exportLoading.value = true;
  const hide = message.loading($t('hr.documentWarning.exporting'), 0);
  try {
    const blob = await exportExpiringEmployeeDocuments(currentWindowDays.value);
    downloadFileFromBlob({
      fileName: 'ones-hr-expiring-documents.csv',
      source: blob,
    });
    message.success($t('hr.documentWarning.exportSuccess'));
  } catch (error) {
    message.error(errorMessageOf(error, $t('hr.documentWarning.exportError')));
  } finally {
    exportLoading.value = false;
    hide();
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

      <div class="min-h-[420px] flex-1">
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
