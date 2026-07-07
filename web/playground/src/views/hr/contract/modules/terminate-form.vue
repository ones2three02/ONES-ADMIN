<script lang="ts" setup>
import type { HrContractApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { terminateContract } from '#/api';
import { $t } from '#/locales';

import { useTerminateSchema } from '../data';

const emits = defineEmits(['success']);

const contractId = ref<string | number>();

const [Form, formApi] = useVbenForm({
  schema: useTerminateSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues<HrContractApi.TerminateRequest>();
    if (!contractId.value) return;

    drawerApi.lock();
    terminateContract(contractId.value, values)
      .then(() => {
        emits('success');
        drawerApi.close();
      })
      .catch(() => {
        drawerApi.unlock();
      });
  },

  async onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<HrContractApi.HrContract>();
      formApi.resetForm();

      if (data) {
        contractId.value = data.id;
      }
      await nextTick();
    }
  },
});

const getDrawerTitle = computed(() => {
  return $t('hr.contract.terminate');
});
</script>
<template>
  <Drawer :title="getDrawerTitle">
    <Form />
  </Drawer>
</template>
