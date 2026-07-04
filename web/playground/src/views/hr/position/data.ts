import type { VbenFormSchema } from '#/adapter/form';
import type { OnActionClickFn, VxeTableGridColumns } from '#/adapter/vxe-table';
import type { HrPositionApi } from '#/api';

import { getDeptList } from '#/api';
import { $t } from '#/locales';

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'positionCode',
      label: $t('hr.position.code'),
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'positionName',
      label: $t('hr.position.name'),
      rules: 'required',
    },
    {
      component: 'ApiTreeSelect',
      componentProps: {
        allowClear: true,
        api: getDeptList,
        class: 'w-full',
        labelField: 'name',
        valueField: 'id',
        childrenField: 'children',
      },
      fieldName: 'deptId',
      label: $t('hr.position.dept'),
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
      label: $t('hr.position.status'),
    },
    {
      component: 'Textarea',
      fieldName: 'description',
      label: $t('hr.position.description'),
    },
  ];
}

export function useColumns<T = HrPositionApi.HrPosition>(
  onActionClick: OnActionClickFn<T>,
  onStatusChange?: (newStatus: any, row: T) => PromiseLike<boolean | undefined>,
): VxeTableGridColumns {
  return [
    {
      field: 'positionCode',
      title: $t('hr.position.code'),
      width: 150,
    },
    {
      field: 'positionName',
      title: $t('hr.position.name'),
      width: 180,
    },
    {
      field: 'deptName',
      title: $t('hr.position.dept'),
      width: 180,
    },
    {
      cellRender: {
        attrs: { beforeChange: onStatusChange },
        name: onStatusChange ? 'CellSwitch' : 'CellTag',
      },
      field: 'enabled',
      title: $t('hr.position.status'),
      width: 120,
    },
    {
      field: 'description',
      minWidth: 150,
      title: $t('hr.position.description'),
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
          nameField: 'positionName',
          nameTitle: $t('hr.position.title'),
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
