<script lang="ts" setup>
import type { SystemDictApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createDictType, updateDictType } from '#/api';

import { useTypeFormSchema } from '../data';

const emit = defineEmits(['success']);

const current = ref<SystemDictApi.DictType>();

const [Form, formApi] = useVbenForm({
  layout: 'vertical',
  schema: useTypeFormSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const values = await formApi.getValues<SystemDictApi.DictTypeSaveRequest>();
    drawerApi.lock();
    try {
      if (current.value?.id) {
        await updateDictType(current.value.id, values);
      } else {
        await createDictType(values);
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
    const data = drawerApi.getData<SystemDictApi.DictType>();
    current.value = data?.id ? data : undefined;
    await nextTick();
    if (data) {
      formApi.setValues(data);
    }
  },
});

const title = computed(() => (current.value?.id ? '编辑字典类型' : '新增字典类型'));
</script>

<template>
  <Drawer :title="title">
    <Form />
  </Drawer>
</template>
