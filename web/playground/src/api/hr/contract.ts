import { requestClient } from '#/api/request';

export namespace HrContractApi {
  export interface HrContract {
    id: string;
    employeeId: string;
    employeeNo?: string;
    employeeName?: string;
    contractNo: string;
    contractType: string;
    status: string;
    startDate: string;
    endDate?: string;
    probationMonths?: number;
    renewalRemindDate?: string;
    attachmentFileId?: string;
    remark?: string;
    createdAt?: string;
    updatedAt?: string;
  }

  export interface SaveRequest {
    contractNo: string;
    contractType: string;
    startDate: string;
    endDate?: string;
    probationMonths?: number;
    renewalRemindDate?: string;
    attachmentFileId?: string;
    remark?: string;
  }

  export interface TerminateRequest {
    actualTerminateDate: string;
    terminateReason: string;
  }
}

export async function getEmployeeContracts(employeeId: string | number) {
  const res = await requestClient.get<any[]>(`/hr/employees/${employeeId}/contracts`);
  return res.map((item) => ({
    ...item,
    id: String(item.id),
    employeeId: String(item.employeeId),
    attachmentFileId: item.attachmentFileId ? String(item.attachmentFileId) : undefined,
  })) as HrContractApi.HrContract[];
}

export async function getExpiringContracts(days?: number) {
  const res = await requestClient.get<any[]>('/hr/contracts/expiring', {
    params: { days },
  });
  return res.map((item) => ({
    ...item,
    id: String(item.id),
    employeeId: String(item.employeeId),
    attachmentFileId: item.attachmentFileId ? String(item.attachmentFileId) : undefined,
  })) as HrContractApi.HrContract[];
}

export async function createContract(employeeId: string | number, data: HrContractApi.SaveRequest) {
  return await requestClient.post<HrContractApi.HrContract>(`/hr/employees/${employeeId}/contracts`, data);
}

export async function updateContract(
  employeeId: string | number,
  contractId: string | number,
  data: HrContractApi.SaveRequest,
) {
  return await requestClient.put<HrContractApi.HrContract>(
    `/hr/employees/${employeeId}/contracts/${contractId}`,
    data,
  );
}

export async function terminateContract(contractId: string | number, data: HrContractApi.TerminateRequest) {
  return await requestClient.post<HrContractApi.HrContract>(`/hr/contracts/${contractId}/terminate`, data);
}
