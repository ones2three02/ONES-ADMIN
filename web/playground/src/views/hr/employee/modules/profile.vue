<script lang="ts" setup>
import type { HrContractApi, HrEmployeeApi } from '#/api';

import { computed, ref } from 'vue';

import { useAccess } from '@vben/access';
import { useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Card,
  Descriptions,
  DescriptionsItem,
  Empty,
  Skeleton,
  TabPane,
  Tabs,
  Tag,
  Timeline,
  TimelineItem,
} from 'antdv-next';

import {
  getEmployee,
  getEmployeeContracts,
  getEmployeeJobs,
  getEmployeeLifecycleEvents,
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
const loading = ref(false);
const { hasAccessByCodes } = useAccess();

const canViewContracts = computed(() => hasAccessByCodes(['hr:contract:list']));
const canViewLifecycle = computed(() =>
  hasAccessByCodes(['hr:employee:lifecycle']),
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
    try {
      await Promise.all([
        getHrDictOptions(HR_GENDER_DICT),
        getHrDictOptions(HR_EMPLOYMENT_TYPE_DICT),
        getHrDictOptions(HR_EMPLOYMENT_STATUS_DICT),
        getHrDictOptions(HR_CONTRACT_TYPE_DICT),
        getHrDictOptions(HR_CONTRACT_STATUS_DICT),
      ]);
      const [detail, contractRows, lifecycleRows, jobRows] = await Promise.all([
        getEmployee(row.id),
        canViewContracts.value ? getEmployeeContracts(row.id) : Promise.resolve([]),
        canViewLifecycle.value
          ? getEmployeeLifecycleEvents(row.id)
          : Promise.resolve([]),
        getEmployeeJobs(row.id),
      ]);
      employee.value = detail;
      contracts.value = contractRows;
      events.value = lifecycleRows;
      jobs.value = jobRows;
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
</script>

<template>
  <Drawer :title="title" :footer="false" class="hr-employee-profile-drawer">
    <Skeleton v-if="loading && !employee" active />
    <div v-else-if="employee" class="flex flex-col gap-4 p-1">
      <div class="grid gap-3 md:grid-cols-4">
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
