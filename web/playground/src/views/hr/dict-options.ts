import type { SystemDictApi } from '#/api';

import {
  formatDictOptionLabel,
  getCachedDictOption,
  getCachedDictOptions,
  getDictFallbackOptions,
  registerDictFallbackOptions,
} from '#/api';
import { $t } from '#/locales';

export const HR_CONTRACT_STATUS_DICT = 'hr_contract_status';
export const HR_CONTRACT_TYPE_DICT = 'hr_contract_type';
export const HR_EMPLOYMENT_STATUS_DICT = 'hr_employment_status';
export const HR_EMPLOYMENT_TYPE_DICT = 'hr_employment_type';
export const HR_GENDER_DICT = 'hr_gender';
export const HR_EMPLOYEE_DOCUMENT_TYPE_DICT = 'hr_employee_document_type';
export const HR_ROSTER_IMPORT_STATUS_DICT = 'hr_roster_import_status';

const fallbackEmploymentTypeOptions = (): SystemDictApi.DictOption[] => [
  {
    color: 'success',
    label: $t('hr.dictFallback.employmentType.fullTime'),
    sortOrder: 10,
    value: 'FULL_TIME',
  },
  {
    color: 'blue',
    label: $t('hr.dictFallback.employmentType.partTime'),
    sortOrder: 20,
    value: 'PART_TIME',
  },
  {
    color: 'processing',
    label: $t('hr.dictFallback.employmentType.intern'),
    sortOrder: 30,
    value: 'INTERN',
  },
  {
    color: 'warning',
    label: $t('hr.dictFallback.employmentType.outsourced'),
    sortOrder: 40,
    value: 'OUTSOURCED',
  },
];

const fallbackEmploymentStatusOptions = (): SystemDictApi.DictOption[] => [
  {
    color: 'success',
    label: $t('hr.dictFallback.employmentStatus.active'),
    sortOrder: 10,
    value: 'ACTIVE',
  },
  {
    color: 'processing',
    label: $t('hr.dictFallback.employmentStatus.probation'),
    sortOrder: 20,
    value: 'PROBATION',
  },
  {
    color: 'warning',
    label: $t('hr.dictFallback.employmentStatus.suspended'),
    sortOrder: 30,
    value: 'SUSPENDED',
  },
  {
    color: 'error',
    label: $t('hr.dictFallback.employmentStatus.resigned'),
    sortOrder: 40,
    value: 'RESIGNED',
  },
];

const fallbackContractTypeOptions = (): SystemDictApi.DictOption[] => [
  {
    color: 'processing',
    label: $t('hr.dictFallback.contractType.fixedTerm'),
    sortOrder: 10,
    value: 'FIXED_TERM',
  },
  {
    color: 'success',
    label: $t('hr.dictFallback.contractType.openEnded'),
    sortOrder: 20,
    value: 'OPEN_ENDED',
  },
  {
    color: 'blue',
    label: $t('hr.dictFallback.contractType.internship'),
    sortOrder: 30,
    value: 'INTERNSHIP',
  },
  {
    color: 'warning',
    label: $t('hr.dictFallback.contractType.service'),
    sortOrder: 40,
    value: 'SERVICE',
  },
];

const fallbackGenderOptions = (): SystemDictApi.DictOption[] => [
  {
    color: 'blue',
    label: $t('hr.dictFallback.gender.male'),
    sortOrder: 10,
    value: 'MALE',
  },
  {
    color: 'magenta',
    label: $t('hr.dictFallback.gender.female'),
    sortOrder: 20,
    value: 'FEMALE',
  },
];

const fallbackContractStatusOptions = (): SystemDictApi.DictOption[] => [
  {
    color: 'default',
    label: $t('hr.dictFallback.contractStatus.draft'),
    sortOrder: 10,
    value: 'DRAFT',
  },
  {
    color: 'success',
    label: $t('hr.dictFallback.contractStatus.active'),
    sortOrder: 20,
    value: 'ACTIVE',
  },
  {
    color: 'warning',
    label: $t('hr.dictFallback.contractStatus.expiring'),
    sortOrder: 30,
    value: 'EXPIRING',
  },
  {
    color: 'error',
    label: $t('hr.dictFallback.contractStatus.terminated'),
    sortOrder: 40,
    value: 'TERMINATED',
  },
];

const fallbackRosterImportStatusOptions = (): SystemDictApi.DictOption[] => [
  {
    color: 'processing',
    label: $t('hr.dictFallback.rosterImportStatus.parsing'),
    sortOrder: 10,
    value: 'PARSING',
  },
  {
    color: 'error',
    label: $t('hr.dictFallback.rosterImportStatus.validationFailed'),
    sortOrder: 20,
    value: 'VALIDATION_FAILED',
  },
  {
    color: 'warning',
    label: $t('hr.dictFallback.rosterImportStatus.partialSuccess'),
    sortOrder: 30,
    value: 'PARTIAL_SUCCESS',
  },
  {
    color: 'success',
    label: $t('hr.dictFallback.rosterImportStatus.success'),
    sortOrder: 40,
    value: 'SUCCESS',
  },
  {
    color: 'error',
    label: $t('hr.dictFallback.rosterImportStatus.failed'),
    sortOrder: 50,
    value: 'FAILED',
  },
];

const fallbackEmployeeDocumentTypeOptions = (): SystemDictApi.DictOption[] => [
  {
    color: 'processing',
    label: $t('hr.dictFallback.employeeDocumentType.identity'),
    sortOrder: 10,
    value: 'IDENTITY',
  },
  {
    color: 'blue',
    label: $t('hr.dictFallback.employeeDocumentType.education'),
    sortOrder: 20,
    value: 'EDUCATION',
  },
  {
    color: 'warning',
    label: $t('hr.dictFallback.employeeDocumentType.certificate'),
    sortOrder: 30,
    value: 'CERTIFICATE',
  },
  {
    color: 'success',
    label: $t('hr.dictFallback.employeeDocumentType.medical'),
    sortOrder: 40,
    value: 'MEDICAL',
  },
  {
    color: 'default',
    label: $t('hr.dictFallback.employeeDocumentType.other'),
    sortOrder: 50,
    value: 'OTHER',
  },
];

const fallbackMap: Record<string, () => SystemDictApi.DictOption[]> = {
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
