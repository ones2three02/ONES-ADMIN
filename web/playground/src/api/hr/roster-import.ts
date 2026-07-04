import { requestClient } from '#/api/request';

export namespace HrRosterImportApi {
  export interface ImportBatch {
    id: string;
    batchNo: string;
    fileName: string;
    status: string;
    totalCount: number;
    successCount: number;
    failedCount: number;
    completedAt?: string;
    createdAt?: string;
  }

  export interface RosterError {
    id: string;
    batchId: string;
    rowNumber: number;
    employeeNo?: string;
    fieldName: string;
    errorMessage: string;
    rawJson?: string;
  }

  export interface BatchQuery {
    page: number;
    pageSize: number;
  }
}

export async function getRosterImportBatches(params: HrRosterImportApi.BatchQuery) {
  const res = await requestClient.get<{
    records: any[];
    total: number;
  }>('/hr/roster-import/batches', { params });
  return {
    items: res.records.map((item) => ({
      ...item,
      id: String(item.id),
    })) as HrRosterImportApi.ImportBatch[],
    total: res.total,
  };
}

export async function uploadRosterFile(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  const res = await requestClient.post<HrRosterImportApi.ImportBatch>(
    '/hr/roster-import/batches',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );
  return {
    ...res,
    id: String(res.id),
  } as HrRosterImportApi.ImportBatch;
}

export async function getRosterImportBatch(id: string | number) {
  const res = await requestClient.get<any>(`/hr/roster-import/batches/${id}`);
  return {
    ...res,
    id: String(res.id),
  } as HrRosterImportApi.ImportBatch;
}

export async function getRosterImportErrors(id: string | number) {
  const res = await requestClient.get<any[]>(`/hr/roster-import/batches/${id}/errors`);
  return res.map((item) => ({
    ...item,
    id: String(item.id),
    batchId: String(item.batchId),
  })) as HrRosterImportApi.RosterError[];
}

export async function downloadRosterTemplate() {
  return await requestClient.get<Blob>('/hr/roster-import/template', {
    responseType: 'blob',
  });
}
