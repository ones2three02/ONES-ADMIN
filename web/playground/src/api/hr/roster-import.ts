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
    page?: number;
    pageNum?: number;
    pageSize?: number;
  }

  export type ImportBatchResponse = Omit<ImportBatch, 'id'> & {
    id: number | string;
  };

  export interface ImportBatchPageResponse {
    list: ImportBatchResponse[];
    total: number;
  }

  export type RosterErrorResponse = Omit<RosterError, 'batchId' | 'id'> & {
    batchId: number | string;
    id: number | string;
  };
}

function normalizeImportBatch(
  item: HrRosterImportApi.ImportBatchResponse,
): HrRosterImportApi.ImportBatch {
  return {
    ...item,
    id: String(item.id),
  };
}

function normalizeRosterError(
  item: HrRosterImportApi.RosterErrorResponse,
): HrRosterImportApi.RosterError {
  return {
    ...item,
    batchId: String(item.batchId),
    id: String(item.id),
  };
}

export async function getRosterImportBatches(params: HrRosterImportApi.BatchQuery) {
  const res = await requestClient.get<HrRosterImportApi.ImportBatchPageResponse>(
    '/hr/roster-import/batches',
    {
    params: {
      ...params,
      pageNum: params.page ?? params.pageNum,
    },
    },
  );
  return {
    items: res.list.map(normalizeImportBatch),
    total: res.total,
  };
}

export async function uploadRosterFile(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  const res = await requestClient.post<HrRosterImportApi.ImportBatchResponse>(
    '/hr/roster-import/batches',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );
  return normalizeImportBatch(res);
}

export async function getRosterImportBatch(id: string | number) {
  const res = await requestClient.get<HrRosterImportApi.ImportBatchResponse>(
    `/hr/roster-import/batches/${id}`,
  );
  return normalizeImportBatch(res);
}

export async function getRosterImportErrors(id: string | number) {
  const res = await requestClient.get<HrRosterImportApi.RosterErrorResponse[]>(
    `/hr/roster-import/batches/${id}/errors`,
  );
  return res.map(normalizeRosterError);
}

export async function downloadRosterTemplate() {
  return await requestClient.get<Blob>('/hr/roster-import/template', {
    responseType: 'blob',
  });
}
