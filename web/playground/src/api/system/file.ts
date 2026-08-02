import { requestClient } from '#/api/request';

interface PageResult<T> {
  empty: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export function normalizeFileMetadata<T extends { id: number | string; uploadedBy?: number | string }>(
  item: T,
) {
  return {
    ...item,
    id: String(item.id),
    uploadedBy:
      item.uploadedBy === undefined || item.uploadedBy === null
        ? undefined
        : String(item.uploadedBy),
  };
}

function normalizePage<T extends { id: number | string; uploadedBy?: number | string }>(
  response: PageResult<T>,
) {
  return {
    items: response.list.map((item) => normalizeFileMetadata(item)),
    total: response.total,
  };
}

function normalizePageParams<T extends { page?: number; pageNum?: number }>(params: T) {
  return {
    ...params,
    pageNum: params.page ?? params.pageNum,
  };
}

export namespace SystemFileApi {
  export interface FileMetadata {
    bucket?: string;
    businessId?: string;
    businessType?: string;
    contentType?: string;
    createdAt?: string;
    deletedAt?: string;
    extension: string;
    id: string;
    originalName: string;
    sizeBytes: number;
    status: 'ACTIVE' | 'DELETED' | 'PURGED' | string;
    storageType: 'LOCAL' | 'MINIO' | string;
    storedName?: string;
    updatedAt?: string;
    uploadedBy?: string;
    url: string;
  }

  export interface FileMetadataQuery {
    businessId?: string;
    businessType?: string;
    endTime?: string;
    extension?: string;
    originalName?: string;
    page?: number;
    pageNum?: number;
    pageSize?: number;
    startTime?: string;
    status?: string;
    storageType?: string;
    uploadedBy?: string;
  }

  export interface FileUploadResponse extends FileMetadata {
    url: string;
  }

  export interface FileRetentionSummary {
    deletedFileRetentionDays: number;
    expiredDeletedFileCount: number;
    expiredDeletedFileSizeBytes: number;
    purgeBefore?: string;
  }

  export interface FileRetentionPurgeResult {
    deletedFileRetentionDays: number;
    purgedFileCount: number;
    purgedFileSizeBytes: number;
    purgeBefore?: string;
  }
}

export async function getFileMetadataList(params: SystemFileApi.FileMetadataQuery) {
  const response = await requestClient.get<PageResult<SystemFileApi.FileMetadata>>(
    '/system/files',
    {
      params: normalizePageParams(params),
    },
  );
  return normalizePage(response);
}

export async function getFileMetadata(id: number | string) {
  const response = await requestClient.get<SystemFileApi.FileMetadata>(
    `/system/files/${id}/metadata`,
  );
  return normalizeFileMetadata(response);
}

export async function downloadSystemFile(metadata: Pick<SystemFileApi.FileMetadata, 'storedName'>) {
  if (!metadata.storedName) {
    throw new Error('文件存储标识缺失');
  }
  return requestClient.get<Blob>(
    `/system/files/${encodeURIComponent(metadata.storedName)}`,
    {
      responseReturn: 'body',
      responseType: 'blob',
    },
  );
}

export async function openSystemFile(metadata: SystemFileApi.FileMetadata) {
  const previewWindow = window.open('', '_blank');
  try {
    const blob = await downloadSystemFile(metadata);
    const url = window.URL.createObjectURL(blob);
    if (previewWindow) {
      previewWindow.opener = null;
      previewWindow.location.href = url;
    } else {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
    window.setTimeout(() => {
      window.URL.revokeObjectURL(url);
    }, 60_000);
  } catch (error) {
    previewWindow?.close();
    throw error;
  }
}

export async function uploadSystemFile(file: File) {
  const response = await requestClient.upload<SystemFileApi.FileUploadResponse>(
    '/system/files/upload',
    { file },
  );
  return normalizeFileMetadata(response);
}

export async function deleteSystemFile(id: string) {
  const response = await requestClient.delete<SystemFileApi.FileMetadata>(
    `/system/files/${id}`,
  );
  return normalizeFileMetadata(response);
}

export async function getFileRetention() {
  return requestClient.get<SystemFileApi.FileRetentionSummary>(
    '/system/files/retention',
  );
}

export async function purgeExpiredDeletedFiles() {
  return requestClient.post<SystemFileApi.FileRetentionPurgeResult>(
    '/system/files/retention/purge',
  );
}
