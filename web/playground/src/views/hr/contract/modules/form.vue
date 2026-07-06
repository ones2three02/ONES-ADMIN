<script lang="ts" setup>
import type { HrContractApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { useVbenForm } from '#/adapter/form';
import { createContract, updateContract } from '#/api';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emits = defineEmits(['success']);

const employeeId = ref<string | number>();
const contractId = ref<string | number>();
const isEdit = ref(false);

type ContractFormValues = Partial<HrContractApi.SaveRequest>;

const [Form, formApi] = useVbenForm({
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as ContractFormValues;
    if (!employeeId.value) return;

    const payload: HrContractApi.SaveRequest = {
      ...(values as HrContractApi.SaveRequest),
      status: dataStatus(values, isEdit.value),
    };

    drawerApi.lock();
    (isEdit.value && contractId.value
      ? updateContract(employeeId.value, contractId.value, payload)
      : createContract(employeeId.value, payload))
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
      const data = drawerApi.getData<{
        employeeId: string | number;
        contract?: HrContractApi.HrContract;
      }>();
      formApi.resetForm();

      if (data) {
        employeeId.value = data.employeeId;
        if (data.contract) {
          isEdit.value = true;
          contractId.value = data.contract.id;
        } else {
          isEdit.value = false;
          contractId.value = undefined;
        }
      }

      await nextTick();
      if (data && data.contract) {
        formApi.setValues(data.contract);
      }
    }
  },
});

const getDrawerTitle = computed(() => {
  return isEdit.value
    ? $t('hr.contract.edit')
    : $t('hr.contract.create');
});

function dataStatus(values: ContractFormValues, editing: boolean) {
  return editing && values.status ? values.status : 'ACTIVE';
}
</script>
<template>
  <Drawer :title="getDrawerTitle">
    <Form />
  </Drawer>
</template>
