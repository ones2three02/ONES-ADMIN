<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrContractApi, HrEmployeeApi } from '#/api';

import { onMounted, ref, watch } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';
import { downloadFileFromBlob } from '@vben/utils';

import { Button, Card, InputSearch, message, TabPane, Tabs } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  exportExpiringContracts,
  getContractAttachmentMetadata,
  getEmployeeContracts,
  getEmployeeList,
  getExpiringContracts,
} from '#/api';
import { openSystemFile } from '#/api/system/file';
import { $t } from '#/locales';

import { useColumns, useExpiringColumns } from './data';
import {
  getHrDictOptions,
  HR_CONTRACT_STATUS_DICT,
  HR_CONTRACT_TYPE_DICT,
} from '../dict-options';
import Form from './modules/form.vue';
import TerminateForm from './modules/terminate-form.vue';

const activeTab = ref('historical');
const employees = ref<HrEmployeeApi.HrEmployee[]>([]);
const searchEmployeeValue = ref('');
const selectedEmployeeId = ref<string>('');
const selectedEmployee = ref<HrEmployeeApi.HrEmployee | null>(null);
const expiringWindowDays = 30;
const expiringExportLoading = ref(false);

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [TerminateDrawer, terminateDrawerApi] = useVbenDrawer({
  connectedComponent: TerminateForm,
  destroyOnClose: true,
});

// 合同历史表格 Grid
const [HistoryGrid, historyGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      ...(useColumns() as any[]),
      {
        align: 'center',
        field: 'action',
        fixed: 'right',
        title: $t('common.action'),
        width: 160,
        slots: { default: 'action' },
      },
    ],
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async () => {
          if (!selectedEmployeeId.value) return [];
          return await getEmployeeContracts(selectedEmployeeId.value);
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
  } as VxeTableGridOptions<HrContractApi.HrContract>,
});

// 即将到期合同表格 Grid
const [ExpiringGrid, expiringGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useExpiringColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async () => {
          return await getExpiringContracts(30); // 预警30天内到期的合同
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
  } as VxeTableGridOptions<HrContractApi.HrContract>,
});

async function loadEmployeeList() {
  try {
    const res = await getEmployeeList({
      pageNum: 1,
      pageSize: 50,
      keyword: searchEmployeeValue.value || undefined,
    });
    employees.value = res.items;
    if (res.items.length > 0 && !selectedEmployeeId.value) {
      selectEmployee(res.items[0]);
    }
  } catch (error) {
    console.error('Failed to load employee list:', error);
  }
}

function selectEmployee(emp: HrEmployeeApi.HrEmployee) {
  selectedEmployeeId.value = emp.id;
  selectedEmployee.value = emp;
  historyGridApi.query();
}

function onRefresh() {
  if (activeTab.value === 'historical') {
    historyGridApi.query();
  } else {
    expiringGridApi.query();
  }
}

function onSignContract() {
  if (!selectedEmployeeId.value) return;
  formDrawerApi.setData({ employeeId: selectedEmployeeId.value }).open();
}

function onEditContract(row: HrContractApi.HrContract) {
  formDrawerApi.setData({
    employeeId: selectedEmployeeId.value,
    contract: row,
  }).open();
}

function onTerminateContract(row: HrContractApi.HrContract) {
  terminateDrawerApi.setData(row).open();
}

function errorMessageOf(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

async function onViewAttachment(row: HrContractApi.HrContract) {
  if (!row.attachmentFileId) {
    message.warning($t('hr.contract.noAttachment'));
    return;
  }
  try {
    const metadata = await getContractAttachmentMetadata(row.id);
    if (!metadata.storedName) {
      message.warning($t('hr.contract.attachmentOpenUnavailable'));
      return;
    }
    await openSystemFile(metadata);
  } catch (error: unknown) {
    message.error(errorMessageOf(error, $t('hr.contract.attachmentLoadError')));
  }
}

async function onExportExpiringContracts() {
  expiringExportLoading.value = true;
  const hide = message.loading($t('hr.contract.expiringExporting'), 0);
  try {
    const blob = await exportExpiringContracts(expiringWindowDays);
    downloadFileFromBlob({
      fileName: 'ones-hr-expiring-contracts.csv',
      source: blob,
    });
    message.success($t('hr.contract.expiringExportSuccess'));
  } catch (error: unknown) {
    message.error(errorMessageOf(error, $t('hr.contract.expiringExportError')));
  } finally {
    expiringExportLoading.value = false;
    hide();
  }
}

onMounted(async () => {
  await Promise.all([
    getHrDictOptions(HR_CONTRACT_TYPE_DICT),
    getHrDictOptions(HR_CONTRACT_STATUS_DICT),
  ]);
  loadEmployeeList();
  expiringGridApi.query();
});

watch(searchEmployeeValue, () => {
  loadEmployeeList();
});
</script>
<template>
  <Page auto-content-height>
    <FormDrawer @success="onRefresh" />
    <TerminateDrawer @success="onRefresh" />

    <Tabs v-model:activeKey="activeTab" class="size-full">
      <TabPane key="historical" :tab="$t('hr.contract.list')">
        <div class="flex size-full">
          <!-- ... -->
          <Card class="w-1/4" :title="$t('hr.employee.title')">
            <InputSearch
              v-model:value="searchEmployeeValue"
              placeholder="搜索姓名..."
              class="mb-4"
            />
            <div class="overflow-y-auto max-h-[500px]">
              <div
                v-for="emp in employees"
                :key="emp.id"
                :class="[
                  'p-3 mb-2 rounded cursor-pointer transition-all border',
                  selectedEmployeeId === emp.id
                    ? 'border-primary bg-primary/10 font-bold'
                    : 'border-transparent hover:bg-gray-100',
                ]"
                @click="selectEmployee(emp)"
              >
                <div class="flex justify-between items-center">
                  <span>{{ emp.realName }}</span>
                  <span class="text-xs text-gray-500">工号: {{ emp.employeeNo }}</span>
                </div>
                <div class="text-xs text-gray-400 mt-1">
                  {{ emp.deptName }} | {{ emp.positionName || '无岗位' }}
                </div>
              </div>
            </div>
          </Card>

          <!-- 右侧选定员工的合同历史 -->
          <div class="w-3/4 ml-4">
            <HistoryGrid :table-title="selectedEmployee ? `【${selectedEmployee.realName}】的合同历史` : '合同历史'">
              <template #toolbar-tools>
                <Button type="primary" :disabled="!selectedEmployeeId" @click="onSignContract">
                  <Plus class="size-5" />
                  {{ $t('hr.contract.create') }}
                </Button>
              </template>
              <template #attachment="{ row }">
                <Button
                  v-if="row.attachmentFileId"
                  size="small"
                  type="link"
                  v-access:code="['hr:contract:list']"
                  @click="onViewAttachment(row)"
                >
                  <template #icon>
                    <IconifyIcon icon="lucide:paperclip" />
                  </template>
                  {{ $t('hr.contract.viewAttachment') }}
                </Button>
                <span v-else class="text-muted-foreground">-</span>
              </template>
              <template #action="{ row }">
                <VbenTableAction
                  :actions="[
                    {
                      text: $t('common.edit'),
                      icon: 'lucide:edit',
                      disabled: row.status !== 'ACTIVE',
                      onClick: () => onEditContract(row),
                    },
                    {
                      text: $t('hr.contract.terminate'),
                      icon: 'lucide:file-x-2',
                      danger: true,
                      disabled: row.status !== 'ACTIVE',
                      onClick: () => onTerminateContract(row),
                    },
                  ]"
                  align="center"
                />
              </template>
            </HistoryGrid>
          </div>
        </div>
      </TabPane>
      <TabPane key="expiring" :tab="$t('hr.contract.expiringContracts')">
        <ExpiringGrid :table-title="$t('hr.contract.expiringContracts')">
          <template #toolbar-tools>
            <Button :loading="expiringExportLoading" @click="onExportExpiringContracts">
              <template #icon>
                <IconifyIcon icon="lucide:download" />
              </template>
              {{ $t('hr.contract.exportExpiringCsv') }}
            </Button>
          </template>
        </ExpiringGrid>
      </TabPane>
    </Tabs>
  </Page>
</template>
