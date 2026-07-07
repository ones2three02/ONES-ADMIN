<script lang="ts" setup>
import type { HrContractApi, HrEmployeeApi } from '#/api';

import { computed, ref } from 'vue';

import { useAccess } from '@vben/access';
import { useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Empty,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Select,
  SelectOption,
  Skeleton,
  TabPane,
  Tabs,
  Tag,
  Timeline,
  TimelineItem,
  Upload,
} from 'antdv-next';

import {
  bindEmployeeDocument,
  getEmployee,
  getEmployeeContracts,
  getEmployeeDocuments,
  getEmployeeJobs,
  getEmployeeLifecycleEvents,
  getEmployeeOrgContext,
  openSystemFile,
  removeEmployeeDocument,
  uploadSystemFile,
} from '#/api';
import { $t } from '#/locales';

import {
  formatHrDictLabel,
  getHrDictOptions,
  HR_CONTRACT_STATUS_DICT,
  HR_CONTRACT_TYPE_DICT,
  HR_EMPLOYEE_DOCUMENT_TYPE_DICT,
  HR_EMPLOYMENT_STATUS_DICT,
  HR_EMPLOYMENT_TYPE_DICT,
  HR_GENDER_DICT,
  getHrDictFallbackOptions,
  getHrDictOption,
} from '../../dict-options';
import { errorMessageOf, normalizeError } from '../../shared/error';

const employee = ref<HrEmployeeApi.HrEmployee>();
const contracts = ref<HrContractApi.HrContract[]>([]);
const events = ref<HrEmployeeApi.LifecycleEvent[]>([]);
const jobs = ref<HrEmployeeApi.EmployeeJob[]>([]);
const orgContext = ref<HrEmployeeApi.EmployeeOrgContext>();
const documents = ref<HrEmployeeApi.EmployeeDocument[]>([]);
const loading = ref(false);
const documentUploadLoading = ref(false);
const pendingDocumentFile = ref<File>();
const documentMetaOpen = ref(false);
const documentMeta = ref<HrEmployeeApi.EmployeeDocumentBindRequest>({
  documentType: 'OTHER',
});
const { hasAccessByCodes } = useAccess();

const canViewContracts = computed(() => hasAccessByCodes(['hr:contract:list']));
const canViewLifecycle = computed(() =>
  hasAccessByCodes(['hr:employee:lifecycle']),
);
const canManageDocuments = computed(() => hasAccessByCodes(['hr:employee:update']));

type UploadRequestOptions = {
  file: Blob | File | string;
  onError?: (error: Error) => void;
  onSuccess?: (data: unknown) => void;
};

const documentTypeOptions = computed(() =>
  getHrDictFallbackOptions(HR_EMPLOYEE_DOCUMENT_TYPE_DICT),
);

const [Drawer, drawerApi] = useVbenDrawer({
  async onOpenChange(isOpen) {
    if (!isOpen) {
      return;
    }
    const row = drawerApi.getData<HrEmployeeApi.HrEmployee>();
    if (!row?.id) {
      return;
    }
    loading.value = true;
    employee.value = undefined;
    contracts.value = [];
    events.value = [];
    jobs.value = [];
    orgContext.value = undefined;
    documents.value = [];
    try {
      await Promise.all([
        getHrDictOptions(HR_GENDER_DICT),
        getHrDictOptions(HR_EMPLOYMENT_TYPE_DICT),
        getHrDictOptions(HR_EMPLOYMENT_STATUS_DICT),
        getHrDictOptions(HR_CONTRACT_TYPE_DICT),
        getHrDictOptions(HR_CONTRACT_STATUS_DICT),
        getHrDictOptions(HR_EMPLOYEE_DOCUMENT_TYPE_DICT),
      ]);
      const [
        detail,
        contractRows,
        lifecycleRows,
        jobRows,
        organization,
        documentRows,
      ] = await Promise.all([
        getEmployee(row.id),
        canViewContracts.value ? getEmployeeContracts(row.id) : Promise.resolve([]),
        canViewLifecycle.value
          ? getEmployeeLifecycleEvents(row.id)
          : Promise.resolve([]),
        getEmployeeJobs(row.id),
        getEmployeeOrgContext(row.id),
        getEmployeeDocuments(row.id),
      ]);
      employee.value = detail;
      contracts.value = contractRows;
      events.value = lifecycleRows;
      jobs.value = jobRows;
      orgContext.value = organization;
      documents.value = documentRows;
    } finally {
      loading.value = false;
    }
  },
});

const title = computed(() =>
  employee.value
    ? $t('hr.employeeProfile.employeeTitle', { name: employee.value.realName })
    : $t('hr.employeeProfile.title'),
);

const activeContract = computed(() =>
  contracts.value.find((item) => item.status === 'ACTIVE'),
);

const summaryItems = computed(() => {
  const current = employee.value;
  if (!current) {
    return [];
  }
  return [
    {
      icon: 'lucide:user-round',
      label: $t('hr.employeeProfile.summaryEmployeeNo'),
      value: current.employeeNo,
    },
    {
      icon: 'lucide:building-2',
      label: $t('hr.employeeProfile.summaryCurrentDept'),
      value: current.deptName || '-',
    },
    {
      icon: 'lucide:briefcase-business',
      label: $t('hr.employeeProfile.summaryPosition'),
      value: current.positionName || '-',
    },
    {
      icon: 'lucide:file-check-2',
      label: $t('hr.employeeProfile.summaryActiveContract'),
      value: activeContract.value?.contractNo || '-',
    },
    {
      icon: 'lucide:git-branch',
      label: $t('hr.employeeProfile.summaryDirectReports'),
      value: `${orgContext.value?.directReportCount ?? 0}`,
    },
    {
      icon: 'lucide:paperclip',
      label: $t('hr.employeeProfile.summaryDocuments'),
      value: `${documents.value.length}`,
    },
  ];
});

function empty(value?: null | number | string) {
  return value === undefined || value === null || value === '' ? '-' : value;
}

function dictLabel(dictCode: string, value?: string) {
  return empty(formatHrDictLabel(dictCode, value));
}

function dictColor(dictCode: string, value?: string) {
  return getHrDictOption(dictCode, value)?.color || 'default';
}

function statusColor(status?: string) {
  switch (status) {
    case 'ACTIVE': {
      return 'success';
    }
    case 'PROBATION':
    case 'EXPIRING': {
      return 'warning';
    }
    case 'RESIGNED':
    case 'TERMINATED': {
      return 'error';
    }
    default: {
      return 'default';
    }
  }
}

function lifecycleColor(type: string) {
  switch (type) {
    case 'ONBOARD': {
      return 'green';
    }
    case 'REGULARIZE': {
      return 'blue';
    }
    case 'TRANSFER': {
      return 'orange';
    }
    case 'RESIGN': {
      return 'red';
    }
    default: {
      return 'gray';
    }
  }
}

function jobRange(job: HrEmployeeApi.EmployeeJob) {
  return $t('hr.employeeProfile.dateRange', {
    end: empty(job.endDate),
    start: empty(job.effectiveDate),
  });
}

function orgNodeTitle(node?: HrEmployeeApi.OrgEmployeeNode) {
  if (!node) {
    return '-';
  }
  return `${node.realName}（${node.employeeNo}）`;
}

function orgNodeMeta(node?: HrEmployeeApi.OrgEmployeeNode) {
  if (!node) {
    return '-';
  }
  return [
    node.deptName,
    node.positionName,
    node.gradeName,
  ].filter(Boolean).join(' / ') || '-';
}

function formatFileSize(size?: number) {
  if (size === undefined || size === null) {
    return '-';
  }
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

async function reloadDocuments() {
  if (!employee.value?.id) {
    documents.value = [];
    return;
  }
  documents.value = await getEmployeeDocuments(employee.value.id);
}

async function handleDocumentUpload(options: UploadRequestOptions) {
  const { file, onError, onSuccess } = options;
  if (!(file instanceof File) || !employee.value?.id) {
    const error = new Error($t('hr.employeeProfile.documentUploadError'));
    message.error(error.message);
    onError?.(error);
    return;
  }
  pendingDocumentFile.value = file;
  documentMeta.value = {
    documentType: 'OTHER',
  };
  documentMetaOpen.value = true;
  onSuccess?.({});
}

async function submitDocumentUpload() {
  if (!pendingDocumentFile.value || !employee.value?.id) {
    documentMetaOpen.value = false;
    return;
  }
  documentUploadLoading.value = true;
  const hide = message.loading($t('hr.employeeProfile.documentUploading'), 0);
  try {
    const uploadedFile = await uploadSystemFile(pendingDocumentFile.value);
    await bindEmployeeDocument(employee.value.id, uploadedFile.id, documentMeta.value);
    await reloadDocuments();
    message.success($t('hr.employeeProfile.documentUploadSuccess'));
  } catch (error: unknown) {
    const normalizedError = normalizeError(
      error,
      $t('hr.employeeProfile.documentUploadError'),
    );
    message.error(
      errorMessageOf(normalizedError, $t('hr.employeeProfile.documentUploadError')),
    );
  } finally {
    hide();
    documentUploadLoading.value = false;
    documentMetaOpen.value = false;
    pendingDocumentFile.value = undefined;
  }
}

function cancelDocumentUpload() {
  documentMetaOpen.value = false;
  pendingDocumentFile.value = undefined;
  documentMeta.value = {
    documentType: 'OTHER',
  };
}

async function openDocument(file: HrEmployeeApi.EmployeeDocument) {
  if (!file.storedName) {
    message.warning($t('hr.employeeProfile.documentUnavailable'));
    return;
  }
  try {
    await openSystemFile(file);
  } catch (error: unknown) {
    message.error(
      errorMessageOf(error, $t('hr.employeeProfile.documentOpenError')),
    );
  }
}

function documentStatusColor(file: HrEmployeeApi.EmployeeDocument) {
  if (file.expired) {
    return 'error';
  }
  if (file.expiringSoon) {
    return 'warning';
  }
  return 'success';
}

function documentStatusText(file: HrEmployeeApi.EmployeeDocument) {
  if (file.expired) {
    return $t('hr.employeeProfile.documentExpired');
  }
  if (file.expiringSoon) {
    return $t('hr.employeeProfile.documentExpiringSoon');
  }
  return file.expireDate
    ? $t('hr.employeeProfile.documentValid')
    : $t('hr.employeeProfile.documentLongTermValid');
}

function confirmRemoveDocument(file: HrEmployeeApi.EmployeeDocument) {
  if (!employee.value?.id) {
    return;
  }
  Modal.confirm({
    content: $t('hr.employeeProfile.documentRemoveConfirm', {
      name: file.originalName,
    }),
    okButtonProps: { danger: true },
    okText: $t('hr.employeeProfile.documentRemoveOk'),
    title: $t('hr.employeeProfile.documentRemoveTitle'),
    async onOk() {
      await removeEmployeeDocument(employee.value!.id, file.id);
      message.success($t('hr.employeeProfile.documentRemoveSuccess'));
      await reloadDocuments();
    },
  });
}
</script>

<template>
  <Drawer :title="title" :footer="false" class="hr-employee-profile-drawer">
    <Skeleton v-if="loading && !employee" active />
    <div v-else-if="employee" class="flex flex-col gap-4 p-1">
      <div class="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
        <Card v-for="item in summaryItems" :key="item.label" variant="borderless">
          <div class="flex items-start justify-between gap-3">
            <div>
              <div class="text-muted-foreground text-xs">{{ item.label }}</div>
              <div class="mt-1 truncate text-base font-medium">{{ item.value }}</div>
            </div>
            <div class="rounded bg-muted p-2 text-primary">
              <IconifyIcon :icon="item.icon" class="size-5" />
            </div>
          </div>
        </Card>
      </div>

      <Tabs>
        <TabPane key="basic" :tab="$t('hr.employeeProfile.tabBasic')">
          <Card variant="borderless">
            <Descriptions bordered :column="2" size="small">
              <DescriptionsItem :label="$t('hr.employee.realName')">{{ employee.realName }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.preferredName')">{{ empty(employee.preferredName) }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.gender')">
                {{ dictLabel(HR_GENDER_DICT, employee.gender) }}
              </DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.employmentType')">
                {{ dictLabel(HR_EMPLOYMENT_TYPE_DICT, employee.employmentType) }}
              </DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.employmentStatus')">
                <Tag :color="statusColor(employee.employmentStatus)">
                  {{ dictLabel(HR_EMPLOYMENT_STATUS_DICT, employee.employmentStatus) }}
                </Tag>
              </DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.hireDate')">{{ empty(employee.hireDate) }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.probationEndDate')">{{ empty(employee.probationEndDate) }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.leaveDate')">{{ empty(employee.leaveDate) }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.mobile')">{{ empty(employee.mobile) }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.email')">{{ empty(employee.email) }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.idCard')">{{ empty(employee.idCardMasked) }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.managerName')">{{ empty(employee.managerName) }}</DescriptionsItem>
              <DescriptionsItem :label="$t('hr.employee.remark')" :span="2">
                {{ empty(employee.remark) }}
              </DescriptionsItem>
            </Descriptions>
          </Card>
        </TabPane>

        <TabPane key="contracts" :tab="$t('hr.employeeProfile.tabContracts')">
          <Empty
            v-if="!canViewContracts"
            :description="$t('hr.employeeProfile.noContractPermission')"
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
          />
          <div v-else-if="contracts.length" class="flex flex-col gap-3">
            <Card v-for="item in contracts" :key="item.id" variant="borderless">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-medium">{{ item.contractNo }}</span>
                    <Tag :color="statusColor(item.status)">
                      {{ dictLabel(HR_CONTRACT_STATUS_DICT, item.status) }}
                    </Tag>
                  </div>
                  <div class="text-muted-foreground mt-2 text-sm">
                    {{ dictLabel(HR_CONTRACT_TYPE_DICT, item.contractType) }}
                    ·
                    {{
                      $t('hr.employeeProfile.dateRange', {
                        end: empty(item.endDate),
                        start: item.startDate,
                      })
                    }}
                  </div>
                </div>
                <div class="text-muted-foreground text-sm">
                  {{
                    $t('hr.employeeProfile.renewalReminder', {
                      date: empty(item.renewalRemindDate),
                    })
                  }}
                </div>
              </div>
            </Card>
          </div>
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </TabPane>

        <TabPane key="documents" :tab="$t('hr.employeeProfile.tabDocuments')">
          <div class="flex flex-col gap-3">
            <div class="flex items-center justify-between gap-3">
              <div class="text-muted-foreground text-sm">
                {{ $t('hr.employeeProfile.documentCount', { count: documents.length }) }}
              </div>
              <Upload
                :custom-request="handleDocumentUpload"
                :show-upload-list="false"
                v-access:code="['system:file:upload']"
              >
                <Button
                  :disabled="!canManageDocuments"
                  :loading="documentUploadLoading"
                  size="small"
                  type="primary"
                >
                  <template #icon>
                    <IconifyIcon icon="lucide:upload" />
                  </template>
                  {{ $t('hr.employeeProfile.uploadDocument') }}
                </Button>
              </Upload>
            </div>

            <div v-if="documents.length" class="flex flex-col gap-3">
              <Card v-for="file in documents" :key="file.id" variant="borderless">
                <div class="flex items-center justify-between gap-3">
                  <div class="flex min-w-0 items-center gap-3">
                    <div class="rounded bg-muted p-2 text-primary">
                      <IconifyIcon class="size-5" icon="lucide:file-text" />
                    </div>
                    <div class="min-w-0">
                      <div class="truncate text-sm font-medium">
                        {{ file.originalName }}
                      </div>
                      <div class="text-muted-foreground mt-1 flex flex-wrap items-center gap-2 text-xs">
                        <Tag :color="dictColor(HR_EMPLOYEE_DOCUMENT_TYPE_DICT, file.documentType)">
                          {{ dictLabel(HR_EMPLOYEE_DOCUMENT_TYPE_DICT, file.documentType) }}
                        </Tag>
                        <Tag :color="documentStatusColor(file)">
                          {{ documentStatusText(file) }}
                        </Tag>
                        <Tag v-if="file.extension" color="blue">
                          {{ file.extension.toUpperCase() }}
                        </Tag>
                        <span>{{ formatFileSize(file.sizeBytes) }}</span>
                        <span v-if="file.storageType">{{ file.storageType }}</span>
                        <span>
                          {{
                            $t('hr.employeeProfile.issueDateDisplay', {
                              date: empty(file.issueDate),
                            })
                          }}
                        </span>
                        <span>
                          {{
                            $t('hr.employeeProfile.expireDateDisplay', {
                              date: empty(file.expireDate),
                            })
                          }}
                        </span>
                      </div>
                      <div
                        v-if="file.remark"
                        class="text-muted-foreground mt-1 truncate text-xs"
                      >
                        {{ file.remark }}
                      </div>
                    </div>
                  </div>
                  <div class="flex shrink-0 items-center gap-1">
                    <Button size="small" type="link" @click="openDocument(file)">
                      {{ $t('hr.employeeProfile.viewDocument') }}
                    </Button>
                    <Button
                      v-if="canManageDocuments"
                      danger
                      size="small"
                      type="link"
                      @click="confirmRemoveDocument(file)"
                    >
                      {{ $t('hr.employeeProfile.removeDocument') }}
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
            <Empty
              v-else
              :description="$t('hr.employeeProfile.noDocuments')"
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
            />
          </div>
        </TabPane>

        <TabPane key="jobs" :tab="$t('hr.employeeProfile.tabJobs')">
          <Timeline v-if="jobs.length" class="mt-2">
            <TimelineItem
              v-for="job in jobs"
              :key="job.id"
              :color="job.endDate ? 'gray' : 'green'"
            >
              <Card variant="borderless">
                <div class="flex flex-col gap-3">
                  <div class="flex flex-wrap items-center justify-between gap-2">
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="font-medium">{{ empty(job.positionName) }}</span>
                      <Tag :color="job.endDate ? 'default' : 'success'">
                        {{
                          job.endDate
                            ? $t('hr.employeeProfile.historicalJob')
                            : $t('hr.employeeProfile.currentJob')
                        }}
                      </Tag>
                    </div>
                    <span class="text-muted-foreground text-xs">
                      {{ jobRange(job) }}
                    </span>
                  </div>
                  <Descriptions bordered :column="2" size="small">
                    <DescriptionsItem :label="$t('hr.employee.deptName')">
                      {{ empty(job.deptName) }}
                    </DescriptionsItem>
                    <DescriptionsItem :label="$t('hr.employee.gradeName')">
                      {{ empty(job.gradeName) }}
                    </DescriptionsItem>
                    <DescriptionsItem :label="$t('hr.employee.managerName')">
                      {{ empty(job.managerName) }}
                    </DescriptionsItem>
                    <DescriptionsItem :label="$t('hr.employee.employmentType')">
                      {{ dictLabel(HR_EMPLOYMENT_TYPE_DICT, job.employmentType) }}
                    </DescriptionsItem>
                    <DescriptionsItem :label="$t('hr.employeeProfile.changeReason')" :span="2">
                      {{ empty(job.changeReason) }}
                    </DescriptionsItem>
                  </Descriptions>
                </div>
              </Card>
            </TimelineItem>
          </Timeline>
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </TabPane>

        <TabPane key="organization" :tab="$t('hr.employeeProfile.tabOrganization')">
          <div v-if="orgContext" class="flex flex-col gap-3">
            <Card variant="borderless">
              <Descriptions bordered :column="2" size="small">
                <DescriptionsItem :label="$t('hr.employeeProfile.orgPath')" :span="2">
                  <div class="flex flex-wrap gap-2">
                    <Tag
                      v-for="dept in orgContext.deptPath"
                      :key="dept.id"
                      color="blue"
                    >
                      {{ dept.name }}
                    </Tag>
                    <span v-if="!orgContext.deptPath.length">-</span>
                  </div>
                </DescriptionsItem>
                <DescriptionsItem :label="$t('hr.employeeProfile.currentEmployee')">
                  {{ orgNodeTitle(orgContext.current) }}
                </DescriptionsItem>
                <DescriptionsItem :label="$t('hr.employeeProfile.currentPosition')">
                  {{ orgNodeMeta(orgContext.current) }}
                </DescriptionsItem>
                <DescriptionsItem :label="$t('hr.employee.managerName')">
                  {{ orgNodeTitle(orgContext.manager) }}
                </DescriptionsItem>
                <DescriptionsItem :label="$t('hr.employeeProfile.managerPosition')">
                  {{ orgNodeMeta(orgContext.manager) }}
                </DescriptionsItem>
              </Descriptions>
            </Card>

            <div v-if="orgContext.directReports.length" class="grid gap-3 md:grid-cols-2">
              <Card
                v-for="report in orgContext.directReports"
                :key="report.id"
                variant="borderless"
              >
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <div class="truncate font-medium">
                      {{ orgNodeTitle(report) }}
                    </div>
                    <div class="text-muted-foreground mt-1 truncate text-xs">
                      {{ orgNodeMeta(report) }}
                    </div>
                  </div>
                  <Tag :color="statusColor(report.employmentStatus)">
                    {{ dictLabel(HR_EMPLOYMENT_STATUS_DICT, report.employmentStatus) }}
                  </Tag>
                </div>
              </Card>
            </div>
            <Empty
              v-else
              :description="$t('hr.employeeProfile.noDirectReports')"
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
            />
          </div>
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </TabPane>

        <TabPane key="lifecycle" :tab="$t('hr.employeeProfile.tabLifecycle')">
          <Empty
            v-if="!canViewLifecycle"
            :description="$t('hr.employeeProfile.noLifecyclePermission')"
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
          />
          <Timeline v-else-if="events.length" class="mt-2">
            <TimelineItem
              v-for="event in events"
              :key="event.id"
              :color="lifecycleColor(event.eventType)"
            >
              <div class="flex flex-col gap-1">
                <span class="font-medium">{{ event.summary }}</span>
                <span class="text-muted-foreground text-xs">
                  {{
                    $t('hr.employeeProfile.lifecycleOperator', {
                      date: event.eventDate,
                      operator: empty(event.createdByName),
                    })
                  }}
                </span>
              </div>
            </TimelineItem>
          </Timeline>
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </TabPane>
      </Tabs>
    </div>
    <Modal
      v-model:open="documentMetaOpen"
      :confirm-loading="documentUploadLoading"
      :title="$t('hr.employeeProfile.documentMetaTitle')"
      @cancel="cancelDocumentUpload"
      @ok="submitDocumentUpload"
    >
      <Form layout="vertical">
        <FormItem :label="$t('hr.employeeProfile.documentType')">
          <Select v-model:value="documentMeta.documentType">
            <SelectOption
              v-for="option in documentTypeOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </SelectOption>
          </Select>
        </FormItem>
        <div class="grid gap-3 md:grid-cols-2">
          <FormItem :label="$t('hr.employeeProfile.issueDate')">
            <DatePicker
              v-model:value="documentMeta.issueDate"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
          </FormItem>
          <FormItem :label="$t('hr.employeeProfile.expireDate')">
            <DatePicker
              v-model:value="documentMeta.expireDate"
              class="w-full"
              value-format="YYYY-MM-DD"
            />
          </FormItem>
        </div>
        <FormItem :label="$t('hr.employee.remark')">
          <Input.TextArea
            v-model:value="documentMeta.remark"
            :maxlength="500"
            :rows="3"
            show-count
          />
        </FormItem>
      </Form>
    </Modal>
  </Drawer>
</template>
