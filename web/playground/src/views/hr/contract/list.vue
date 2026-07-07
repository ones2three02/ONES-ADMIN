<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrContractApi, HrEmployeeApi } from '#/api';

import { computed, onMounted, ref, watch } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';

import { Button, Card, InputSearch, message, Statistic, TabPane, Tabs } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  exportExpiringContracts,
  getContractAttachmentMetadata,
  getEmployeeContracts,
  getEmployeeList,
  getExpiringContracts,
} from '#/api';
import { $t } from '#/locales';

import { errorMessageOf } from '../shared/error';
import { downloadHrBlobWithFeedback, openHrFileWithFeedback } from '../shared/file';
import { isDueWithinDays } from '../shared/warning';
import { useColumns, useExpiringColumns, useExpiringGridFormSchema } from './data';
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
const expiringContracts = ref<HrContractApi.HrContract[]>([]);
const expiringWindowDays = ref(30);
const expiringExportLoading = ref(false);

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [TerminateDrawer, terminateDrawerApi] = useVbenDrawer({
  connectedComponent: TerminateForm,
  destroyOnClose: true,
});

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

const [ExpiringGrid, expiringGridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useExpiringGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: useExpiringColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async (_, formValues) => {
          const days = Number(formValues?.days ?? 30);
          expiringWindowDays.value = days;
          const result = await getExpiringContracts(days);
          expiringContracts.value = result;
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
  } as VxeTableGridOptions<HrContractApi.HrContract>,
});

const expiringMetrics = computed(() => [
  {
    icon: 'lucide:file-warning',
    title: $t('hr.contract.metricTotal'),
    value: expiringContracts.value.length,
  },
  {
    icon: 'lucide:badge-alert',
    title: $t('hr.contract.metricSevenDays'),
    value: expiringContracts.value.filter((item) => isDueWithinDays(item.endDate, 7)).length,
  },
  {
    icon: 'lucide:calendar-clock',
    title: $t('hr.contract.metricWindow'),
    value: expiringWindowDays.value,
  },
]);

async function loadEmployeeList() {
  try {
    const res = await getEmployeeList({
      pageNum: 1,
      pageSize: 50,
      keyword: searchEmployeeValue.value || undefined,
    });
    employees.value = res.items;
    if (res.items.length > 0 && !selectedEmployeeId.value) {
      const firstEmployee = res.items[0];
      if (firstEmployee) {
        selectEmployee(firstEmployee);
      }
    }
  } catch (error: unknown) {
    message.error(errorMessageOf(error, $t('hr.contract.employeeLoadError')));
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

async function onViewAttachment(row: HrContractApi.HrContract) {
  if (!row.attachmentFileId) {
    message.warning($t('hr.contract.noAttachment'));
    return;
  }
  try {
    const metadata = await getContractAttachmentMetadata(row.id);
    await openHrFileWithFeedback(metadata, {
      errorMessage: $t('hr.contract.attachmentOpenError'),
      loadingMessage: $t('hr.contract.attachmentOpening'),
      unavailableMessage: $t('hr.contract.attachmentOpenUnavailable'),
    });
  } catch (error: unknown) {
    message.error(errorMessageOf(error, $t('hr.contract.attachmentLoadError')));
  }
}

async function onExportExpiringContracts() {
  expiringExportLoading.value = true;
  try {
    await downloadHrBlobWithFeedback(
      {
        errorMessage: $t('hr.contract.expiringExportError'),
        fileName: 'ones-hr-expiring-contracts.csv',
        loadingMessage: $t('hr.contract.expiringExporting'),
        successMessage: $t('hr.contract.expiringExportSuccess'),
      },
      () => exportExpiringContracts(expiringWindowDays.value),
    );
  } finally {
    expiringExportLoading.value = false;
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
              :placeholder="$t('hr.contract.employeeSearchPlaceholder')"
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
                  <span class="text-xs text-gray-500">
                    {{ $t('hr.employee.employeeNo') }}: {{ emp.employeeNo }}
                  </span>
                </div>
                <div class="text-xs text-gray-400 mt-1">
                  {{ emp.deptName }} | {{ emp.positionName || $t('hr.employee.noPosition') }}
                </div>
              </div>
            </div>
          </Card>

          <div class="w-3/4 ml-4">
            <HistoryGrid
              :table-title="
                selectedEmployee
                  ? $t('hr.contract.employeeContractHistory', {
                      name: selectedEmployee.realName,
                    })
                  : $t('hr.contract.contractHistory')
              "
            >
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
        <div class="flex size-full flex-col gap-4">
          <div class="grid gap-4 md:grid-cols-3">
            <Card v-for="item in expiringMetrics" :key="item.title" variant="borderless">
              <div class="flex items-start justify-between gap-3">
                <Statistic :title="item.title" :value="item.value" />
                <div class="rounded bg-muted p-2 text-primary">
                  <IconifyIcon :icon="item.icon" class="size-5" />
                </div>
              </div>
            </Card>
          </div>

          <div class="min-h-[420px] flex-1">
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
          </div>
        </div>
      </TabPane>
    </Tabs>
  </Page>
</template>
