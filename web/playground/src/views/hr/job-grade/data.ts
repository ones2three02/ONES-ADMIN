import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { HrJobGradeApi } from '#/api';

import { $t } from '#/locales';

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'gradeCode',
      label: $t('hr.jobGrade.code'),
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'gradeName',
      label: $t('hr.jobGrade.name'),
      rules: 'required',
    },
    {
      component: 'InputNumber',
      componentProps: {
        min: 0,
        precision: 0,
        style: { width: '100%' },
      },
      defaultValue: 0,
      fieldName: 'gradeRank',
      label: $t('hr.jobGrade.rank'),
      rules: 'required',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: $t('common.enabled'), value: true },
          { label: $t('common.disabled'), value: false },
        ],
        optionType: 'button',
      },
      defaultValue: true,
      fieldName: 'enabled',
      label: $t('hr.jobGrade.status'),
    },
  ];
}

export function useColumns<T = HrJobGradeApi.HrJobGrade>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    {
      field: 'gradeCode',
      title: $t('hr.jobGrade.code'),
      width: 150,
    },
    {
      field: 'gradeName',
      title: $t('hr.jobGrade.name'),
      width: 180,
    },
    {
      field: 'gradeRank',
      title: $t('hr.jobGrade.rank'),
      width: 150,
      sortable: true,
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'enabled',
      title: $t('hr.jobGrade.status'),
      width: 120,
    },
    {
      field: 'createdAt',
      title: $t('common.createTime'),
      width: 180,
    },
    {
      align: 'center',
      cellRender: {
        attrs: {
          nameField: 'gradeName',
          nameTitle: $t('hr.jobGrade.title'),
          onClick: onActionClick,
        },
        name: 'CellOperation',
      },
      field: 'operation',
      fixed: 'right',
      title: $t('common.action'),
      width: 120,
    },
  ];
}
