import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { getDeptList, getEmployeeList, getJobGradeList, getPositionList } from '#/api';
import { $t } from '#/locales';

import {
  formatHrDictLabel,
  getHrDictOptions,
  HR_EMPLOYMENT_STATUS_DICT,
  HR_EMPLOYMENT_TYPE_DICT,
} from '../dict-options';

async function getEmployeeOptions() {
  const res = await getEmployeeList({ page: 1, pageSize: 200 });
  return res.items;
}

export function useFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'employeeNo',
      label: $t('hr.employee.employeeNo'),
      rules: 'required',
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
    },
    {
      component: 'Input',
      fieldName: 'realName',
      label: $t('hr.employee.realName'),
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'preferredName',
      label: $t('hr.employee.preferredName'),
    },
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: '男', value: 'MALE' },
          { label: '女', value: 'FEMALE' },
        ],
      },
      fieldName: 'gender',
      label: $t('hr.employee.gender'),
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'mobile',
      label: $t('hr.employee.mobile'),
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'email',
      label: $t('hr.employee.email'),
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'idCardNumber',
      label: $t('hr.employee.idCard'),
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
      label: $t('hr.employee.deptName'),
      rules: 'required',
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: getPositionList,
        labelField: 'positionName',
        valueField: 'id',
      },
      fieldName: 'positionId',
      label: $t('hr.employee.positionName'),
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: getJobGradeList,
        labelField: 'gradeName',
        valueField: 'id',
      },
      fieldName: 'gradeId',
      label: $t('hr.employee.gradeName'),
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: getEmployeeOptions,
        labelField: 'realName',
        valueField: 'id',
      },
      fieldName: 'managerEmployeeId',
      label: $t('hr.employee.managerName'),
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: () => getHrDictOptions(HR_EMPLOYMENT_TYPE_DICT),
        labelField: 'label',
        valueField: 'value',
      },
      fieldName: 'employmentType',
      label: $t('hr.employee.employmentType'),
      rules: 'required',
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: () => getHrDictOptions(HR_EMPLOYMENT_STATUS_DICT),
        labelField: 'label',
        valueField: 'value',
      },
      fieldName: 'employmentStatus',
      label: $t('hr.employee.employmentStatus'),
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
    },
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'hireDate',
      label: $t('hr.employee.hireDate'),
      rules: 'required',
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
    },
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'probationEndDate',
      label: $t('hr.employee.probationEndDate'),
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      label: $t('hr.employee.remark'),
    },
  ];
}

export function useTransferSchema(): VbenFormSchema[] {
  return [
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
      label: '调入部门',
      rules: 'required',
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: getPositionList,
        labelField: 'positionName',
        valueField: 'id',
      },
      fieldName: 'positionId',
      label: '新岗位',
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: getJobGradeList,
        labelField: 'gradeName',
        valueField: 'id',
      },
      fieldName: 'gradeId',
      label: '新职级',
    },
    {
      component: 'ApiSelect',
      componentProps: {
        api: getEmployeeOptions,
        labelField: 'realName',
        valueField: 'id',
      },
      fieldName: 'managerEmployeeId',
      label: '新主管',
    },
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'effectiveDate',
      label: '生效日期',
      rules: 'required',
    },
    {
      component: 'Textarea',
      fieldName: 'changeReason',
      label: '变动原因',
      rules: 'required',
    },
  ];
}

export function useRegularizeSchema(): VbenFormSchema[] {
  return [
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'regularizeDate',
      label: '实际转正日期',
      rules: 'required',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      label: '转正备注',
    },
  ];
}

export function useResignSchema(): VbenFormSchema[] {
  return [
    {
      component: 'DatePicker',
      componentProps: {
        valueFormat: 'YYYY-MM-DD',
      },
      fieldName: 'leaveDate',
      label: '离职日期',
      rules: 'required',
    },
    {
      component: 'Textarea',
      fieldName: 'resignationReason',
      label: '离职原因',
      rules: 'required',
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'keyword',
      label: '姓名/工号/手机/邮箱',
    },
    {
      component: 'ApiSelect',
      componentProps: {
        allowClear: true,
        api: () => getHrDictOptions(HR_EMPLOYMENT_STATUS_DICT),
        labelField: 'label',
        valueField: 'value',
      },
      fieldName: 'employmentStatus',
      label: $t('hr.employee.employmentStatus'),
    },
  ];
}

export function useColumns(): VxeTableGridColumns {
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
      field: 'gender',
      title: $t('hr.employee.gender'),
      width: 80,
      formatter: ({ cellValue }) => (cellValue === 'MALE' ? '男' : '女'),
    },
    {
      field: 'mobile',
      title: $t('hr.employee.mobile'),
      width: 130,
    },
    {
      field: 'email',
      title: $t('hr.employee.email'),
      width: 180,
    },
    {
      field: 'deptName',
      title: $t('hr.employee.deptName'),
      width: 150,
    },
    {
      field: 'positionName',
      title: $t('hr.employee.positionName'),
      width: 150,
    },
    {
      field: 'gradeName',
      title: $t('hr.employee.gradeName'),
      width: 120,
    },
    {
      field: 'employmentType',
      title: $t('hr.employee.employmentType'),
      width: 120,
      formatter: ({ cellValue }) =>
        formatHrDictLabel(HR_EMPLOYMENT_TYPE_DICT, cellValue),
    },
    {
      field: 'employmentStatus',
      title: $t('hr.employee.employmentStatus'),
      width: 100,
      formatter: ({ cellValue }) =>
        formatHrDictLabel(HR_EMPLOYMENT_STATUS_DICT, cellValue),
    },
    {
      field: 'hireDate',
      title: $t('hr.employee.hireDate'),
      width: 120,
    },
    {
      field: 'probationEndDate',
      title: $t('hr.employee.probationEndDate'),
      width: 140,
    },
    {
      field: 'leaveDate',
      title: $t('hr.employee.leaveDate'),
      width: 120,
    },
  ];
}
