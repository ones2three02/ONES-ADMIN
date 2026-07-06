import { requestClient } from '#/api/request';

interface PageResult<T> {
  list: T[];
  total: number;
}

function normalizePage<T extends { id: number | string }>(response: PageResult<T>) {
  return {
    items: response.list.map((item) => ({
      ...item,
      id: String(item.id),
    })),
    total: response.total,
  };
}

function normalizePageParams<T extends { page?: number; pageNum?: number }>(params: T) {
  return {
    ...params,
    pageNum: params.page ?? params.pageNum,
  };
}

export namespace SystemDictApi {
  export interface DictType {
    createdAt?: string;
    dictCode: string;
    dictName: string;
    enabled: boolean;
    id: string;
    itemCount: number;
    remark?: string;
    sortOrder: number;
    updatedAt?: string;
  }

  export interface DictItem {
    color?: string;
    createdAt?: string;
    dictCode: string;
    enabled: boolean;
    id: string;
    itemLabel: string;
    itemValue: string;
    remark?: string;
    sortOrder: number;
    typeId: string;
    updatedAt?: string;
  }

  export interface DictOption {
    color?: string;
    label: string;
    sortOrder: number;
    value: string;
  }

  export interface DictTypeQuery {
    dictCode?: string;
    dictName?: string;
    enabled?: boolean;
    page?: number;
    pageNum?: number;
    pageSize?: number;
  }

  export interface DictItemQuery {
    dictCode?: string;
    enabled?: boolean;
    itemLabel?: string;
    itemValue?: string;
    page?: number;
    pageNum?: number;
    pageSize?: number;
    typeId?: string;
  }

  export interface DictTypeSaveRequest {
    dictCode: string;
    dictName: string;
    enabled: boolean;
    remark?: string;
    sortOrder?: number;
  }

  export interface DictItemSaveRequest {
    color?: string;
    dictCode?: string;
    enabled: boolean;
    itemLabel: string;
    itemValue: string;
    remark?: string;
    sortOrder?: number;
    typeId?: string;
  }
}

export async function getDictTypeList(params: SystemDictApi.DictTypeQuery) {
  const response = await requestClient.get<PageResult<SystemDictApi.DictType>>(
    '/system/dicts/types',
    {
      params: normalizePageParams(params),
    },
  );
  return normalizePage(response);
}

export async function createDictType(data: SystemDictApi.DictTypeSaveRequest) {
  return requestClient.post<SystemDictApi.DictType>('/system/dicts/types', data);
}

export async function updateDictType(
  id: string,
  data: SystemDictApi.DictTypeSaveRequest,
) {
  return requestClient.put<SystemDictApi.DictType>(
    `/system/dicts/types/${id}`,
    data,
  );
}

export async function deleteDictType(id: string) {
  return requestClient.delete(`/system/dicts/types/${id}`);
}

export async function getDictItemList(params: SystemDictApi.DictItemQuery) {
  const response = await requestClient.get<PageResult<SystemDictApi.DictItem>>(
    '/system/dicts/items',
    {
      params: normalizePageParams(params),
    },
  );
  return normalizePage(response);
}

export async function createDictItem(data: SystemDictApi.DictItemSaveRequest) {
  return requestClient.post<SystemDictApi.DictItem>('/system/dicts/items', data);
}

export async function updateDictItem(
  id: string,
  data: SystemDictApi.DictItemSaveRequest,
) {
  return requestClient.put<SystemDictApi.DictItem>(
    `/system/dicts/items/${id}`,
    data,
  );
}

export async function deleteDictItem(id: string) {
  return requestClient.delete(`/system/dicts/items/${id}`);
}

export async function getDictOptions(dictCode: string) {
  return requestClient.get<SystemDictApi.DictOption[]>(
    `/system/dicts/${dictCode}/options`,
  );
}
