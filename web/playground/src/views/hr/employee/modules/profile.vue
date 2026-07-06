<script lang="ts" setup>
import type { HrContractApi, HrEmployeeApi, SystemFileApi } from '#/api';

import { computed, ref } from 'vue';

import { useAccess } from '@vben/access';
import { useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Descriptions,
  DescriptionsItem,
  Empty,
  message,
  Modal,
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
  removeEmployeeDocument,
  uploadSystemFile,
} from '#/api';

import {
  formatHrDictLabel,
  getHrDictOptions,
  HR_CONTRACT_STATUS_DICT,
  HR_CONTRACT_TYPE_DICT,
  HR_EMPLOYMENT_STATUS_DICT,
  HR_EMPLOYMENT_TYPE_DICT,
  HR_GENDER_DICT,
} from '../../dict-options';

const employee = ref<HrEmployeeApi.HrEmployee>();
const contracts = ref<HrContractApi.HrContract[]>([]);
const events = ref<HrEmployeeApi.LifecycleEvent[]>([]);
const jobs = ref<HrEmployeeApi.EmployeeJob[]>([]);
const orgContext = ref<HrEmployeeApi.EmployeeOrgContext>();
const documents = ref<SystemFileApi.FileMetadata[]>([]);
const loading = ref(false);
const documentUploadLoading = ref(false);
const { hasAccessByCodes } = useAccess();

const canViewContracts = computed(() => hasAccessByCodes(['hr:contract:list']));
const canViewLifecycle = computed(() =>
  hasAccessByCodes(['hr:employee:lifecycle']),
);
const canManageDocuments = computed(() => hasAccessByCodes(['hr:employee:update']));

type UploadRequestOptions = {
  file: Blob | File | string;
  onError?: (error: Error) => void;
  onSuccess?: (data: SystemFileApi.FileMetadata) => void;
};

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
    ? `员工档案 - ${employee.value.realName}`
    : '员工档案',
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
      label: '员工编号',
      value: current.employeeNo,
    },
    {
      icon: 'lucide:building-2',
      label: '当前部门',
      value: current.deptName || '-',
    },
    {
      icon: 'lucide:briefcase-business',
      label: '岗位',
      value: current.positionName || '-',
    },
    {
      icon: 'lucide:file-check-2',
      label: '有效合同',
      value: activeContract.value?.contractNo || '-',
    },
    {
      icon: 'lucide:git-branch',
      label: '直属下级',
      value: `${orgContext.value?.directReportCount ?? 0}`,
    },
    {
      icon: 'lucide:paperclip',
      label: '资料附件',
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
  return `${empty(job.effectiveDate)} 至 ${empty(job.endDate)}`;
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

function errorMessageOf(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

function normalizeError(error: unknown, fallback: string) {
  return error instanceof Error ? error : new Error(fallback);
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
    const error = new Error('资料附件上传失败');
    message.error(error.message);
    onError?.(error);
    return;
  }
  documentUploadLoading.value = true;
  const hide = message.loading('正在上传资料附件...', 0);
  try {
    const uploadedFile = await uploadSystemFile(file);
    const boundFile = await bindEmployeeDocument(employee.value.id, uploadedFile.id);
    await reloadDocuments();
    message.success('资料附件已上传');
    onSuccess?.(boundFile);
  } catch (error: unknown) {
    const normalizedError = normalizeError(error, '资料附件上传失败');
    message.error(errorMessageOf(normalizedError, '资料附件上传失败'));
    onError?.(normalizedError);
  } finally {
    hide();
    documentUploadLoading.value = false;
  }
}

function openDocument(file: SystemFileApi.FileMetadata) {
  if (!file.url) {
    message.warning('资料附件暂不可访问');
    return;
  }
  window.open(file.url, '_blank', 'noopener,noreferrer');
}

function confirmRemoveDocument(file: SystemFileApi.FileMetadata) {
  if (!employee.value?.id) {
    return;
  }
  Modal.confirm({
    content: `确认移除资料附件「${file.originalName}」吗？`,
    okButtonProps: { danger: true },
    okText: '确认移除',
    title: '移除资料附件',
    async onOk() {
      await removeEmployeeDocument(employee.value!.id, file.id);
      message.success('资料附件已移除');
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
        <TabPane key="basic" tab="基础信息">
          <Card variant="borderless">
            <Descriptions bordered :column="2" size="small">
              <DescriptionsItem label="姓名">{{ employee.realName }}</DescriptionsItem>
              <DescriptionsItem label="常用名">{{ empty(employee.preferredName) }}</DescriptionsItem>
              <DescriptionsItem label="性别">
                {{ dictLabel(HR_GENDER_DICT, employee.gender) }}
              </DescriptionsItem>
              <DescriptionsItem label="用工类型">
                {{ dictLabel(HR_EMPLOYMENT_TYPE_DICT, employee.employmentType) }}
              </DescriptionsItem>
              <DescriptionsItem label="状态">
                <Tag :color="statusColor(employee.employmentStatus)">
                  {{ dictLabel(HR_EMPLOYMENT_STATUS_DICT, employee.employmentStatus) }}
                </Tag>
              </DescriptionsItem>
              <DescriptionsItem label="入职日期">{{ empty(employee.hireDate) }}</DescriptionsItem>
              <DescriptionsItem label="试用期结束">{{ empty(employee.probationEndDate) }}</DescriptionsItem>
              <DescriptionsItem label="离职日期">{{ empty(employee.leaveDate) }}</DescriptionsItem>
              <DescriptionsItem label="手机号">{{ empty(employee.mobile) }}</DescriptionsItem>
              <DescriptionsItem label="邮箱">{{ empty(employee.email) }}</DescriptionsItem>
              <DescriptionsItem label="证件号">{{ empty(employee.idCardMasked) }}</DescriptionsItem>
              <DescriptionsItem label="直属上级">{{ empty(employee.managerName) }}</DescriptionsItem>
              <DescriptionsItem label="备注" :span="2">
                {{ empty(employee.remark) }}
              </DescriptionsItem>
            </Descriptions>
          </Card>
        </TabPane>

        <TabPane key="contracts" tab="合同">
          <Empty
            v-if="!canViewContracts"
            description="暂无合同查看权限"
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
                    · {{ item.startDate }} 至 {{ empty(item.endDate) }}
                  </div>
                </div>
                <div class="text-muted-foreground text-sm">
                  续签提醒：{{ empty(item.renewalRemindDate) }}
                </div>
              </div>
            </Card>
          </div>
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </TabPane>

        <TabPane key="documents" tab="资料附件">
          <div class="flex flex-col gap-3">
            <div class="flex items-center justify-between gap-3">
              <div class="text-muted-foreground text-sm">
                共 {{ documents.length }} 个附件
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
                  上传资料
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
                        <Tag v-if="file.extension" color="blue">
                          {{ file.extension.toUpperCase() }}
                        </Tag>
                        <span>{{ formatFileSize(file.sizeBytes) }}</span>
                        <span v-if="file.storageType">{{ file.storageType }}</span>
                        <span>{{ empty(file.createdAt) }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="flex shrink-0 items-center gap-1">
                    <Button size="small" type="link" @click="openDocument(file)">
                      查看
                    </Button>
                    <Button
                      v-if="canManageDocuments"
                      danger
                      size="small"
                      type="link"
                      @click="confirmRemoveDocument(file)"
                    >
                      移除
                    </Button>
                  </div>
                </div>
              </Card>
            </div>
            <Empty v-else description="暂无资料附件" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
        </TabPane>

        <TabPane key="jobs" tab="任职记录">
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
                        {{ job.endDate ? '历史任职' : '当前任职' }}
                      </Tag>
                    </div>
                    <span class="text-muted-foreground text-xs">
                      {{ jobRange(job) }}
                    </span>
                  </div>
                  <Descriptions bordered :column="2" size="small">
                    <DescriptionsItem label="部门">
                      {{ empty(job.deptName) }}
                    </DescriptionsItem>
                    <DescriptionsItem label="职级">
                      {{ empty(job.gradeName) }}
                    </DescriptionsItem>
                    <DescriptionsItem label="直属上级">
                      {{ empty(job.managerName) }}
                    </DescriptionsItem>
                    <DescriptionsItem label="用工类型">
                      {{ dictLabel(HR_EMPLOYMENT_TYPE_DICT, job.employmentType) }}
                    </DescriptionsItem>
                    <DescriptionsItem label="变动原因" :span="2">
                      {{ empty(job.changeReason) }}
                    </DescriptionsItem>
                  </Descriptions>
                </div>
              </Card>
            </TimelineItem>
          </Timeline>
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </TabPane>

        <TabPane key="organization" tab="组织关系">
          <div v-if="orgContext" class="flex flex-col gap-3">
            <Card variant="borderless">
              <Descriptions bordered :column="2" size="small">
                <DescriptionsItem label="组织路径" :span="2">
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
                <DescriptionsItem label="当前员工">
                  {{ orgNodeTitle(orgContext.current) }}
                </DescriptionsItem>
                <DescriptionsItem label="当前岗位">
                  {{ orgNodeMeta(orgContext.current) }}
                </DescriptionsItem>
                <DescriptionsItem label="直属上级">
                  {{ orgNodeTitle(orgContext.manager) }}
                </DescriptionsItem>
                <DescriptionsItem label="上级岗位">
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
            <Empty v-else description="暂无可见直属下级" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </TabPane>

        <TabPane key="lifecycle" tab="生命周期">
          <Empty
            v-if="!canViewLifecycle"
            description="暂无生命周期查看权限"
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
                  {{ event.eventDate }} · 操作人：{{ empty(event.createdByName) }}
                </span>
              </div>
            </TimelineItem>
          </Timeline>
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </TabPane>
      </Tabs>
    </div>
  </Drawer>
</template>
