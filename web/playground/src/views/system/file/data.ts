import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { h } from 'vue';

import { Tag } from 'antdv-next';

const statusColorMap: Record<string, string> = {
  ACTIVE: 'success',
  DELETED: 'error',
  PURGED: 'default',
};

const storageColorMap: Record<string, string> = {
  LOCAL: 'processing',
  MINIO: 'blue',
};

function formatEmpty(value?: null | number | string) {
  return value === undefined || value === null || value === '' ? '-' : value;
}

function formatSize(size?: number) {
  if (size === undefined || size === null) {
    return '-';
  }
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function tag(value: string | undefined, colors: Record<string, string>) {
  if (!value) {
    return '-';
  }
  return h(Tag, { color: colors[value] ?? 'default' }, () => value);
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'originalName',
      label: '文件名',
    },
    {
      component: 'Input',
      fieldName: 'extension',
      label: '扩展名',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '本地存储', value: 'LOCAL' },
          { label: 'MinIO', value: 'MINIO' },
        ],
      },
      fieldName: 'storageType',
      label: '存储类型',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '可用', value: 'ACTIVE' },
          { label: '已删除', value: 'DELETED' },
          { label: '已清理', value: 'PURGED' },
        ],
      },
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'Input',
      fieldName: 'businessType',
      label: '业务类型',
    },
    {
      component: 'RangePicker',
      componentProps: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: true,
      },
      fieldName: 'createdAt',
      label: '上传时间',
    },
  ];
}

export function useColumns(): VxeTableGridColumns {
  return [
    {
      field: 'originalName',
      minWidth: 220,
      title: '文件名',
    },
    {
      field: 'extension',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      title: '扩展名',
      width: 90,
    },
    {
      field: 'sizeBytes',
      formatter: ({ cellValue }) => formatSize(cellValue),
      title: '大小',
      width: 110,
    },
    {
      field: 'storageType',
      slots: {
        default: ({ row }) => tag(row.storageType, storageColorMap),
      },
      title: '存储',
      width: 110,
    },
    {
      field: 'status',
      slots: {
        default: ({ row }) => tag(row.status, statusColorMap),
      },
      title: '状态',
      width: 100,
    },
    {
      field: 'businessType',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 180,
      title: '业务类型',
    },
    {
      field: 'businessId',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      minWidth: 130,
      title: '业务 ID',
    },
    {
      field: 'uploadedBy',
      formatter: ({ cellValue }) => formatEmpty(cellValue),
      title: '上传人',
      width: 110,
    },
    {
      field: 'createdAt',
      title: '上传时间',
      width: 180,
    },
    {
      align: 'center',
      field: 'action',
      fixed: 'right',
      slots: { default: 'action' },
      title: '操作',
      width: 160,
    },
  ];
}
