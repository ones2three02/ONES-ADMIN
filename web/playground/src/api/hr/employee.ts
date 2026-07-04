import { requestClient } from '#/api/request';

export namespace HrEmployeeApi {
  export interface HrEmployee {
    id: string;
    employeeNo: string;
    realName: string;
    preferredName?: string;
    gender: string;
    mobile: string;
    email: string;
    idCardMasked?: string;
    userId?: string;
    deptId: string;
    deptName?: string;
    positionId?: string;
    positionName?: string;
    gradeId?: string;
    gradeName?: string;
    managerEmployeeId?: string;
    managerName?: string;
    employmentType: string;
    employmentStatus: string;
    hireDate: string;
    probationEndDate?: string;
    leaveDate?: string;
    remark?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface EmployeeQuery {
    page: number;
    pageSize: number;
    realName?: string;
    employeeNo?: string;
    deptId?: string;
    employmentStatus?: string;
  }

  export interface CreateRequest {
    realName: string;
    preferredName?: string;
    gender: string;
    mobile: string;
    email: string;
    idCard?: string;
    deptId: string;
    positionId?: string;
    gradeId?: string;
    managerEmployeeId?: string;
    employmentType: string;
    hireDate: string;
    probationMonths?: number;
    remark?: string;
  }

  export interface TransferRequest {
    deptId: string;
    positionId?: string;
    gradeId?: string;
    managerEmployeeId?: string;
    effectiveDate: string;
    changeReason: string;
  }

  export interface RegularizeRequest {
    actualRegularizeDate: string;
    remark?: string;
  }

  export interface ResignRequest {
    leaveDate: string;
    reason: string;
  }

  export interface LifecycleEvent {
    id: string;
    employeeId: string;
    eventType: string;
    eventDate: string;
    beforeStatus?: string;
    afterStatus?: string;
    summary: string;
    detailJson?: string;
    createdByName?: string;
    createdAt: string;
  }
}

export async function getEmployeeList(params: HrEmployeeApi.EmployeeQuery) {
  const res = await requestClient.get<{
    records: any[];
    total: number;
  }>('/hr/employees', { params });
  return {
    items: res.records.map((item) => ({
      ...item,
      id: String(item.id),
      deptId: String(item.deptId),
      positionId: item.positionId ? String(item.positionId) : undefined,
      gradeId: item.gradeId ? String(item.gradeId) : undefined,
      managerEmployeeId: item.managerEmployeeId ? String(item.managerEmployeeId) : undefined,
      userId: item.userId ? String(item.userId) : undefined,
    })),
    total: res.total,
  };
}

export async function getEmployee(id: string | number) {
  return await requestClient.get<HrEmployeeApi.HrEmployee>(`/hr/employees/${id}`);
}

export async function createEmployee(data: HrEmployeeApi.CreateRequest) {
  return await requestClient.post<HrEmployeeApi.HrEmployee>('/hr/employees', data);
}

export async function transferEmployee(id: string | number, data: HrEmployeeApi.TransferRequest) {
  return await requestClient.post<HrEmployeeApi.HrEmployee>(`/hr/employees/${id}/transfer`, data);
}

export async function regularizeEmployee(id: string | number, data: HrEmployeeApi.RegularizeRequest) {
  return await requestClient.post<HrEmployeeApi.HrEmployee>(`/hr/employees/${id}/regularize`, data);
}

export async function resignEmployee(id: string | number, data: HrEmployeeApi.ResignRequest) {
  return await requestClient.post<HrEmployeeApi.HrEmployee>(`/hr/employees/${id}/resign`, data);
}

export async function getEmployeeLifecycleEvents(id: string | number) {
  return await requestClient.get<HrEmployeeApi.LifecycleEvent[]>(`/hr/employees/${id}/lifecycle-events`);
}
