<script lang="ts" setup>
import type { HrJobGradeApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createJobGrade, updateJobGrade } from '#/api';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emits = defineEmits(['success']);

const formData = ref<HrJobGradeApi.HrJobGrade>();
const id = ref<string | number>();

const [Form, formApi] = useVbenForm({
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues();
    drawerApi.lock();
    (id.value ? updateJobGrade(id.value, values as any) : createJobGrade(values as any))
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
      const data = drawerApi.getData<HrJobGradeApi.HrJobGrade>();
      formApi.resetForm();

      if (data && data.id) {
        formData.value = data;
        id.value = data.id;
      } else {
        formData.value = undefined;
        id.value = undefined;
      }

      await nextTick();
      if (data) {
        formApi.setValues(data);
      }
    }
  },
});

const getDrawerTitle = computed(() => {
  return id.value
    ? $t('hr.jobGrade.edit')
    : $t('hr.jobGrade.create');
});
</script>
<template>
  <Drawer :title="getDrawerTitle">
    <Form />
  </Drawer>
</template>
