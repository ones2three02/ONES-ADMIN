import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { HrEmployeeApi } from '#/api';

import { h } from 'vue';

import { Tag } from 'antdv-next';

import { $t } from '#/locales';

import { formatHrDictLabel, HR_EMPLOYEE_DOCUMENT_TYPE_DICT } from '../dict-options';

const DAY_MS = 24 * 60 * 60 * 1000;

function startOfToday() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

export function daysUntil(expireDate?: string) {
  if (!expireDate) {
    return undefined;
  }
  const target = new Date(`${expireDate}T00:00:00`);
  if (Number.isNaN(target.getTime())) {
    return undefined;
  }
  return Math.round((target.getTime() - startOfToday().getTime()) / DAY_MS);
}

function riskTag(row: HrEmployeeApi.EmployeeDocument) {
  const remainDays = daysUntil(row.expireDate);
  if (row.expired || (remainDays !== undefined && remainDays < 0)) {
    return h(Tag, { color: 'error' }, () => $t('hr.documentWarning.expired'));
  }
  if (remainDays === 0) {
    return h(Tag, { color: 'error' }, () => $t('hr.documentWarning.dueToday'));
  }
  if (remainDays !== undefined && remainDays <= 7) {
    return h(Tag, { color: 'warning' }, () =>
      $t('hr.documentWarning.remainingDays', { days: remainDays }),
    );
  }
  return h(Tag, { color: 'processing' }, () =>
    remainDays === undefined
      ? $t('hr.documentWarning.expiringSoon')
      : $t('hr.documentWarning.remainingDays', { days: remainDays }),
  );
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: false,
        options: [
          { label: $t('hr.documentWarning.days7'), value: 7 },
          { label: $t('hr.documentWarning.days30'), value: 30 },
          { label: $t('hr.documentWarning.days60'), value: 60 },
          { label: $t('hr.documentWarning.days90'), value: 90 },
        ],
      },
      defaultValue: 30,
      fieldName: 'days',
      label: $t('hr.documentWarning.warningWindow'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns {
  return [
    {
      field: 'employeeNo',
      title: $t('hr.employee.employeeNo'),
      width: 120,
    },
    {
      field: 'realName',
      title: $t('hr.employee.realName'),
      width: 120,
    },
    {
      field: 'deptName',
      minWidth: 140,
      title: $t('hr.employee.deptName'),
    },
    {
      field: 'documentType',
      formatter: ({ cellValue }) =>
        formatHrDictLabel(HR_EMPLOYEE_DOCUMENT_TYPE_DICT, cellValue),
      title: $t('hr.documentWarning.documentType'),
      width: 140,
    },
    {
      field: 'originalName',
      minWidth: 220,
      title: $t('hr.documentWarning.fileName'),
    },
    {
      field: 'issueDate',
      title: $t('hr.documentWarning.issueDate'),
      width: 120,
    },
    {
      field: 'expireDate',
      title: $t('hr.documentWarning.expireDate'),
      width: 120,
    },
    {
      field: 'riskStatus',
      slots: {
        default: ({ row }) => riskTag(row),
      },
      title: $t('hr.documentWarning.riskStatus'),
      width: 130,
    },
    {
      align: 'center',
      field: 'action',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('common.action'),
      width: 130,
    },
  ];
}
