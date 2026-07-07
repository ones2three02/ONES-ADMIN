import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { $t } from '#/locales';

import {
  formatHrDictLabel,
  getHrDictOptions,
  HR_CONTRACT_STATUS_DICT,
  HR_CONTRACT_TYPE_DICT,
} from '../dict-options';

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
      label: '终止日期',
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
  return [
    {
      component: 'Select',
      componentProps: {
        allowClear: false,
        options: [
          { label: $t('hr.contract.days7'), value: 7 },
          { label: $t('hr.contract.days30'), value: 30 },
          { label: $t('hr.contract.days60'), value: 60 },
          { label: $t('hr.contract.days90'), value: 90 },
        ],
      },
      defaultValue: 30,
      fieldName: 'days',
      label: $t('hr.contract.warningWindow'),
    },
  ];
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
  ];
}
