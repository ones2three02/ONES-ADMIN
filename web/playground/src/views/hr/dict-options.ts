import type { SystemDictApi } from '#/api';

import { getDictOptions } from '#/api';

export const HR_CONTRACT_TYPE_DICT = 'hr_contract_type';
export const HR_EMPLOYMENT_STATUS_DICT = 'hr_employment_status';
export const HR_EMPLOYMENT_TYPE_DICT = 'hr_employment_type';

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

const fallbackMap: Record<string, SystemDictApi.DictOption[]> = {
  [HR_CONTRACT_TYPE_DICT]: fallbackContractTypeOptions,
  [HR_EMPLOYMENT_STATUS_DICT]: fallbackEmploymentStatusOptions,
  [HR_EMPLOYMENT_TYPE_DICT]: fallbackEmploymentTypeOptions,
};

const cache = new Map<string, SystemDictApi.DictOption[]>();

export async function getHrDictOptions(dictCode: string) {
  if (cache.has(dictCode)) {
    return cache.get(dictCode)!;
  }
  try {
    const options = await getDictOptions(dictCode);
    cache.set(dictCode, options);
    return options;
  } catch {
    const fallback = fallbackMap[dictCode] ?? [];
    cache.set(dictCode, fallback);
    return fallback;
  }
}

export function getHrDictFallbackOptions(dictCode: string) {
  return fallbackMap[dictCode] ?? [];
}

export function formatHrDictLabel(dictCode: string, value?: string) {
  if (!value) {
    return '-';
  }
  const options = cache.get(dictCode) ?? fallbackMap[dictCode] ?? [];
  return options.find((item) => item.value === value)?.label ?? value;
}
