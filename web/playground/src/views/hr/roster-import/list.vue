<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrRosterImportApi } from '#/api';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Card, message, Upload } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { downloadRosterTemplate, getRosterImportBatches, uploadRosterFile } from '#/api';
import { $t } from '#/locales';

import { useColumns } from './data';
import ErrorsModal from './modules/errors.vue';

const [Errors, errorsModalApi] = useVbenModal({
  connectedComponent: ErrorsModal,
});

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
          return await getRosterImportBatches({
            page: page.currentPage,
            pageSize: page.pageSize,
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
      search: false,
      zoom: true,
    },
  } as VxeTableGridOptions<HrRosterImportApi.ImportBatch>,
});

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
  const hide = message.loading('正在上传并解析花名册...', 0);
  try {
    await uploadRosterFile(file);
    message.success('花名册导入请求提交成功，正在后台解析');
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
</script>
<template>
  <Page auto-content-height>
    <Errors @success="onRefresh" />

    <div class="flex flex-col size-full gap-4">
      <!-- 拖拽上传 Card -->
      <Card title="导入花名册 (CSV)" class="w-full">
        <div class="flex gap-4 items-center">
          <div class="flex-1 max-w-md">
            <Upload.Dragger
              name="file"
              :multiple="false"
              :show-upload-list="false"
              :custom-request="handleCustomUpload"
              accept=".csv"
            >
              <p class="ant-upload-drag-icon flex justify-center mt-4">
                <IconifyIcon icon="lucide:upload" class="size-10 text-primary" style="font-size: 32px;" />
              </p>
              <p class="ant-upload-text px-4 pb-4">
                点击或拖拽 CSV 文件到此区域进行花名册上传导入
              </p>
            </Upload.Dragger>
          </div>
          <div class="flex flex-col gap-2">
            <h4 class="font-bold text-gray-700">导入指南:</h4>
            <ul class="text-xs text-gray-500 list-disc list-inside space-y-1">
              <li>上传文件格式必须为 CSV 格式，且编码为 UTF-8。</li>
              <li>请优先下载标准导入模板，参照模板字段填写。</li>
              <li>部门、岗位和职级字段需填写对应的系统 ID 标识。</li>
              <li>上传后系统将在后台异步解析，请在下方列表刷新查看结果。</li>
            </ul>
            <Button type="primary" class="w-fit mt-2" @click="handleDownloadTemplate">
              <IconifyIcon icon="lucide:download" class="mr-2" />
              {{ $t('hr.rosterImport.downloadTemplate') }}
            </Button>
          </div>
        </div>
      </Card>

      <!-- 历史批次列表 -->
      <div class="flex-1 min-h-[300px]">
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
