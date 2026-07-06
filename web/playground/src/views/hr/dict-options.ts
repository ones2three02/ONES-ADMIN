import type { SystemDictApi } from '#/api';

import {
  formatDictOptionLabel,
  getCachedDictOption,
  getCachedDictOptions,
  getDictFallbackOptions,
  registerDictFallbackOptions,
} from '#/api';

export const HR_CONTRACT_STATUS_DICT = 'hr_contract_status';
export const HR_CONTRACT_TYPE_DICT = 'hr_contract_type';
export const HR_EMPLOYMENT_STATUS_DICT = 'hr_employment_status';
export const HR_EMPLOYMENT_TYPE_DICT = 'hr_employment_type';
export const HR_GENDER_DICT = 'hr_gender';
export const HR_EMPLOYEE_DOCUMENT_TYPE_DICT = 'hr_employee_document_type';
export const HR_ROSTER_IMPORT_STATUS_DICT = 'hr_roster_import_status';

const fallbackEmploymentTypeOptions: SystemDictApi.DictOption[] = [
  { color: 'success', label: '全职(正式)', sortOrder: 10, value: 'FULL_TIME' },
  { color: 'blue', label: '兼职', sortOrder: 20, value: 'PART_TIME' },
  { color: 'processing', label: '实习生', sortOrder: 30, value: 'INTERN' },
  { color: 'warning', label: '劳务外包', sortOrder: 40, value: 'OUTSOURCED' },
];

const fallbackEmploymentStatusOptions: SystemDictApi.DictOption[] = [
  { color: 'success', label: '在职(正式)', sortOrder: 10, value: 'ACTIVE' },
  { color: 'processing', label: '试用期', sortOrder: 20, value: 'PROBATION' },
  { color: 'warning', label: '停职', sortOrder: 30, value: 'SUSPENDED' },
  { color: 'error', label: '已离职', sortOrder: 40, value: 'RESIGNED' },
];

const fallbackContractTypeOptions: SystemDictApi.DictOption[] = [
  {
    color: 'processing',
    label: '固定期限劳动合同',
    sortOrder: 10,
    value: 'FIXED_TERM',
  },
  {
    color: 'success',
    label: '无固定期限劳动合同',
    sortOrder: 20,
    value: 'OPEN_ENDED',
  },
  { color: 'blue', label: '实习协议', sortOrder: 30, value: 'INTERNSHIP' },
  { color: 'warning', label: '劳务合同', sortOrder: 40, value: 'SERVICE' },
];

const fallbackGenderOptions: SystemDictApi.DictOption[] = [
  { color: 'blue', label: '男', sortOrder: 10, value: 'MALE' },
  { color: 'magenta', label: '女', sortOrder: 20, value: 'FEMALE' },
];

const fallbackContractStatusOptions: SystemDictApi.DictOption[] = [
  { color: 'default', label: '草稿', sortOrder: 10, value: 'DRAFT' },
  { color: 'success', label: '履约中', sortOrder: 20, value: 'ACTIVE' },
  { color: 'warning', label: '即将到期', sortOrder: 30, value: 'EXPIRING' },
  { color: 'error', label: '已终止', sortOrder: 40, value: 'TERMINATED' },
];

const fallbackRosterImportStatusOptions: SystemDictApi.DictOption[] = [
  { color: 'processing', label: '解析中', sortOrder: 10, value: 'PARSING' },
  { color: 'error', label: '校验失败', sortOrder: 20, value: 'VALIDATION_FAILED' },
  { color: 'warning', label: '部分成功', sortOrder: 30, value: 'PARTIAL_SUCCESS' },
  { color: 'success', label: '导入成功', sortOrder: 40, value: 'SUCCESS' },
  { color: 'error', label: '导入失败', sortOrder: 50, value: 'FAILED' },
];

const fallbackEmployeeDocumentTypeOptions: SystemDictApi.DictOption[] = [
  { color: 'processing', label: '身份证明', sortOrder: 10, value: 'IDENTITY' },
  { color: 'blue', label: '学历证明', sortOrder: 20, value: 'EDUCATION' },
  { color: 'warning', label: '资格证书', sortOrder: 30, value: 'CERTIFICATE' },
  { color: 'success', label: '体检报告', sortOrder: 40, value: 'MEDICAL' },
  { color: 'default', label: '其他资料', sortOrder: 50, value: 'OTHER' },
];

const fallbackMap: Record<string, SystemDictApi.DictOption[]> = {
  [HR_CONTRACT_STATUS_DICT]: fallbackContractStatusOptions,
  [HR_CONTRACT_TYPE_DICT]: fallbackContractTypeOptions,
  [HR_EMPLOYMENT_STATUS_DICT]: fallbackEmploymentStatusOptions,
  [HR_EMPLOYMENT_TYPE_DICT]: fallbackEmploymentTypeOptions,
  [HR_EMPLOYEE_DOCUMENT_TYPE_DICT]: fallbackEmployeeDocumentTypeOptions,
  [HR_GENDER_DICT]: fallbackGenderOptions,
  [HR_ROSTER_IMPORT_STATUS_DICT]: fallbackRosterImportStatusOptions,
};

Object.entries(fallbackMap).forEach(([dictCode, options]) => {
  registerDictFallbackOptions(dictCode, options);
});

export async function getHrDictOptions(dictCode: string) {
  return getCachedDictOptions(dictCode);
}

export function getHrDictFallbackOptions(dictCode: string) {
  return getDictFallbackOptions(dictCode);
}

export function getHrDictOption(dictCode: string, value?: string) {
  return getCachedDictOption(dictCode, value);
}

export function formatHrDictLabel(dictCode: string, value?: string) {
  return formatDictOptionLabel(dictCode, value);
}
