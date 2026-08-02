<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SystemDictApi } from '#/api';

import { computed, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';

import { Button, Card, message, Modal, Statistic, Tag } from 'antdv-next';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import {
  deleteDictItem,
  deleteDictType,
  getDictItemList,
  getDictTypeList,
} from '#/api';

import {
  useItemColumns,
  useItemSearchSchema,
  useTypeColumns,
  useTypeSearchSchema,
} from './data';
import ItemForm from './modules/item-form.vue';
import TypeForm from './modules/type-form.vue';

defineOptions({ name: 'SystemDict' });

const selectedType = ref<SystemDictApi.DictType>();
const typeRows = ref<SystemDictApi.DictType[]>([]);
const itemRows = ref<SystemDictApi.DictItem[]>([]);

const [TypeDrawer, typeDrawerApi] = useVbenDrawer({
  connectedComponent: TypeForm,
  destroyOnClose: true,
});

const [ItemDrawer, itemDrawerApi] = useVbenDrawer({
  connectedComponent: ItemForm,
  destroyOnClose: true,
});

const [TypeGrid, typeGridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useTypeSearchSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    autoResize: true,
    columns: useTypeColumns(),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getDictTypeList({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
          typeRows.value = result.items;
          if (
            selectedType.value &&
            !result.items.some((item) => item.id === selectedType.value?.id)
          ) {
            selectedType.value = undefined;
          }
          if (!selectedType.value && result.items.length > 0) {
            selectedType.value = result.items[0];
          }
          itemGridApi.query();
          return result;
        },
      },
    },
    rowConfig: {
      isCurrent: true,
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      export: false,
      refresh: true,
      search: true,
      zoom: true,
    },
  } as VxeTableGridOptions<SystemDictApi.DictType>,
});

const [ItemGrid, itemGridApi] = useVbenVxeGrid({
  formOptions: {
    schema: useItemSearchSchema(),
    submitOnChange: true,
  },
  gridOptions: {
    autoResize: true,
    columns: useItemColumns(),
    height: '100%',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          if (!selectedType.value) {
            itemRows.value = [];
            return {
              items: [],
              total: 0,
            };
          }
          const result = await getDictItemList({
            pageNum: page.currentPage,
            pageSize: page.pageSize,
            typeId: selectedType.value.id,
            ...formValues,
          });
          itemRows.value = result.items;
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
  } as VxeTableGridOptions<SystemDictApi.DictItem>,
});

const metrics = computed(() => [
  {
    icon: 'lucide:book-open-text',
    title: '字典类型',
    value: typeRows.value.length,
  },
  {
    icon: 'lucide:list-tree',
    title: '当前字典项',
    value: itemRows.value.length,
  },
  {
    icon: 'lucide:circle-check',
    title: '启用字典项',
    value: itemRows.value.filter((item) => item.enabled).length,
  },
]);

function onRefreshTypes() {
  typeGridApi.query();
}

function onRefreshItems() {
  itemGridApi.query();
}

function onRefreshDicts() {
  itemGridApi.query();
  typeGridApi.query();
}

function onSelectType(row: SystemDictApi.DictType) {
  selectedType.value = row;
  onRefreshItems();
}

function onCreateType() {
  typeDrawerApi.setData({ enabled: true, sortOrder: 0 }).open();
}

function onEditType(row: SystemDictApi.DictType) {
  typeDrawerApi.setData(row).open();
}

function onDeleteType(row: SystemDictApi.DictType) {
  Modal.confirm({
    content: `确认删除字典类型「${row.dictName}」吗？删除前需要先清空该类型下的字典项。`,
    okButtonProps: { danger: true },
    okText: '确认删除',
    title: '删除字典类型',
    async onOk() {
      await deleteDictType(row.id);
      message.success('删除成功');
      onRefreshTypes();
    },
  });
}

function onCreateItem() {
  if (!selectedType.value) {
    message.warning('请先选择字典类型');
    return;
  }
  itemDrawerApi
    .setData({
      type: selectedType.value,
    })
    .open();
}

function onEditItem(row: SystemDictApi.DictItem) {
  itemDrawerApi
    .setData({
      item: row,
      type: selectedType.value,
    })
    .open();
}

function onDeleteItem(row: SystemDictApi.DictItem) {
  Modal.confirm({
    content: `确认删除字典项「${row.itemLabel}」吗？`,
    okButtonProps: { danger: true },
    okText: '确认删除',
    title: '删除字典项',
    async onOk() {
      await deleteDictItem(row.id);
      message.success('删除成功');
      onRefreshItems();
      typeGridApi.query();
    },
  });
}
</script>

<template>
  <Page auto-content-height title="数据字典">
    <TypeDrawer @success="onRefreshTypes" />
    <ItemDrawer @success="onRefreshDicts" />

    <div class="flex size-full flex-col gap-4 p-4" data-testid="system-dict-page">
      <div class="flex items-center justify-between gap-3">
        <div>
          <div class="flex items-center gap-2 text-lg font-medium">
            <IconifyIcon class="size-5 text-primary" icon="lucide:book-open-text" />
            数据字典
          </div>
          <div class="text-muted-foreground mt-1 text-sm">
            统一维护系统与 HRMS 常用枚举，沉淀前后端可复用的选项资产。
          </div>
        </div>
        <Button @click="onRefreshTypes">
          <template #icon>
            <IconifyIcon icon="lucide:refresh-cw" />
          </template>
          刷新
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

      <div class="grid min-h-[420px] flex-1 gap-4 xl:grid-cols-[minmax(420px,0.9fr)_1.4fr]">
        <Card variant="borderless" class="system-dict-grid-card min-w-0">
          <div class="min-h-0 flex-1">
            <TypeGrid table-title="字典类型">
              <template #toolbar-tools>
                <Button
                  type="primary"
                  v-access:code="['system:dict:type:create']"
                  @click="onCreateType"
                >
                  <Plus class="size-5" />
                  新增类型
                </Button>
              </template>
              <template #typeAction="{ row }">
                <VbenTableAction
                  :actions="[
                    {
                      text: '选择',
                      icon: 'lucide:mouse-pointer-click',
                      onClick: () => onSelectType(row),
                    },
                    {
                      text: '编辑',
                      icon: 'lucide:edit',
                      auth: ['system:dict:type:update'],
                      onClick: () => onEditType(row),
                    },
                  ]"
                  :dropdown-actions="[
                    {
                      text: '删除',
                      icon: 'lucide:trash-2',
                      danger: true,
                      auth: ['system:dict:type:delete'],
                      disabled: row.itemCount > 0,
                      onClick: () => onDeleteType(row),
                    },
                  ]"
                  align="center"
                />
              </template>
            </TypeGrid>
          </div>
        </Card>

        <Card variant="borderless" class="system-dict-grid-card min-w-0">
          <div class="mb-3 flex items-center justify-between gap-3">
            <div class="min-w-0">
              <div class="truncate text-sm font-medium">
                {{ selectedType?.dictName || '字典项' }}
              </div>
              <div class="text-muted-foreground mt-1 truncate text-xs">
                {{ selectedType?.dictCode || '请选择左侧字典类型' }}
              </div>
            </div>
            <Tag v-if="selectedType" :color="selectedType.enabled ? 'success' : 'error'">
              {{ selectedType.enabled ? '启用' : '停用' }}
            </Tag>
          </div>

          <div class="min-h-0 flex-1">
            <ItemGrid :table-title="selectedType ? '字典项' : '字典项'">
              <template #toolbar-tools>
                <Button
                  type="primary"
                  :disabled="!selectedType"
                  v-access:code="['system:dict:item:create']"
                  @click="onCreateItem"
                >
                  <Plus class="size-5" />
                  新增字典项
                </Button>
              </template>
              <template #itemAction="{ row }">
                <VbenTableAction
                  :actions="[
                    {
                      text: '编辑',
                      icon: 'lucide:edit',
                      auth: ['system:dict:item:update'],
                      onClick: () => onEditItem(row),
                    },
                    {
                      text: '删除',
                      icon: 'lucide:trash-2',
                      danger: true,
                      auth: ['system:dict:item:delete'],
                      onClick: () => onDeleteItem(row),
                    },
                  ]"
                  align="center"
                />
              </template>
            </ItemGrid>
          </div>
        </Card>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.system-dict-grid-card {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.system-dict-grid-card :deep(.ant-card-body) {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
}
</style>
