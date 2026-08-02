<script lang="ts" setup>
import type { HrEmployeeApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { regularizeEmployee } from '#/api';
import { $t } from '#/locales';

import { useRegularizeSchema } from '../data';

const emits = defineEmits(['success']);

const employeeId = ref<string | number>();
const employeeName = ref('');

const [Form, formApi] = useVbenForm({
  schema: useRegularizeSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues<HrEmployeeApi.RegularizeRequest>();
    if (!employeeId.value) return;

    drawerApi.lock();
    regularizeEmployee(employeeId.value, values)
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
      const data = drawerApi.getData<HrEmployeeApi.HrEmployee>();
      formApi.resetForm();

      if (data) {
        employeeId.value = data.id;
        employeeName.value = data.realName;
      }
      await nextTick();
    }
  },
});

const getDrawerTitle = computed(() => {
  return $t('hr.employeeLifecycle.regularizeTitle', {
    name: employeeName.value,
  });
});
</script>
<template>
  <Drawer :title="getDrawerTitle">
    <Form />
  </Drawer>
</template>
