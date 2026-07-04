import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { $t } from '#/locales';

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'contractNo',
      label: $t('hr.contract.contractNo'),
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: '固定期限劳动合同', value: 'FIXED' },
          { label: '无固定期限劳动合同', value: 'UNFIXED' },
          { label: '劳务派遣合同', value: 'DISPATCH' },
          { label: '实习协议', value: 'INTERN' },
        ],
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
      formatter: ({ cellValue }) => {
        const types: any = {
          FIXED: '固定期限劳动合同',
          UNFIXED: '无固定期限劳动合同',
          DISPATCH: '劳务派遣合同',
          INTERN: '实习协议',
        };
        return types[cellValue] || cellValue;
      },
    },
    {
      field: 'status',
      title: $t('hr.contract.status'),
      width: 120,
      formatter: ({ cellValue }) => {
        const statuses: any = {
          ACTIVE: '履约中',
          TERMINATED: '已终止',
          EXPIRED: '已到期',
        };
        return statuses[cellValue] || cellValue;
      },
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
      field: 'remark',
      minWidth: 150,
      title: $t('hr.contract.remark'),
    },
  ];
}

export function useExpiringColumns(): VxeTableGridColumns {
  return [
    {
      field: 'employeeNo',
      title: '工号',
      width: 100,
    },
    {
      field: 'employeeName',
      title: '姓名',
      width: 120,
    },
    {
      field: 'contractNo',
      title: '合同编号',
      width: 150,
    },
    {
      field: 'endDate',
      title: '到期日期',
      width: 120,
    },
    {
      field: 'renewalRemindDate',
      title: '预警提醒日期',
      width: 130,
    },
  ];
}
