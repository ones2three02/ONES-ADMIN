<script lang="ts" setup>
import type { HrContractApi, SystemFileApi } from '#/api';

import { computed, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, message, Upload } from 'antdv-next';

import { useVbenForm } from '#/adapter/form';
import {
  createContract,
  getContractAttachmentMetadata,
  openSystemFile,
  updateContract,
  uploadSystemFile,
} from '#/api';
import { $t } from '#/locales';

import { errorMessageOf, normalizeError } from '../../shared/error';
import { formatFileSize } from '../../shared/file';
import { useFormSchema } from '../data';

const emits = defineEmits(['success']);

const employeeId = ref<string | number>();
const contractId = ref<string | number>();
const isEdit = ref(false);
const attachmentFile = ref<SystemFileApi.FileMetadata>();
const attachmentFileId = ref<string>();
const attachmentLoading = ref(false);
const uploadLoading = ref(false);

type ContractFormValues = Partial<HrContractApi.SaveRequest>;
type AttachmentMetadataContext = {
  attachmentFileId?: string;
  id?: string | number;
};
type UploadRequestOptions = {
  file: Blob | File | string;
  onError?: (error: Error) => void;
  onSuccess?: (data: SystemFileApi.FileMetadata) => void;
};

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
      attachmentFileId: values.attachmentFileId || undefined,
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
      resetAttachment();

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
        await loadAttachmentMetadata(data.contract);
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

function resetAttachment() {
  attachmentFile.value = undefined;
  attachmentFileId.value = undefined;
  attachmentLoading.value = false;
  uploadLoading.value = false;
}

async function loadAttachmentMetadata(contract?: AttachmentMetadataContext) {
  attachmentFile.value = undefined;
  attachmentFileId.value = contract?.attachmentFileId;
  if (!contract?.id || !contract.attachmentFileId) {
    return undefined;
  }
  attachmentLoading.value = true;
  try {
    const metadata = await getContractAttachmentMetadata(contract.id);
    attachmentFile.value = metadata;
    attachmentFileId.value = metadata.id;
    await formApi.setFieldValue('attachmentFileId', metadata.id, false);
    return metadata;
  } catch (error: unknown) {
    message.error(errorMessageOf(error, $t('hr.contract.attachmentLoadError')));
    return undefined;
  } finally {
    attachmentLoading.value = false;
  }
}

async function setAttachment(file: SystemFileApi.FileMetadata) {
  attachmentFile.value = file;
  attachmentFileId.value = file.id;
  await formApi.setFieldValue('attachmentFileId', file.id, false);
}

async function removeAttachment() {
  attachmentFile.value = undefined;
  attachmentFileId.value = undefined;
  await formApi.setFieldValue('attachmentFileId', undefined, false);
}

async function handleCustomUpload(options: UploadRequestOptions) {
  const { file, onError, onSuccess } = options;
  if (!(file instanceof File)) {
    const error = new Error($t('hr.contract.attachmentUploadError'));
    message.error(errorMessageOf(error, $t('hr.contract.attachmentUploadError')));
    onError?.(error);
    return;
  }
  uploadLoading.value = true;
  const hide = message.loading($t('hr.contract.attachmentUploading'), 0);
  try {
    const uploadedFile = await uploadSystemFile(file);
    await setAttachment(uploadedFile);
    message.success($t('hr.contract.attachmentUploadSuccess'));
    onSuccess?.(uploadedFile);
  } catch (error: unknown) {
    const normalizedError = normalizeError(
      error,
      $t('hr.contract.attachmentUploadError'),
    );
    message.error(errorMessageOf(normalizedError, $t('hr.contract.attachmentUploadError')));
    onError?.(normalizedError);
  } finally {
    hide();
    uploadLoading.value = false;
  }
}

async function previewAttachment() {
  const file =
    attachmentFile.value ||
    (await loadAttachmentMetadata(
      contractId.value && attachmentFileId.value
        ? {
            attachmentFileId: attachmentFileId.value,
            id: contractId.value,
          }
        : undefined,
    ));
  if (!file?.storedName) {
    message.warning($t('hr.contract.attachmentOpenUnavailable'));
    return;
  }
  try {
    await openSystemFile(file);
  } catch (error: unknown) {
    message.error(errorMessageOf(error, $t('hr.contract.attachmentOpenError')));
  }
}
</script>
<template>
  <Drawer :title="getDrawerTitle">
    <Form />
    <div class="mt-4 border-t pt-4">
      <div class="mb-3 flex items-center justify-between gap-3">
        <div class="flex items-center gap-2 text-sm font-medium">
          <IconifyIcon class="size-4 text-primary" icon="lucide:paperclip" />
          {{ $t('hr.contract.attachment') }}
        </div>
        <Upload
          :custom-request="handleCustomUpload"
          :show-upload-list="false"
          v-access:code="['system:file:upload']"
        >
          <Button :loading="uploadLoading" size="small">
            <template #icon>
              <IconifyIcon icon="lucide:upload" />
            </template>
            {{
              attachmentFileId
                ? $t('hr.contract.replaceAttachment')
                : $t('hr.contract.uploadAttachment')
            }}
          </Button>
        </Upload>
      </div>

      <div class="rounded border bg-muted/30 p-3">
        <div
          v-if="attachmentFileId"
          class="flex items-center justify-between gap-3"
        >
          <div class="flex min-w-0 items-center gap-2">
            <IconifyIcon
              class="size-5 shrink-0 text-primary"
              icon="lucide:file-text"
            />
            <div class="min-w-0">
              <div class="truncate text-sm font-medium">
                {{
                  attachmentFile?.originalName ||
                  `${$t('hr.contract.attachmentId')} ${attachmentFileId}`
                }}
              </div>
              <div class="text-muted-foreground mt-1 flex items-center gap-2 text-xs">
                <span>{{ attachmentFile?.extension?.toUpperCase() || '-' }}</span>
                <span>{{ formatFileSize(attachmentFile?.sizeBytes) }}</span>
                <span v-if="attachmentFile?.storageType">
                  {{ attachmentFile.storageType }}
                </span>
              </div>
            </div>
          </div>
          <div class="flex shrink-0 items-center gap-1">
            <Button
              :disabled="!attachmentFile?.storedName && !attachmentFileId"
              :loading="attachmentLoading"
              size="small"
              type="link"
              v-access:code="['hr:contract:list']"
              @click="previewAttachment"
            >
              {{ $t('hr.contract.viewAttachment') }}
            </Button>
            <Button danger size="small" type="link" @click="removeAttachment">
              {{ $t('hr.contract.removeAttachment') }}
            </Button>
          </div>
        </div>
        <div
          v-else
          class="text-muted-foreground flex items-center gap-2 text-sm"
        >
          <IconifyIcon class="size-4" icon="lucide:file-plus-2" />
          {{ $t('hr.contract.noAttachment') }}
        </div>
      </div>
    </div>
  </Drawer>
</template>
