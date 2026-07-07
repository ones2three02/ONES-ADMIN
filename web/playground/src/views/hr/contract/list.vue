<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrContractApi, HrEmployeeApi } from '#/api';

import { useDebounceFn } from '@vueuse/core';
import { computed, onMounted, ref, watch } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';

import {
  Button,
  Card,
  Empty,
  InputSearch,
  message,
  Spin,
  Statistic,
  TabPane,
  Tabs,
} from 'antdv-next';

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
const employeeLoading = ref(false);
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
    autoResize: true,
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
    height: '100%',
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
    autoResize: true,
    columns: useExpiringColumns(),
    height: '100%',
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

const historyEmptyDescription = computed(() =>
  selectedEmployee.value
    ? $t('hr.contract.historyEmptyDescription')
    : $t('hr.contract.historyEmptySelectDescription'),
);

async function loadEmployeeList() {
  employeeLoading.value = true;
  try {
    const res = await getEmployeeList({
      pageNum: 1,
      pageSize: 50,
      keyword: searchEmployeeValue.value || undefined,
    });
    employees.value = res.items;
    const currentEmployee = res.items.find(
      (item) => item.id === selectedEmployeeId.value,
    );
    const nextEmployee = currentEmployee ?? res.items[0] ?? null;
    if ((nextEmployee?.id ?? '') !== selectedEmployeeId.value) {
      selectEmployee(nextEmployee);
    } else {
      selectedEmployee.value = nextEmployee;
    }
  } catch (error: unknown) {
    message.error(errorMessageOf(error, $t('hr.contract.employeeLoadError')));
  } finally {
    employeeLoading.value = false;
  }
}

function selectEmployee(emp: HrEmployeeApi.HrEmployee | null) {
  selectedEmployeeId.value = emp?.id ?? '';
  selectedEmployee.value = emp;
  historyGridApi.query();
}

function clearEmployeeSearch() {
  searchEmployeeValue.value = '';
}

const debouncedLoadEmployeeList = useDebounceFn(loadEmployeeList, 250);

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
  debouncedLoadEmployeeList();
});
</script>
<template>
  <Page auto-content-height>
    <FormDrawer @success="onRefresh" />
    <TerminateDrawer @success="onRefresh" />

    <Tabs v-model:activeKey="activeTab" class="contract-tabs size-full">
      <TabPane key="historical" :tab="$t('hr.contract.list')">
        <div class="flex size-full min-h-0 gap-4">
          <Card
            class="contract-employee-card"
            :title="$t('hr.employee.title')"
            :body-style="{ display: 'flex', flexDirection: 'column', minHeight: 0 }"
          >
            <InputSearch
              v-model:value="searchEmployeeValue"
              allow-clear
              :placeholder="$t('hr.contract.employeeSearchPlaceholder')"
              class="contract-employee-search"
            />
            <Spin :spinning="employeeLoading" class="contract-employee-spin">
              <div v-if="employees.length > 0" class="contract-employee-list">
                <div
                  v-for="emp in employees"
                  :key="emp.id"
                  :class="[
                    'contract-employee-item',
                    selectedEmployeeId === emp.id
                      ? 'border-primary bg-primary/10 text-primary shadow-sm'
                      : 'border-transparent hover:border-border hover:bg-accent',
                  ]"
                  @click="selectEmployee(emp)"
                >
                  <div class="flex min-w-0 items-center justify-between gap-3">
                    <span class="truncate font-medium">{{ emp.realName }}</span>
                    <span class="shrink-0 text-xs text-muted-foreground">
                      {{ $t('hr.employee.employeeNo') }}: {{ emp.employeeNo }}
                    </span>
                  </div>
                  <div class="mt-1 truncate text-xs text-muted-foreground">
                    {{ emp.deptName }} | {{ emp.positionName || $t('hr.employee.noPosition') }}
                  </div>
                </div>
              </div>
              <Empty
                v-else
                :description="$t('hr.contract.employeeEmptyDescription')"
                image="simple"
              >
                <Button v-if="searchEmployeeValue" type="link" @click="clearEmployeeSearch">
                  {{ $t('hr.contract.clearEmployeeSearch') }}
                </Button>
              </Empty>
            </Spin>
          </Card>

          <div class="flex min-w-0 flex-1 flex-col">
            <HistoryGrid
              :table-title="
                selectedEmployee
                  ? $t('hr.contract.employeeContractHistory', {
                      name: selectedEmployee.realName,
                    })
                  : $t('hr.contract.contractHistory')
              "
            >
              <template #empty>
                <Empty :description="historyEmptyDescription" image="simple">
                  <Button
                    v-if="selectedEmployeeId"
                    type="primary"
                    @click="onSignContract"
                  >
                    <Plus class="size-5" />
                    {{ $t('hr.contract.create') }}
                  </Button>
                </Empty>
              </template>
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
        <div class="flex size-full min-h-0 flex-col gap-4">
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

          <div class="min-h-0 flex-1">
            <ExpiringGrid :table-title="$t('hr.contract.expiringContracts')">
              <template #empty>
                <Empty
                  :description="$t('hr.contract.expiringEmptyDescription')"
                  image="simple"
                />
              </template>
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

<style scoped>
.contract-tabs {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.contract-tabs :deep(.ant-tabs-content-holder),
.contract-tabs :deep(.ant-tabs-content),
.contract-tabs :deep(.ant-tabs-tabpane) {
  min-height: 0;
  flex: 1;
}

.contract-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.contract-employee-card {
  display: flex;
  width: 300px;
  min-width: 260px;
  max-width: 340px;
  min-height: 0;
  flex: 0 0 300px;
  flex-direction: column;
}

.contract-employee-card :deep(.ant-card-body) {
  flex: 1;
  overflow: hidden;
}

.contract-employee-search {
  flex: 0 0 auto;
  margin-bottom: 12px;
}

.contract-employee-spin,
.contract-employee-spin :deep(.ant-spin-container) {
  min-height: 0;
  flex: 1;
}

.contract-employee-spin :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.contract-employee-list {
  min-height: 0;
  flex: 1;
  overflow: auto;
}

.contract-employee-item {
  margin-bottom: 8px;
  cursor: pointer;
  border-width: 1px;
  border-radius: 6px;
  padding: 12px;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}
</style>
