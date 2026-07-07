import type { SystemDictApi } from './dict';

import { getDictOptions } from './dict';

type DictFallbackOptionsSource =
  | SystemDictApi.DictOption[]
  | (() => SystemDictApi.DictOption[]);

const fallbackOptions = new Map<string, DictFallbackOptionsSource>();
const optionsCache = new Map<string, SystemDictApi.DictOption[]>();

export function registerDictFallbackOptions(
  dictCode: string,
  options: DictFallbackOptionsSource,
) {
  fallbackOptions.set(dictCode, options);
  optionsCache.delete(dictCode);
}

export async function getCachedDictOptions(dictCode: string) {
  if (optionsCache.has(dictCode)) {
    return optionsCache.get(dictCode)!;
  }
  try {
    const options = await getDictOptions(dictCode);
    optionsCache.set(dictCode, options);
    return options;
  } catch {
    const fallback = getDictFallbackOptions(dictCode);
    if (typeof fallbackOptions.get(dictCode) !== 'function') {
      optionsCache.set(dictCode, fallback);
    }
    return fallback;
  }
}

export function getDictFallbackOptions(dictCode: string) {
  const options = fallbackOptions.get(dictCode);
  if (!options) {
    return [];
  }
  const resolvedOptions = typeof options === 'function' ? options() : options;
  return resolvedOptions.map((item) => ({ ...item }));
}

export function getCachedDictOption(dictCode: string, value?: string) {
  if (!value) {
    return undefined;
  }
  const options = optionsCache.get(dictCode) ?? getDictFallbackOptions(dictCode);
  return options.find((item) => item.value === value);
}

export function formatDictOptionLabel(dictCode: string, value?: string) {
  if (!value) {
    return '-';
  }
  return getCachedDictOption(dictCode, value)?.label ?? value;
}

export function clearDictOptionsCache(dictCode?: string) {
  if (dictCode) {
    optionsCache.delete(dictCode);
    return;
  }
  optionsCache.clear();
}
