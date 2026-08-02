import { requestClient } from '#/api/request';

export namespace HrPositionApi {
  export interface HrPosition {
    id: string;
    positionCode: string;
    positionName: string;
    deptId?: string;
    deptName?: string;
    description?: string;
    enabled: boolean;
    createdAt?: string;
  }

  export interface SaveRequest {
    positionCode: string;
    positionName: string;
    deptId?: string;
    description?: string;
    enabled: boolean;
  }

  export type PositionResponse = Omit<HrPosition, 'deptId' | 'id'> & {
    deptId?: number | string;
    id: number | string;
  };
}

function normalizePosition(item: HrPositionApi.PositionResponse): HrPositionApi.HrPosition {
  return {
    ...item,
    deptId: item.deptId ? String(item.deptId) : undefined,
    id: String(item.id),
  };
}

export async function getPositionList() {
  const res = await requestClient.get<HrPositionApi.PositionResponse[]>('/hr/positions');
  return res.map(normalizePosition);
}

export async function createPosition(data: HrPositionApi.SaveRequest) {
  return await requestClient.post<HrPositionApi.HrPosition>('/hr/positions', data);
}

export async function updatePosition(id: string | number, data: HrPositionApi.SaveRequest) {
  return await requestClient.put<HrPositionApi.HrPosition>(`/hr/positions/${id}`, data);
}
