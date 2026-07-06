<script lang="ts" setup>
import type { SystemDictApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createDictItem, updateDictItem } from '#/api';

import { useItemFormSchema } from '../data';

const emit = defineEmits(['success']);

const current = ref<SystemDictApi.DictItem>();
const selectedType = ref<SystemDictApi.DictType>();

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useItemFormSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues<SystemDictApi.DictItemSaveRequest>();
    const payload = {
      ...values,
      dictCode: selectedType.value?.dictCode ?? values.dictCode,
      typeId: selectedType.value?.id ?? values.typeId,
    };
    drawerApi.lock();
    try {
      if (current.value?.id) {
        await updateDictItem(current.value.id, payload);
      } else {
        await createDictItem(payload);
      }
      emit('success');
      drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    formApi.resetForm();
    const data = drawerApi.getData<{
      item?: SystemDictApi.DictItem;
      type?: SystemDictApi.DictType;
    }>();
    current.value = data?.item?.id ? data.item : undefined;
    selectedType.value = data?.type;
    await nextTick();
    formApi.setValues({
      ...(data?.item ?? {}),
      dictCode: data?.type?.dictCode ?? data?.item?.dictCode,
      enabled: data?.item?.enabled ?? true,
      sortOrder: data?.item?.sortOrder ?? 0,
      typeId: data?.type?.id ?? data?.item?.typeId,
    });
  },
});

const title = computed(() => (current.value?.id ? '编辑字典项' : '新增字典项'));
</script>

<template>
  <Drawer :title="title">
    <Form />
  </Drawer>
</template>
