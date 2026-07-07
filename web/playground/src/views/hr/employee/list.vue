<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrEmployeeApi, SystemDeptApi } from '#/api';

import { computed, onMounted, ref, watch } from 'vue';

import { Page, Tree, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';

import { Button, Empty, InputSearch, message } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { exportEmployees, getDeptList, getEmployeeList } from '#/api';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import {
  getHrDictOptions,
  HR_EMPLOYMENT_STATUS_DICT,
  HR_EMPLOYMENT_TYPE_DICT,
  HR_GENDER_DICT,
} from '../dict-options';
import { errorMessageOf } from '../shared/error';
import { downloadHrBlobWithFeedback } from '../shared/file';
import GuidedWorkbenchBar from '../../shared/guided-workbench-bar.vue';
import SplitListLayout from '../../shared/split-list-layout.vue';
import Form from './modules/form.vue';
import Lifecycle from './modules/lifecycle.vue';
import Profile from './modules/profile.vue';
import RegularizeForm from './modules/regularize-form.vue';
import ResignForm from './modules/resign-form.vue';
import TransferForm from './modules/transfer-form.vue';

const deptList = ref<SystemDeptApi.SystemDept[]>([]);
const fullDeptList = ref<SystemDeptApi.SystemDept[]>([]);
const searchDeptValue = ref('');
const selectedDeptId = ref<string>('');
const latestQuery = ref<Partial<HrEmployeeApi.EmployeeQuery>>({});

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [TransferDrawer, transferDrawerApi] = useVbenDrawer({
  connectedComponent: TransferForm,
  destroyOnClose: true,
});

const [RegularizeDrawer, regularizeDrawerApi] = useVbenDrawer({
  connectedComponent: RegularizeForm,
  destroyOnClose: true,
});

const [ResignDrawer, resignDrawerApi] = useVbenDrawer({
  connectedComponent: ResignForm,
  destroyOnClose: true,
});

const [LifecycleDrawer, lifecycleDrawerApi] = useVbenDrawer({
  connectedComponent: Lifecycle,
  destroyOnClose: true,
});

const [ProfileDrawer, profileDrawerApi] = useVbenDrawer({
  connectedComponent: Profile,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    commonConfig: {
      labelWidth: 120,
    },
    schema: useGridFormSchema(),
    submitOnChange: true,
    wrapperClass: 'grid-cols-1 xl:grid-cols-2',
  },
  gridOptions: {
    autoResize: true,
    columns: [
      ...(useColumns() as any[]),
      {
        align: 'center',
        field: 'action',
        fixed: 'right',
        title: $t('hr.employee.action'),
        width: 188,
        slots: { default: 'action' },
      },
    ],
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          latestQuery.value = {
            ...formValues,
            deptId: selectedDeptId.value || undefined,
          };
          return await getEmployeeList({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...latestQuery.value,
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
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<HrEmployeeApi.HrEmployee>,
});

const selectedDeptName = computed(() => {
  if (!selectedDeptId.value) return '';
  return findDeptName(fullDeptList.value, selectedDeptId.value) ?? '';
});

const employeeScopeTitle = computed(() =>
  selectedDeptName.value
    ? $t('hr.employeeList.currentDepartment', { name: selectedDeptName.value })
    : $t('hr.employeeList.allDepartmentScope'),
);

const employeeScopeDescription = computed(() =>
  selectedDeptName.value
    ? $t('hr.employeeList.currentDepartmentTip')
    : $t('hr.employeeList.allDepartmentTip'),
);

function onRefresh() {
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

async function onExport() {
  await downloadHrBlobWithFeedback(
    {
      errorMessage: $t('hr.employeeList.exportError'),
      fileName: 'ones-hr-employees.csv',
      loadingMessage: $t('hr.employeeList.exporting'),
      successMessage: $t('hr.employeeList.exportSuccess'),
    },
    () => exportEmployees({
      ...latestQuery.value,
      deptId: selectedDeptId.value || undefined,
    }),
  );
}

function onEdit(row: HrEmployeeApi.HrEmployee) {
  formDrawerApi.setData(row).open();
}

function onTransfer(row: HrEmployeeApi.HrEmployee) {
  transferDrawerApi.setData(row).open();
}

function onRegularize(row: HrEmployeeApi.HrEmployee) {
  regularizeDrawerApi.setData(row).open();
}

function onResign(row: HrEmployeeApi.HrEmployee) {
  resignDrawerApi.setData(row).open();
}

function onViewLifecycle(row: HrEmployeeApi.HrEmployee) {
  lifecycleDrawerApi.setData(row).open();
}

function onViewProfile(row: HrEmployeeApi.HrEmployee) {
  profileDrawerApi.setData(row).open();
}

async function loadDeptList() {
  try {
    const res = await getDeptList();
    fullDeptList.value = res;
    deptList.value = filterDeptTree(res, searchDeptValue.value);
    if (
      selectedDeptId.value &&
      !findDeptName(fullDeptList.value, selectedDeptId.value)
    ) {
      selectedDeptId.value = '';
      onRefresh();
    }
  } catch (error: unknown) {
    message.error(errorMessageOf(error, $t('hr.employeeList.deptLoadError')));
  }
}

function selectDept(item: { value?: SystemDeptApi.SystemDept }) {
  selectedDeptId.value = item.value?.id || '';
  onRefresh();
}

function clearDeptSelection() {
  if (!selectedDeptId.value) {
    return;
  }
  selectedDeptId.value = '';
  onRefresh();
}

function clearDeptSearch() {
  searchDeptValue.value = '';
}

function searchDept(value: string) {
  deptList.value = filterDeptTree(fullDeptList.value, value);
}

function filterDeptTree(
  depts: SystemDeptApi.SystemDept[],
  keyword: string,
): SystemDeptApi.SystemDept[] {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) return depts;

  const filtered: SystemDeptApi.SystemDept[] = [];
  for (const dept of depts) {
    const children = filterDeptTree(dept.children ?? [], keyword);
    const matched = dept.name.toLowerCase().includes(normalizedKeyword);
    if (!matched && children.length === 0) continue;
    filtered.push({
      ...dept,
      children,
    });
  }
  return filtered;
}

function findDeptName(
  depts: SystemDeptApi.SystemDept[],
  deptId: string,
): string | undefined {
  for (const dept of depts) {
    if (dept.id === deptId) return dept.name;
    const childName = findDeptName(dept.children ?? [], deptId);
    if (childName) return childName;
  }
  return undefined;
}

onMounted(async () => {
  await Promise.all([
    getHrDictOptions(HR_GENDER_DICT),
    getHrDictOptions(HR_EMPLOYMENT_TYPE_DICT),
    getHrDictOptions(HR_EMPLOYMENT_STATUS_DICT),
  ]);
  loadDeptList();
  onRefresh();
});

watch(searchDeptValue, (value) => {
  searchDept(value);
});
</script>
<template>
  <Page auto-content-height>
    <FormDrawer @success="onRefresh" />
    <TransferDrawer @success="onRefresh" />
    <RegularizeDrawer @success="onRefresh" />
    <ResignDrawer @success="onRefresh" />
    <LifecycleDrawer @success="onRefresh" />
    <ProfileDrawer @success="onRefresh" />

    <SplitListLayout :side-title="$t('system.dept.title')">
      <template #aside>
        <div class="employee-dept-search">
          <InputSearch
            v-model:value="searchDeptValue"
            :placeholder="$t('hr.employeeList.deptSearchPlaceholder')"
          />
          <Button
            block
            :disabled="!selectedDeptId"
            size="small"
            type="link"
            @click="clearDeptSelection"
          >
            {{ $t('hr.employeeList.allDepartments') }}
          </Button>
        </div>
        <div class="employee-dept-tree">
          <Tree
            v-if="deptList.length > 0"
            v-model="selectedDeptId"
            label-field="name"
            value-field="id"
            :tree-data="deptList"
            :default-expanded-level="2"
            @select="selectDept"
          />
          <Empty
            v-else
            :description="$t('hr.employeeList.deptEmptyDescription')"
            image="simple"
          >
            <Button v-if="searchDeptValue" type="link" @click="clearDeptSearch">
              {{ $t('hr.employeeList.clearDeptSearch') }}
            </Button>
          </Empty>
        </div>
      </template>

      <GuidedWorkbenchBar
        :title="employeeScopeTitle"
        :description="employeeScopeDescription"
        :status-text="
          selectedDeptName
            ? $t('hr.employeeList.departmentScoped')
            : $t('hr.employeeList.allDepartmentStatus')
        "
        :action-text="$t('hr.employeeList.allDepartments')"
        :action-disabled="!selectedDeptId"
        @action="clearDeptSelection"
      />

      <Grid :table-title="$t('hr.employee.list')">
        <template #empty>
          <Empty
            :description="$t('hr.employeeList.emptyDescription')"
            image="simple"
          >
            <Button type="primary" @click="onCreate">
              <Plus class="size-5" />
              {{ $t('hr.employee.createEmployee') }}
            </Button>
          </Empty>
        </template>
        <template #toolbar-tools>
          <Button v-access:code="['hr:employee:export']" class="mr-2" @click="onExport">
            <IconifyIcon icon="lucide:download" class="size-4" />
            {{ $t('hr.employeeList.exportRoster') }}
          </Button>
          <Button type="primary" @click="onCreate">
            <Plus class="size-5" />
            {{ $t('hr.employee.createEmployee') }}
          </Button>
        </template>
        <template #action="{ row }">
          <VbenTableAction
            :actions="[
              {
                text: $t('hr.employeeList.profile'),
                icon: 'lucide:user-round-search',
                onClick: () => onViewProfile(row),
                auth: ['hr:employee:detail'],
              },
              {
                text: $t('hr.employee.editEmployee'),
                icon: 'lucide:user-pen',
                onClick: () => onEdit(row),
                auth: ['hr:employee:update'],
              },
              {
                text: $t('hr.employee.lifecycleEvents'),
                icon: 'lucide:history',
                onClick: () => onViewLifecycle(row),
              },
            ]"
            :dropdown-actions="[
              {
                text: $t('hr.employee.transfer'),
                icon: 'lucide:shuffle',
                disabled: row.employmentStatus === 'RESIGNED',
                onClick: () => onTransfer(row),
                auth: ['hr:employee:transfer'],
              },
              {
                text: $t('hr.employee.regularize'),
                icon: 'lucide:badge-check',
                disabled: row.employmentStatus !== 'PROBATION',
                onClick: () => onRegularize(row),
                auth: ['hr:employee:regularize'],
              },
              {
                text: $t('hr.employee.resign'),
                icon: 'lucide:user-minus',
                danger: true,
                disabled: row.employmentStatus === 'RESIGNED',
                onClick: () => onResign(row),
                auth: ['hr:employee:resign'],
              },
            ]"
            align="center"
          />
        </template>
      </Grid>
    </SplitListLayout>
  </Page>
</template>

<style scoped>
.employee-dept-search {
  flex: 0 0 auto;
  margin-bottom: 12px;
}

.employee-dept-tree {
  min-height: 0;
  flex: 1;
  overflow: auto;
}

.employee-dept-tree :deep(.ant-tree-node-content-wrapper) {
  min-width: 0;
}

.employee-dept-tree :deep(.ant-tree-title) {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}
</style>
