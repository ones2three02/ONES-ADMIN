import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridColumns } from '#/adapter/vxe-table';

import { getDeptList, getEmployeeList, getJobGradeList, getPositionList } from '#/api';
import { $t } from '#/locales';

async function getEmployeeOptions() {
  const res = await getEmployeeList({ page: 1, pageSize: 200 });
  return res.items;
}

export function useFormSchema(): VbenFormSchema[] {
  return [
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
      fieldName: 'idCard',
      label: $t('hr.employee.idCard'),
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
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
    },
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: '全职(正式)', value: 'FULL_TIME' },
          { label: '兼职', value: 'PART_TIME' },
          { label: '实习生', value: 'INTERN' },
          { label: '劳务外包', value: 'OUTSOURCED' },
        ],
      },
      fieldName: 'employmentType',
      label: $t('hr.employee.employmentType'),
      rules: 'required',
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
      component: 'InputNumber',
      componentProps: {
        min: 0,
        precision: 0,
        style: { width: '100%' },
      },
      fieldName: 'probationMonths',
      label: '试用期(月)',
      dependencies: {
        show: (values) => !values.id,
        triggerFields: ['id'],
      },
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
      fieldName: 'actualRegularizeDate',
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
      fieldName: 'reason',
      label: '离职原因',
      rules: 'required',
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      fieldName: 'realName',
      label: $t('hr.employee.realName'),
    },
    {
      component: 'Input',
      fieldName: 'employeeNo',
      label: $t('hr.employee.employeeNo'),
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: [
          { label: '在职', value: 'ACTIVE' },
          { label: '试用期', value: 'PROBATION' },
          { label: '已离职', value: 'TERMINATED' },
        ],
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
      formatter: ({ cellValue }) => {
        const types: any = {
          FULL_TIME: '全职(正式)',
          PART_TIME: '兼职',
          INTERN: '实习生',
          OUTSOURCED: '劳务外包',
        };
        return types[cellValue] || cellValue;
      },
    },
    {
      field: 'employmentStatus',
      title: $t('hr.employee.employmentStatus'),
      width: 100,
      formatter: ({ cellValue }) => {
        const statuses: any = {
          ACTIVE: '在职(正式)',
          PROBATION: '试用期',
          TERMINATED: '已离职',
        };
        return statuses[cellValue] || cellValue;
      },
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
