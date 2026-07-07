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

function successTag(success: boolean) {
  return h(
    Tag,
    { color: success ? 'success' : 'error' },
    () => (success ? '成功' : '失败'),
  );
}

function formatEmpty(value?: null | number | string) {
  return value === undefined || value === null || value === '' ? '-' : value;
}

export function useLoginLogFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'username',
      label: '用户名',
    },
    {
      component: 'Input',
      fieldName: 'ip',
      label: 'IP 地址',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '成功', value: true },
          { label: '失败', value: false },
        ],
      },
      fieldName: 'success',
      label: '登录结果',
    },
    {
      component: 'RangePicker',
      componentProps: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: true,
      },
      fieldName: 'createdAt',
      label: '登录时间',
    },
  ];
}

export function useOperationLogFormSchema(): VbenFormSchema[] {
  return [
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
      component: 'Input',
      fieldName: 'operation',
      label: '操作',
    },
    {
      component: 'Input',
      fieldName: 'permissionCode',
      label: '权限码',
    },
    {
      component: 'Input',
      fieldName: 'traceId',
      label: 'TraceId',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '成功', value: true },
          { label: '失败', value: false },
        ],
      },
      fieldName: 'success',
      label: '执行结果',
    },
    {
      component: 'RangePicker',
      componentProps: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: true,
      },
      fieldName: 'createdAt',
      label: '操作时间',
    },
  ];
}

export function useLoginLogColumns(): VxeTableGridColumns {
  return [
    {
      field: 'username',
      minWidth: 140,
      title: '用户名',
    },
    {
      field: 'success',
      slots: {
        default: ({ row }) => successTag(row.success),
      },
      title: '结果',
      width: 90,
    },
    {
      field: 'failureReason',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 200,
      title: '失败原因',
    },
    {
      field: 'ip',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 140,
      title: 'IP 地址',
    },
    {
      field: 'traceId',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 220,
      title: 'TraceId',
    },
    {
      field: 'userAgent',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 260,
      title: 'User-Agent',
    },
    {
      field: 'createdAt',
      title: '登录时间',
      width: 180,
    },
  ];
}

export function useOperationLogColumns(): VxeTableGridColumns {
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
      field: 'module',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 150,
      title: '模块',
    },
    {
      field: 'operation',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 180,
      title: '操作',
    },
    {
      field: 'success',
      slots: {
        default: ({ row }) => successTag(row.success),
      },
      title: '结果',
      width: 90,
    },
    {
      field: 'responseCode',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      title: '响应码',
      width: 100,
    },
    {
      field: 'durationMs',
      formatter: ({ cellValue }) =>
        cellValue === undefined || cellValue === null ? '-' : `${cellValue} ms`,
      title: '耗时',
      width: 110,
    },
    {
      field: 'permissionCode',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 190,
      title: '权限码',
    },
    {
      field: 'traceId',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 220,
      title: 'TraceId',
    },
    {
      field: 'createdAt',
      title: '操作时间',
      width: 180,
    },
  ];
}
