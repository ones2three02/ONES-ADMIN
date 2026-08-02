import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { $t } from '#/locales';

import {
  formatHrDictLabel,
  getHrDictOptions,
  HR_CONTRACT_STATUS_DICT,
  HR_CONTRACT_TYPE_DICT,
} from '../dict-options';
import {
  daysUntil,
  renderDueRiskTag,
  useWarningWindowSchema,
} from '../shared/warning';

export { daysUntil };

function contractRiskTag(endDate?: string) {
  return renderDueRiskTag({
    date: endDate,
    dueTodayText: $t('hr.contract.dueToday'),
    expiredText: $t('hr.contract.expired'),
    expiringSoonText: $t('hr.contract.expiringSoon'),
    remainingText: (days) => $t('hr.contract.remainingDays', { days }),
  });
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'contractNo',
      label: $t('hr.contract.contractNo'),
      rules: 'required',
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: () => getHrDictOptions(HR_CONTRACT_TYPE_DICT),
        labelField: 'label',
        valueField: 'value',
      },
      fieldName: 'contractType',
      label: $t('hr.contract.contractType'),
      rules: 'required',
    },
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'startDate',
      label: $t('hr.contract.startDate'),
      rules: 'required',
    },
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'endDate',
      label: $t('hr.contract.endDate'),
    },
    {
      component: 'InputNumber',
      componentProps: {
        min: 0,
        precision: 0,
        style: { width: '100%' },
      },
      fieldName: 'probationMonths',
      label: $t('hr.contract.probationMonths'),
    },
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'renewalRemindDate',
      label: $t('hr.contract.renewalRemindDate'),
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      label: $t('hr.contract.remark'),
    },
    {
      component: 'Input',
      dependencies: {
        show: false,
        triggerFields: ['contractNo'],
      },
      fieldName: 'attachmentFileId',
      label: $t('hr.contract.attachment'),
    },
  ];
}

export function useTerminateSchema(): VbenFormSchema[] {
  return [
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'actualTerminateDate',
      label: $t('hr.contract.actualTerminateDate'),
      rules: 'required',
    },
    {
      component: 'Textarea',
      fieldName: 'terminateReason',
      label: $t('hr.contract.terminateReason'),
      rules: 'required',
    },
  ];
}

export function useColumns(): VxeTableGridColumns {
  return [
    {
      field: 'contractNo',
      title: $t('hr.contract.contractNo'),
      width: 150,
    },
    {
      field: 'contractType',
      title: $t('hr.contract.contractType'),
      width: 150,
      formatter: ({ cellValue }) => formatHrDictLabel(HR_CONTRACT_TYPE_DICT, cellValue),
    },
    {
      field: 'status',
      title: $t('hr.contract.status'),
      width: 120,
      formatter: ({ cellValue }) => formatHrDictLabel(HR_CONTRACT_STATUS_DICT, cellValue),
    },
    {
      field: 'startDate',
      title: $t('hr.contract.startDate'),
      width: 120,
    },
    {
      field: 'endDate',
      title: $t('hr.contract.endDate'),
      width: 120,
    },
    {
      field: 'probationMonths',
      title: $t('hr.contract.probationMonths'),
      width: 120,
    },
    {
      field: 'renewalRemindDate',
      title: $t('hr.contract.renewalRemindDate'),
      width: 130,
    },
    {
      field: 'attachmentFileId',
      slots: { default: 'attachment' },
      title: $t('hr.contract.attachment'),
      width: 110,
    },
    {
      field: 'remark',
      minWidth: 150,
      title: $t('hr.contract.remark'),
    },
  ];
}

export function useExpiringGridFormSchema(): VbenFormSchema[] {
  return useWarningWindowSchema({
    dayLabels: {
      7: $t('hr.contract.days7'),
      30: $t('hr.contract.days30'),
      60: $t('hr.contract.days60'),
      90: $t('hr.contract.days90'),
    },
    label: $t('hr.contract.warningWindow'),
  });
}

export function useExpiringColumns(): VxeTableGridColumns {
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
      field: 'contractNo',
      title: $t('hr.contract.contractNo'),
      width: 150,
    },
    {
      field: 'contractType',
      title: $t('hr.contract.contractType'),
      width: 150,
      formatter: ({ cellValue }) => formatHrDictLabel(HR_CONTRACT_TYPE_DICT, cellValue),
    },
    {
      field: 'status',
      title: $t('hr.contract.status'),
      width: 120,
      formatter: ({ cellValue }) => formatHrDictLabel(HR_CONTRACT_STATUS_DICT, cellValue),
    },
    {
      field: 'endDate',
      title: $t('hr.contract.endDate'),
      width: 120,
    },
    {
      field: 'renewalRemindDate',
      title: $t('hr.contract.renewalRemindDate'),
      width: 130,
    },
    {
      field: 'riskStatus',
      slots: {
        default: ({ row }) => contractRiskTag(row.endDate),
      },
      title: $t('hr.contract.riskStatus'),
      width: 130,
    },
  ];
}
