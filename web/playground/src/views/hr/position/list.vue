<script lang="ts" setup>
import type { OnActionClickParams, VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrPositionApi } from '#/api';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, message, Modal } from 'antdv-next';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getPositionList, updatePosition } from '#/api';
import { $t } from '#/locales';

import { useColumns } from './data';
import Form from './modules/form.vue';

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

function onActionClick(e: OnActionClickParams<HrPositionApi.HrPosition>) {
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
          const list = await getPositionList();
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
  } as VxeTableGridOptions<HrPositionApi.HrPosition>,
});

function onRefresh() {
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

function onEdit(row: HrPositionApi.HrPosition) {
  formDrawerApi.setData(row).open();
}

function confirm(content: string, title: string) {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      content,
      onCancel() {
        reject(new Error('已取消'));
      },
      onOk() {
        resolve(true);
      },
      title,
    });
  });
}

async function onStatusChange(
  newStatus: boolean,
  row: HrPositionApi.HrPosition,
) {
  const statusStr = newStatus ? '启用' : '禁用';
  try {
    await confirm(
      `您确定要将岗位 ${row.positionName} 切换为 【${statusStr}】 吗？`,
      '切换状态',
    );
    await updatePosition(row.id, {
      positionCode: row.positionCode,
      positionName: row.positionName,
      deptId: row.deptId,
      description: row.description,
      enabled: newStatus,
    });
    message.success(`已切换为 ${statusStr}`);
    return true;
  } catch {
    return false;
  }
}
</script>
<template>
  <Page auto-content-height>
    <FormDrawer @success="onRefresh" />
    <Grid :table-title="$t('hr.position.list')">
      <template #toolbar-tools>
        <Button type="primary" @click="onCreate">
          <Plus class="size-5" />
          {{ $t('hr.position.create') }}
        </Button>
      </template>
    </Grid>
  </Page>
</template>
