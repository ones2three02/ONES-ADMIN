<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { HrEmployeeApi } from '#/api';

import { onMounted, ref, watch } from 'vue';

import { Page, Tree, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, Card, InputSearch } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { getDeptList, getEmployeeList } from '#/api';
import { $t } from '#/locales';

import { useColumns, useGridFormSchema } from './data';
import Form from './modules/form.vue';
import Lifecycle from './modules/lifecycle.vue';
import RegularizeForm from './modules/regularize-form.vue';
import ResignForm from './modules/resign-form.vue';
import TransferForm from './modules/transfer-form.vue';

const deptList = ref<any[]>([]);
const searchDeptValue = ref('');
const selectedDeptId = ref<string>('');

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

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useGridFormSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    columns: [
      ...(useColumns() as any[]),
      {
        align: 'center',
        field: 'action',
        fixed: 'right',
        title: $t('common.action'),
        width: 220,
        slots: { default: 'action' },
      },
    ],
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getEmployeeList({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            deptId: selectedDeptId.value || undefined,
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

function onRefresh() {
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
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

async function loadDeptList() {
  try {
    const res = await getDeptList();
    deptList.value = res;
  } catch (error) {
    console.error('Failed to load department list:', error);
  }
}

function selectDept(v: string) {
  selectedDeptId.value = v;
  onRefresh();
}

function searchDept(value: string) {
  if (!value) {
    loadDeptList();
    return;
  }
  const filtered = deptList.value.filter((dept) =>
    dept.name.toLowerCase().includes(value.toLowerCase()),
  );
  deptList.value = filtered;
}

onMounted(() => {
  loadDeptList();
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

    <div class="flex size-full">
      <!-- 左侧部门树 -->
      <Card class="w-1/5" :title="$t('system.dept.title')">
        <InputSearch
          v-model:value="searchDeptValue"
          placeholder="搜索部门..."
          class="mb-4"
        />
        <Tree
          label-field="name"
          value-field="id"
          :tree-data="deptList"
          :default-expanded-level="2"
          @select="selectDept"
        />
      </Card>

      <!-- 右侧员工列表 -->
      <div class="w-4/5 ml-4">
        <Grid :table-title="$t('hr.employee.list')">
          <template #toolbar-tools>
            <Button type="primary" @click="onCreate">
              <Plus class="size-5" />
              {{ $t('hr.employee.createEmployee') }}
            </Button>
          </template>
          <template #action="{ row }">
            <VbenTableAction
              :actions="[
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
      </div>
    </div>
  </Page>
</template>
