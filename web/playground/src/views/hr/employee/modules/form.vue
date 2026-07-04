<script lang="ts" setup>
import { computed, nextTick } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createEmployee } from '#/api'; // 注意：编辑员工并没有update接口吗？
// 等等！后端好像确实没有直接update员工的接口，而是调岗、转正、离职。
// 我们来看一下后端 HrEmployeeController 里是否有编辑普通字段（比如mobile、email、displayName）的接口？
// 确实没有，后端只有新增、调岗、转正、离职。这说明普通字段编辑在后端是没有暴露的，或者是在调岗里更新信息。
// 那我们在这个表单里就只在【新增员工】时调用 createEmployee。
// 如果是编辑，我们需要什么操作吗？如果只是展示或因为后端没写 update 接口，我们可以暂且把编辑作为只读查看，
// 或者在前端暂时只支持【新增员工】的抽屉。
// 为了严谨起见，我们查看一下 HrEmployeeService 里有没有 update 方法？
// 有可能是在 Controller 里没有，也有可能有。不过我们在 HrEmployeeController 里看到确实只有：
// createEmployee, transferEmployee, regularizeEmployee, resignEmployee，没有普通的 updateEmployee。
// 那我们的 drawer 就主要是用作“新增员工”。
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emits = defineEmits(['success']);

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
    createEmployee(values as any)
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
      formApi.resetForm();
      await nextTick();
    }
  },
});

const getDrawerTitle = computed(() => {
  return $t('hr.employee.createEmployee');
});
</script>
<template>
  <Drawer :title="getDrawerTitle">
    <Form />
  </Drawer>
</template>
