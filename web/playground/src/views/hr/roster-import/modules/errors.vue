<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getRosterImportErrors } from '#/api';
import { $t } from '#/locales';

import { useErrorColumns } from '../data';

const batchId = ref<string | number>();

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useErrorColumns(),
    height: 400,
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async () => {
          if (!batchId.value) return [];
          return await getRosterImportErrors(batchId.value);
        },
      },
    },
    rowConfig: {
      keyField: 'id',
    },
  },
});

const [Modal, modalApi] = useVbenModal({
  onCancel() {
    modalApi.close();
  },
  async onOpenChange(isOpen) {
    if (isOpen) {
      const data = modalApi.getData<{ id: string | number }>();
      if (data) {
        batchId.value = data.id;
        gridApi.query();
      }
    }
  },
});
</script>
<template>
  <Modal :title="$t('hr.rosterImport.errors')" :footer="false" width="800px">
    <Grid />
  </Modal>
</template>
