import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { h } from 'vue';

import { Tag } from 'antdv-next';

const methodColorMap: Record<string, string> = {
  DELETE: 'error',
  GET: 'processing',
  PATCH: 'warning',
  POST: 'success',
  PUT: 'warning',
};

const riskColorMap: Record<string, string> = {
  HIGH: 'error',
  LOW: 'success',
  MEDIUM: 'warning',
};

const lifecycleColorMap: Record<string, string> = {
  ACTIVE: 'success',
  DEPRECATED: 'warning',
  REMOVED: 'error',
};

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'path',
      label: '接口路径',
    },
    {
      component: 'Input',
      fieldName: 'module',
      label: '模块',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].map((method) => ({
          label: method,
          value: method,
        })),
      },
      fieldName: 'method',
      label: '请求方法',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '公开接口', value: 'PUBLIC' },
          { label: '登录可访问', value: 'LOGIN' },
          { label: '权限控制', value: 'PERMISSION' },
        ],
      },
      fieldName: 'authType',
      label: '访问策略',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '低风险', value: 'LOW' },
          { label: '中风险', value: 'MEDIUM' },
          { label: '高风险', value: 'HIGH' },
        ],
      },
      fieldName: 'riskLevel',
      label: '风险等级',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '正常', value: 'ACTIVE' },
          { label: '已废弃', value: 'DEPRECATED' },
          { label: '已移除', value: 'REMOVED' },
        ],
      },
      fieldName: 'lifecycle',
      label: '生命周期',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '是', value: true },
          { label: '否', value: false },
        ],
      },
      fieldName: 'permissionMissing',
      label: '权限缺失',
    },
  ];
}

export function useColumns(): VxeTableGridColumns {
  return [
    {
      field: 'method',
      slots: {
        default: ({ row }) =>
          h(
            Tag,
            { color: methodColorMap[row.method] ?? 'default' },
            () => row.method,
          ),
      },
      title: '方法',
      width: 90,
    },
    {
      field: 'path',
      minWidth: 260,
      title: '接口路径',
    },
    {
      field: 'summary',
      minWidth: 180,
      title: '接口说明',
    },
    {
      field: 'module',
      title: '模块',
      width: 160,
    },
    {
      field: 'authType',
      slots: {
        default: ({ row }) =>
          h(
            Tag,
            { color: row.authType === 'PUBLIC' ? 'blue' : 'default' },
            () => row.authType,
          ),
      },
      title: '访问策略',
      width: 120,
    },
    {
      field: 'permissionCodes',
      slots: {
        default: ({ row }) =>
          row.permissionCodes?.length
            ? row.permissionCodes.join('、')
            : row.requiresPermission
              ? '未配置'
              : '-',
      },
      title: '权限码',
      width: 220,
    },
    {
      field: 'riskLevel',
      slots: {
        default: ({ row }) =>
          h(
            Tag,
            { color: riskColorMap[row.riskLevel] ?? 'default' },
            () => row.riskLevel,
          ),
      },
      title: '风险',
      width: 100,
    },
    {
      field: 'lifecycle',
      slots: {
        default: ({ row }) =>
          h(
            Tag,
            { color: lifecycleColorMap[row.lifecycle] ?? 'default' },
            () => row.lifecycle,
          ),
      },
      title: '生命周期',
      width: 120,
    },
    {
      field: 'repeatSubmitProtected',
      slots: {
        default: ({ row }) =>
          row.writeOperation
            ? h(
                Tag,
                { color: row.repeatSubmitProtected ? 'success' : 'warning' },
                () => (row.repeatSubmitProtected ? '已防护' : '未防护'),
              )
            : '-',
      },
      title: '重复提交',
      width: 120,
    },
    {
      field: 'operationId',
      minWidth: 220,
      title: 'Operation ID',
    },
    {
      field: 'owner',
      title: 'Owner',
      width: 140,
    },
    {
      field: 'sinceVersion',
      title: '版本',
      width: 120,
    },
  ];
}
