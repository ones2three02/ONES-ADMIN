import { requestClient } from '#/api/request';

interface PageResult<T> {
  empty: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

export namespace SystemAuditApi {
  export interface LoginLog {
    createdAt: string;
    failureReason?: string;
    id: string;
    ip?: string;
    success: boolean;
    traceId?: string;
    userAgent?: string;
    userId?: string;
    username: string;
  }

  export interface OperationLog {
    createdAt: string;
    durationMs?: number;
    errorMessage?: string;
    id: string;
    ip?: string;
    method: string;
    module?: string;
    operation?: string;
    path: string;
    permissionCode?: string;
    responseCode?: number;
    success: boolean;
    traceId?: string;
    userAgent?: string;
    userId?: string;
  }

  export interface LoginLogQuery {
    endTime?: string;
    ip?: string;
    page?: number;
    pageNum?: number;
    pageSize?: number;
    startTime?: string;
    success?: boolean;
    username?: string;
  }

  export interface OperationLogQuery {
    endTime?: string;
    method?: string;
    page?: number;
    pageNum?: number;
    pageSize?: number;
    path?: string;
    responseCode?: number;
    startTime?: string;
    success?: boolean;
    traceId?: string;
    userId?: string;
  }

  export interface AuditRetentionSummary {
    expiredLoginLogCount: number;
    expiredOperationLogCount: number;
    loginLogExpireBefore: string;
    loginLogRetentionDays: number;
    operationLogExpireBefore: string;
    operationLogRetentionDays: number;
  }

  export interface AuditRetentionCleanup {
    deletedLoginLogCount: number;
    deletedOperationLogCount: number;
    loginLogExpireBefore: string;
    loginLogRetentionDays: number;
    operationLogExpireBefore: string;
    operationLogRetentionDays: number;
  }
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

export async function getLoginLogList(params: SystemAuditApi.LoginLogQuery) {
  const response = await requestClient.get<PageResult<SystemAuditApi.LoginLog>>(
    '/system/audit/login-logs',
    {
      params: normalizePageParams(params),
    },
  );
  return normalizePage(response);
}

export async function getOperationLogList(params: SystemAuditApi.OperationLogQuery) {
  const response = await requestClient.get<PageResult<SystemAuditApi.OperationLog>>(
    '/system/audit/operation-logs',
    {
      params: normalizePageParams(params),
    },
  );
  return normalizePage(response);
}

export async function getAuditRetention() {
  return requestClient.get<SystemAuditApi.AuditRetentionSummary>(
    '/system/audit/retention',
  );
}

export async function cleanupExpiredAuditLogs() {
  return requestClient.post<SystemAuditApi.AuditRetentionCleanup>(
    '/system/audit/retention/cleanup',
  );
}

export async function exportLoginLogs(params: SystemAuditApi.LoginLogQuery) {
  return requestClient.get<Blob>('/system/audit/login-logs/export', {
    params: normalizePageParams(params),
    responseType: 'blob',
  });
}

export async function exportOperationLogs(params: SystemAuditApi.OperationLogQuery) {
  return requestClient.get<Blob>('/system/audit/operation-logs/export', {
    params: normalizePageParams(params),
    responseType: 'blob',
  });
}
