<script lang="ts" setup>
import type { HrEmployeeApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { transferEmployee } from '#/api';
import { $t } from '#/locales';

import { useTransferSchema } from '../data';

const emits = defineEmits(['success']);

const employeeId = ref<string | number>();
const employeeName = ref('');

const [Form, formApi] = useVbenForm({
  schema: useTransferSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    if (!employeeId.value) return;

    drawerApi.lock();
    transferEmployee(employeeId.value, values as any)
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
      if (data) {
        formApi.setValues({
          deptId: data.deptId,
          positionId: data.positionId,
          gradeId: data.gradeId,
          managerEmployeeId: data.managerEmployeeId,
        });
      }
    }
  },
});

const getDrawerTitle = computed(() => {
  return $t('hr.employeeLifecycle.transferTitle', {
    name: employeeName.value,
  });
});
</script>
<template>
  <Drawer :title="getDrawerTitle">
    <Form />
  </Drawer>
</template>
