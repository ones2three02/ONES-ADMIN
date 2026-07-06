<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrEmployeeApi } from '#/api';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, message, Statistic } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { getExpiringEmployeeDocuments } from '#/api';
import { $t } from '#/locales';

import { daysUntil, useColumns, useGridFormSchema } from './data';
import {
  getHrDictOptions,
  HR_EMPLOYEE_DOCUMENT_TYPE_DICT,
} from '../dict-options';

defineOptions({ name: 'HrDocumentWarning' });

const rows = ref<HrEmployeeApi.EmployeeDocument[]>([]);
const currentWindowDays = ref(30);

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
    value: rows.value.filter((item) => {
      const remainDays = daysUntil(item.expireDate);
      return remainDays !== undefined && remainDays >= 0 && remainDays <= 7;
    }).length,
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

function onViewDocument(row: HrEmployeeApi.EmployeeDocument) {
  if (!row.url) {
    message.warning($t('hr.documentWarning.fileOpenUnavailable'));
    return;
  }
  try {
    window.open(row.url, '_blank', 'noopener,noreferrer');
  } catch (error) {
    message.error(errorMessageOf(error, $t('hr.documentWarning.fileOpenError')));
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
        <Button @click="onRefresh">
          <template #icon>
            <IconifyIcon icon="lucide:refresh-cw" />
          </template>
          {{ $t('common.refresh') }}
        </Button>
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
                  disabled: !row.url,
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
