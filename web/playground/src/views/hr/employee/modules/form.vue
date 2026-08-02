<script lang="ts" setup>
import type { HrEmployeeApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createEmployee, getEmployee, updateEmployee } from '#/api';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emits = defineEmits(['success']);
const editingEmployee = ref<HrEmployeeApi.HrEmployee>();

type EmployeeFormValues = Partial<
  HrEmployeeApi.CreateRequest & HrEmployeeApi.UpdateRequest
> & {
  sensitiveVisible?: boolean;
};

const [Form, formApi] = useVbenForm({
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues<EmployeeFormValues>();
    drawerApi.lock();
    const submit = editingEmployee.value
      ? updateEmployee(editingEmployee.value.id, buildUpdatePayload(values))
      : createEmployee(buildCreatePayload(values));
    submit
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
      editingEmployee.value = data?.id ? data : undefined;
      formApi.resetForm();
      await nextTick();
      if (editingEmployee.value) {
        editingEmployee.value = await getEmployee(editingEmployee.value.id);
        formApi.setValues({
          ...editingEmployee.value,
          idCardNumber: undefined,
        });
      }
    }
  },
});

const getDrawerTitle = computed(() => {
  return editingEmployee.value
    ? $t('hr.employee.editEmployee')
    : $t('hr.employee.createEmployee');
});

function emptyToUndefined(value: unknown) {
  return value === '' || value === null ? undefined : value;
}

function buildCreatePayload(values: EmployeeFormValues): HrEmployeeApi.CreateRequest {
  return {
    employeeNo: values.employeeNo!,
    realName: values.realName!,
    preferredName: emptyToUndefined(values.preferredName) as string | undefined,
    gender: values.gender!,
    mobile: values.mobile!,
    email: values.email!,
    idCardNumber: emptyToUndefined(values.idCardNumber) as string | undefined,
    userId: emptyToUndefined(values.userId) as string | undefined,
    deptId: values.deptId!,
    positionId: emptyToUndefined(values.positionId) as string | undefined,
    gradeId: emptyToUndefined(values.gradeId) as string | undefined,
    managerEmployeeId: emptyToUndefined(values.managerEmployeeId) as string | undefined,
    employmentType: values.employmentType!,
    employmentStatus: emptyToUndefined(values.employmentStatus) as string | undefined,
    hireDate: values.hireDate!,
    probationEndDate: emptyToUndefined(values.probationEndDate) as string | undefined,
    remark: emptyToUndefined(values.remark) as string | undefined,
  };
}

function buildUpdatePayload(values: EmployeeFormValues): HrEmployeeApi.UpdateRequest {
  const payload: HrEmployeeApi.UpdateRequest = {
    realName: values.realName!,
    preferredName: emptyToUndefined(values.preferredName) as string | undefined,
    gender: emptyToUndefined(values.gender) as string | undefined,
    userId: emptyToUndefined(values.userId) as string | undefined,
    probationEndDate: emptyToUndefined(values.probationEndDate) as string | undefined,
    remark: emptyToUndefined(values.remark) as string | undefined,
  };
  if (values.sensitiveVisible !== false) {
    payload.mobile = emptyToUndefined(values.mobile) as string | undefined;
    payload.email = emptyToUndefined(values.email) as string | undefined;
  }
  const idCardNumber = emptyToUndefined(values.idCardNumber) as string | undefined;
  if (idCardNumber) {
    payload.idCardNumber = idCardNumber;
  }
  return payload;
}
</script>
<template>
  <Drawer :title="getDrawerTitle">
    <Form />
  </Drawer>
</template>
