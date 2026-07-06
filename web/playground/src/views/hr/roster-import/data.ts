import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { $t } from '#/locales';

import { formatHrDictLabel, HR_ROSTER_IMPORT_STATUS_DICT } from '../dict-options';

export function useColumns(): VxeTableGridColumns {
  return [
    {
      field: 'batchNo',
      title: $t('hr.rosterImport.batchNo'),
      width: 180,
    },
    {
      field: 'fileName',
      title: $t('hr.rosterImport.fileName'),
      minWidth: 150,
    },
    {
      field: 'status',
      title: $t('hr.rosterImport.status'),
      width: 120,
      formatter: ({ cellValue }) =>
        formatHrDictLabel(HR_ROSTER_IMPORT_STATUS_DICT, cellValue),
    },
    {
      field: 'totalCount',
      title: $t('hr.rosterImport.totalCount'),
      width: 100,
    },
    {
      field: 'successCount',
      title: $t('hr.rosterImport.successCount'),
      width: 100,
    },
    {
      field: 'failedCount',
      title: $t('hr.rosterImport.failedCount'),
      width: 100,
    },
    {
      field: 'completedAt',
      title: $t('hr.rosterImport.completedAt'),
      width: 180,
    },
    {
      field: 'createdAt',
      title: $t('common.createTime'),
      width: 180,
    },
  ];
}

export function useErrorColumns(): VxeTableGridColumns {
  return [
    {
      field: 'rowNumber',
      title: $t('hr.rosterImport.rowNumber'),
      width: 100,
    },
    {
      field: 'employeeNo',
      title: '工号',
      width: 120,
    },
    {
      field: 'fieldName',
      title: $t('hr.rosterImport.fieldName'),
      width: 150,
    },
    {
      field: 'errorMessage',
      title: $t('hr.rosterImport.errorMessage'),
      minWidth: 200,
    },
  ];
}
