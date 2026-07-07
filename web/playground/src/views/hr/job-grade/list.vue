<script lang="ts" setup>
import type { OnActionClickParams, VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrJobGradeApi } from '#/api';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message } from 'antdv-next';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getJobGradeList, updateJobGrade } from '#/api';
import { $t } from '#/locales';

import { errorMessageOf } from '../shared/error';
import {
  confirmHrStatusChange,
  isHrStatusChangeCancelled,
} from '../shared/status-change';
import { useColumns } from './data';
import Form from './modules/form.vue';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

function onActionClick(e: OnActionClickParams<HrJobGradeApi.HrJobGrade>) {
  if (e.code === 'edit') {
    onEdit(e.row);
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useColumns(onActionClick, onStatusChange),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async () => {
          const list = await getJobGradeList();
          return list;
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
  } as VxeTableGridOptions<HrJobGradeApi.HrJobGrade>,
});

function onRefresh() {
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

function onEdit(row: HrJobGradeApi.HrJobGrade) {
  formDrawerApi.setData(row).open();
}

async function onStatusChange(
  newStatus: boolean,
  row: HrJobGradeApi.HrJobGrade,
) {
  const statusStr = newStatus
    ? $t('hr.statusChange.enabledAction')
    : $t('hr.statusChange.disabledAction');
  try {
    await confirmHrStatusChange(
      $t('hr.statusChange.jobGradeConfirm', {
        name: row.gradeName,
        status: statusStr,
      }),
      $t('hr.statusChange.title'),
    );
    await updateJobGrade(row.id, {
      gradeCode: row.gradeCode,
      gradeName: row.gradeName,
      gradeRank: row.gradeRank,
      enabled: newStatus,
    });
    message.success($t('hr.statusChange.success', { status: statusStr }));
    return true;
  } catch (error) {
    if (isHrStatusChangeCancelled(error)) {
      return false;
    }
    message.error(errorMessageOf(error, $t('hr.statusChange.error')));
    return false;
  }
}
</script>
<template>
  <Page auto-content-height>
    <FormDrawer @success="onRefresh" />
    <Grid :table-title="$t('hr.jobGrade.list')">
      <template #toolbar-tools>
        <Button type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('hr.jobGrade.create') }}
        </Button>
      </template>
    </Grid>
  </Page>
</template>
