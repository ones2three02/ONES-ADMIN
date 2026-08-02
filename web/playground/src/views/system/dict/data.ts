import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { h } from 'vue';

import { Tag } from 'antdv-next';

function statusTag(enabled: boolean) {
  return h(
    Tag,
    { color: enabled ? 'success' : 'error' },
    () => (enabled ? '启用' : '停用'),
  );
}

function colorTag(value?: string) {
  if (!value) {
    return '-';
  }
  return h(Tag, { color: value }, () => value);
}

export function useTypeFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'dictCode',
      label: '字典编码',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'dictName',
      label: '字典名称',
      rules: 'required',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '启用', value: true },
          { label: '停用', value: false },
        ],
        optionType: 'button',
      },
      defaultValue: true,
      fieldName: 'enabled',
      label: '状态',
    },
    {
      component: 'InputNumber',
      componentProps: {
        min: 0,
        precision: 0,
        style: { width: '100%' },
      },
      defaultValue: 0,
      fieldName: 'sortOrder',
      label: '排序',
    },
    {
      component: 'Textarea',
      componentProps: {
        maxLength: 255,
        rows: 3,
        showCount: true,
      },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

export function useItemFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      dependencies: {
        disabled: true,
        triggerFields: ['dictCode'],
      },
      fieldName: 'dictCode',
      label: '字典编码',
    },
    {
      component: 'Input',
      fieldName: 'itemLabel',
      label: '字典标签',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'itemValue',
      label: '字典值',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: 'success', value: 'success' },
          { label: 'processing', value: 'processing' },
          { label: 'warning', value: 'warning' },
          { label: 'error', value: 'error' },
          { label: 'blue', value: 'blue' },
          { label: 'default', value: 'default' },
        ],
      },
      fieldName: 'color',
      label: '颜色',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: [
          { label: '启用', value: true },
          { label: '停用', value: false },
        ],
        optionType: 'button',
      },
      defaultValue: true,
      fieldName: 'enabled',
      label: '状态',
    },
    {
      component: 'InputNumber',
      componentProps: {
        min: 0,
        precision: 0,
        style: { width: '100%' },
      },
      defaultValue: 0,
      fieldName: 'sortOrder',
      label: '排序',
    },
    {
      component: 'Textarea',
      componentProps: {
        maxLength: 255,
        rows: 3,
        showCount: true,
      },
      fieldName: 'remark',
      label: '备注',
    },
  ];
}

export function useTypeSearchSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'dictCode',
      label: '字典编码',
    },
    {
      component: 'Input',
      fieldName: 'dictName',
      label: '字典名称',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '启用', value: true },
          { label: '停用', value: false },
        ],
      },
      fieldName: 'enabled',
      label: '状态',
    },
  ];
}

export function useItemSearchSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'itemLabel',
      label: '字典标签',
    },
    {
      component: 'Input',
      fieldName: 'itemValue',
      label: '字典值',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '启用', value: true },
          { label: '停用', value: false },
        ],
      },
      fieldName: 'enabled',
      label: '状态',
    },
  ];
}

export function useTypeColumns(): VxeTableGridColumns {
  return [
    {
      field: 'dictCode',
      minWidth: 170,
      title: '字典编码',
    },
    {
      field: 'dictName',
      minWidth: 150,
      title: '字典名称',
    },
    {
      field: 'enabled',
      slots: {
        default: ({ row }) => statusTag(row.enabled),
      },
      title: '状态',
      width: 90,
    },
    {
      field: 'itemCount',
      title: '字典项',
      width: 90,
    },
    {
      field: 'sortOrder',
      title: '排序',
      width: 80,
    },
    {
      field: 'createdAt',
      title: '创建时间',
      width: 180,
    },
    {
      align: 'center',
      field: 'action',
      fixed: 'right',
      title: '操作',
      width: 150,
      slots: { default: 'typeAction' },
    },
  ];
}

export function useItemColumns(): VxeTableGridColumns {
  return [
    {
      field: 'itemLabel',
      minWidth: 160,
      title: '字典标签',
    },
    {
      field: 'itemValue',
      minWidth: 150,
      title: '字典值',
    },
    {
      field: 'color',
      slots: {
        default: ({ row }) => colorTag(row.color),
      },
      title: '颜色',
      width: 120,
    },
    {
      field: 'enabled',
      slots: {
        default: ({ row }) => statusTag(row.enabled),
      },
      title: '状态',
      width: 90,
    },
    {
      field: 'sortOrder',
      title: '排序',
      width: 80,
    },
    {
      field: 'remark',
      minWidth: 180,
      title: '备注',
    },
    {
      align: 'center',
      field: 'action',
      fixed: 'right',
      title: '操作',
      width: 150,
      slots: { default: 'itemAction' },
    },
  ];
}
