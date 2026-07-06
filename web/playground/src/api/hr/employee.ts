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
    sensitiveVisible?: boolean;
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
    page?: number;
    pageNum?: number;
    pageSize?: number;
    keyword?: string;
    deptId?: string;
    positionId?: string;
    gradeId?: string;
    employmentStatus?: string;
  }

  export interface CreateRequest {
    employeeNo: string;
    realName: string;
    preferredName?: string;
    gender: string;
    mobile: string;
    email: string;
    idCardNumber?: string;
    userId?: string;
    deptId: string;
    positionId?: string;
    gradeId?: string;
    managerEmployeeId?: string;
    employmentType: string;
    employmentStatus?: string;
    hireDate: string;
    probationEndDate?: string;
    remark?: string;
  }

  export interface UpdateRequest {
    realName: string;
    preferredName?: string;
    gender?: string;
    mobile?: string;
    email?: string;
    idCardNumber?: string;
    userId?: string;
    probationEndDate?: string;
    remark?: string;
  }

  export interface TransferRequest {
    deptId: string;
    positionId?: string;
    gradeId?: string;
    managerEmployeeId?: string;
    employmentType?: string;
    effectiveDate: string;
    changeReason: string;
  }

  export interface RegularizeRequest {
    regularizeDate: string;
    remark?: string;
  }

  export interface ResignRequest {
    leaveDate: string;
    resignationReason: string;
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

  export interface EmployeeJob {
    id: string;
    employeeId: string;
    deptId?: string;
    deptName?: string;
    positionId?: string;
    positionName?: string;
    gradeId?: string;
    gradeName?: string;
    managerEmployeeId?: string;
    managerName?: string;
    employmentType: string;
    effectiveDate: string;
    endDate?: string;
    changeReason?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface OrgEmployeeNode {
    id: string;
    employeeNo: string;
    realName: string;
    deptId?: string;
    deptName?: string;
    positionId?: string;
    positionName?: string;
    gradeId?: string;
    gradeName?: string;
    employmentStatus: string;
  }

  export interface DeptNode {
    id: string;
    name: string;
  }

  export interface EmployeeOrgContext {
    employeeId: string;
    deptId?: string;
    deptName?: string;
    deptPath: DeptNode[];
    manager?: OrgEmployeeNode;
    current: OrgEmployeeNode;
    directReports: OrgEmployeeNode[];
    directReportCount: number;
  }
}

export async function getEmployeeList(params: HrEmployeeApi.EmployeeQuery) {
  const res = await requestClient.get<{
    list: any[];
    total: number;
  }>('/hr/employees', { params });
  return {
    items: res.list.map((item) => ({
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

export async function updateEmployee(id: string | number, data: HrEmployeeApi.UpdateRequest) {
  return await requestClient.put<HrEmployeeApi.HrEmployee>(`/hr/employees/${id}`, data);
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

export async function getEmployeeJobs(id: string | number) {
  const res = await requestClient.get<HrEmployeeApi.EmployeeJob[]>(`/hr/employees/${id}/jobs`);
  return res.map((item) => ({
    ...item,
    id: String(item.id),
    employeeId: String(item.employeeId),
    deptId: item.deptId ? String(item.deptId) : undefined,
    gradeId: item.gradeId ? String(item.gradeId) : undefined,
    managerEmployeeId: item.managerEmployeeId
      ? String(item.managerEmployeeId)
      : undefined,
    positionId: item.positionId ? String(item.positionId) : undefined,
  }));
}

function normalizeOrgEmployeeNode(node?: HrEmployeeApi.OrgEmployeeNode) {
  if (!node) {
    return undefined;
  }
  return {
    ...node,
    deptId: node.deptId ? String(node.deptId) : undefined,
    gradeId: node.gradeId ? String(node.gradeId) : undefined,
    id: String(node.id),
    positionId: node.positionId ? String(node.positionId) : undefined,
  };
}

export async function getEmployeeOrgContext(id: string | number) {
  const res = await requestClient.get<HrEmployeeApi.EmployeeOrgContext>(
    `/hr/employees/${id}/org-context`,
  );
  return {
    ...res,
    current: normalizeOrgEmployeeNode(res.current)!,
    deptId: res.deptId ? String(res.deptId) : undefined,
    deptPath: res.deptPath.map((item) => ({
      ...item,
      id: String(item.id),
    })),
    directReports: res.directReports.map((item) => normalizeOrgEmployeeNode(item)!),
    employeeId: String(res.employeeId),
    manager: normalizeOrgEmployeeNode(res.manager),
  };
}

export async function exportEmployees(params: HrEmployeeApi.EmployeeQuery) {
  return await requestClient.get<Blob>('/hr/employees/export', {
    params,
    responseType: 'blob',
  });
}
