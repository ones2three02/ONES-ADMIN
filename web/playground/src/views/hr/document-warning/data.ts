import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { HrEmployeeApi } from '#/api';

import { $t } from '#/locales';

import { formatHrDictLabel, HR_EMPLOYEE_DOCUMENT_TYPE_DICT } from '../dict-options';
import {
  daysUntil,
  renderDueRiskTag,
  useWarningWindowSchema,
} from '../shared/warning';

export { daysUntil };

function riskTag(row: HrEmployeeApi.EmployeeDocument) {
  return renderDueRiskTag({
    date: row.expireDate,
    dueTodayText: $t('hr.documentWarning.dueToday'),
    expired: row.expired,
    expiredText: $t('hr.documentWarning.expired'),
    expiringSoonText: $t('hr.documentWarning.expiringSoon'),
    remainingText: (days) => $t('hr.documentWarning.remainingDays', { days }),
  });
}

export function useGridFormSchema(): VbenFormSchema[] {
  return useWarningWindowSchema({
    dayLabels: {
      7: $t('hr.documentWarning.days7'),
      30: $t('hr.documentWarning.days30'),
      60: $t('hr.documentWarning.days60'),
      90: $t('hr.documentWarning.days90'),
    },
    label: $t('hr.documentWarning.warningWindow'),
  });
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
