<script lang="ts" setup>
import type { Recordable } from '@vben/types';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SystemDeptApi, SystemUserApi } from '#/api';

import { useDebounceFn } from '@vueuse/core';
import { computed, onMounted, ref, watch } from 'vue';

import { Page, Tree, useVbenDrawer } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { Button, Empty, InputSearch, message, Modal, Spin } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { deleteUser, getDeptList, getUserList, updateUser } from '#/api';
import { $t } from '#/locales';

import GuidedWorkbenchBar from '../../shared/guided-workbench-bar.vue';
import SplitListLayout from '../../shared/split-list-layout.vue';
import { useColumns, useGridFormSchema } from './data';
import Detail from './modules/detail.vue';
import Form from './modules/form.vue';

const deptList = ref<SystemDeptApi.SystemDept[]>([]);
const fullDeptList = ref<SystemDeptApi.SystemDept[]>([]);
const inputSearchValue = ref('');
const selectedDeptId = ref<string>('');
const deptLoading = ref(false);

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [DetailDrawer, detailDrawerApi] = useVbenDrawer({
  connectedComponent: Detail,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    commonConfig: {
      labelWidth: 96,
    },
    fieldMappingTime: [['createTime', ['startTime', 'endTime']]],
    schema: useGridFormSchema(),
    submitOnChange: true,
    wrapperClass: 'grid-cols-1 xl:grid-cols-2',
  },
  gridOptions: {
    columns: useColumns(onStatusChange),
    autoResize: true,
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getUserList({
            page: page.currentPage,
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
  } as VxeTableGridOptions<SystemUserApi.SystemUser>,
});

const selectedDeptName = computed(() => {
  if (!selectedDeptId.value) return '';
  return findDeptName(fullDeptList.value, selectedDeptId.value) ?? '';
});

const userTableTitle = computed(() =>
  selectedDeptName.value
    ? $t('system.user.departmentUserList', { name: selectedDeptName.value })
    : $t('system.user.list'),
);

const userScopeTitle = computed(() =>
  selectedDeptName.value
    ? $t('system.user.currentDepartment', { name: selectedDeptName.value })
    : $t('system.user.allDepartmentScope'),
);

const userScopeDescription = computed(() =>
  selectedDeptName.value
    ? $t('system.user.currentDepartmentTip')
    : $t('system.user.allDepartmentTip'),
);

/**
 * 将Antd的Modal.confirm封装为promise，方便在异步函数中调用。
 * @param content 提示内容
 * @param title 提示标题
 */
function confirm(content: string, title: string) {
  return new Promise((reslove, reject) => {
    Modal.confirm({
      content,
      onCancel() {
        reject(new Error('已取消'));
      },
      onOk() {
        reslove(true);
      },
      title,
    });
  });
}

/**
 * 状态开关即将改变
 * @param newStatus 期望改变的状态值
 * @param row 行数据
 * @returns 返回false则中止改变，返回其他值（undefined、true）则允许改变
 */
async function onStatusChange(
  newStatus: number,
  row: SystemUserApi.SystemUser,
) {
  const status: Recordable<string> = {
    0: '禁用',
    1: '启用',
  };
  try {
    await confirm(
      `你要将${row.name}的状态切换为 【${status[newStatus.toString()]}】 吗？`,
      `切换状态`,
    );
    await updateUser(row.id, { status: newStatus });
    return true;
  } catch {
    return false;
  }
}

function onEdit(row: SystemUserApi.SystemUser) {
  formDrawerApi.setData(row).open();
}

function onDetail(row: SystemUserApi.SystemUser) {
  detailDrawerApi.setData(row).open();
}

function onDelete(row: SystemUserApi.SystemUser) {
  const hideLoading = message.loading({
    content: $t('ui.actionMessage.deleting', [row.name]),
    duration: 0,
    key: 'action_process_msg',
  });
  deleteUser(row.id)
    .then(() => {
      message.success({
        content: $t('ui.actionMessage.deleteSuccess', [row.name]),
        key: 'action_process_msg',
      });
      onRefresh();
    })
    .catch(() => {
      hideLoading();
    });
}

function onRefresh() {
  gridApi.query();
}

function onCreate() {
  formDrawerApi.setData({}).open();
}

async function loadDeptList() {
  deptLoading.value = true;
  try {
    const res = await getDeptList();
    fullDeptList.value = res;
    deptList.value = filterDeptTree(res, inputSearchValue.value);
    if (
      selectedDeptId.value &&
      !findDeptName(fullDeptList.value, selectedDeptId.value)
    ) {
      selectedDeptId.value = '';
      onRefresh();
    }
  } catch (error) {
    console.error('Failed to load department list:', error);
    message.error($t('system.user.deptLoadError'));
  } finally {
    deptLoading.value = false;
  }
}

function selectDept(item: { value?: SystemDeptApi.SystemDept }) {
  selectedDeptId.value = item.value?.id || '';
  gridApi.query();
}

function clearDeptSelection() {
  if (!selectedDeptId.value) {
    return;
  }
  selectedDeptId.value = '';
  gridApi.query();
}

function clearDeptSearch() {
  inputSearchValue.value = '';
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

const debouncedSearchDept = useDebounceFn(searchDept, 200);

onMounted(() => {
  loadDeptList();
});

watch(inputSearchValue, (value) => {
  debouncedSearchDept(value);
});
</script>
<template>
  <Page auto-content-height>
    <FormDrawer @success="onRefresh" />
    <DetailDrawer @success="onRefresh" />
    <SplitListLayout :side-title="$t('system.dept.title')">
      <template #aside>
        <InputSearch
          v-model:value="inputSearchValue"
          allow-clear
          :placeholder="$t('system.user.placeholder')"
          class="system-user-dept-search"
        />
        <Button
          block
          :disabled="!selectedDeptId"
          size="small"
          type="link"
          @click="clearDeptSelection"
        >
          {{ $t('system.user.allDepartments') }}
        </Button>
        <Spin :spinning="deptLoading" class="system-user-dept-spin">
          <div v-if="deptList.length > 0" class="system-user-dept-tree">
            <Tree
              v-model="selectedDeptId"
              label-field="name"
              value-field="id"
              :tree-data="deptList"
              :default-expanded-level="2"
              @select="selectDept"
            />
          </div>
          <Empty
            v-else
            :description="$t('system.user.deptEmptyDescription')"
            image="simple"
          >
            <Button v-if="inputSearchValue" type="link" @click="clearDeptSearch">
              {{ $t('system.user.clearDeptSearch') }}
            </Button>
          </Empty>
        </Spin>
      </template>

      <GuidedWorkbenchBar
        icon="lucide:users-round"
        :title="userScopeTitle"
        :description="userScopeDescription"
        :status-text="
          selectedDeptName
            ? $t('system.user.departmentScoped')
            : $t('system.user.allDepartmentStatus')
        "
        :action-text="$t('system.user.allDepartments')"
        :action-disabled="!selectedDeptId"
        @action="clearDeptSelection"
      />

      <Grid :table-title="userTableTitle">
        <template #empty>
          <Empty :description="$t('system.user.emptyDescription')" image="simple">
            <Button type="primary" @click="onCreate">
              <Plus class="size-5" />
              {{ $t('ui.actionTitle.create', [$t('system.user.name')]) }}
            </Button>
          </Empty>
        </template>
        <template #toolbar-tools>
          <Button type="primary" @click="onCreate">
            <Plus class="size-5" />
            {{ $t('ui.actionTitle.create', [$t('system.user.name')]) }}
          </Button>
        </template>
        <template #action="{ row }">
          <VbenTableAction
            :actions="[
              {
                text: $t('common.detail'),
                icon: 'lucide:eye',
                onClick: () => onDetail(row),
              },
              {
                text: $t('common.edit'),
                icon: 'lucide:edit',
                onClick: () => onEdit(row),
              },
            ]"
            :dropdown-actions="[
              {
                text: $t('common.delete'),
                icon: 'lucide:trash-2',
                danger: true,
                popConfirm: {
                  title: $t('ui.actionMessage.deleteConfirm', [row.name]),
                  confirm: () => onDelete(row),
                },
                auth: ['AC_100100'],
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
.system-user-dept-search {
  flex: 0 0 auto;
  margin-bottom: 12px;
}

.system-user-dept-spin,
.system-user-dept-spin :deep(.ant-spin-container) {
  min-height: 0;
  flex: 1;
}

.system-user-dept-spin :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
}

.system-user-dept-tree {
  min-height: 0;
  flex: 1;
  overflow: auto;
}

.system-user-dept-tree :deep(.ant-tree-node-content-wrapper) {
  min-width: 0;
}

.system-user-dept-tree :deep(.ant-tree-title) {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}
</style>
