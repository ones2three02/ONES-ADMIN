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
}

export async function getPositionList() {
  const res = await requestClient.get<any[]>('/hr/positions');
  return res.map((item) => ({
    ...item,
    id: String(item.id),
    deptId: item.deptId ? String(item.deptId) : undefined,
  })) as HrPositionApi.HrPosition[];
}

export async function createPosition(data: HrPositionApi.SaveRequest) {
  return await requestClient.post<HrPositionApi.HrPosition>('/hr/positions', data);
}

export async function updatePosition(id: string | number, data: HrPositionApi.SaveRequest) {
  return await requestClient.put<HrPositionApi.HrPosition>(`/hr/positions/${id}`, data);
}
