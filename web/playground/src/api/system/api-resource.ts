import { requestClient } from '#/api/request';

export namespace SystemApiResourceApi {
  export interface ApiResource {
    accessPolicyExplicit: boolean;
    accessPolicyReason?: string;
    apiKey: string;
    authType: string;
    deprecated: boolean;
    handler: string;
    invalidPermissionCodes: string[];
    lifecycle: string;
    method: string;
    module: string;
    operationId: string;
    owner?: string;
    path: string;
    permissionAssignable: boolean;
    permissionCodeStandard: boolean;
    permissionCodes: string[];
    permissionMissing: boolean;
    permissionMode: string;
    permissionRegistered: boolean;
    repeatSubmitProtected: boolean;
    replacementApiKey?: string;
    requiresPermission: boolean;
    riskLevel: string;
    sinceVersion?: string;
    summary?: string;
    sunsetVersion?: string;
    unassignablePermissionCodes: string[];
    unregisteredPermissionCodes: string[];
    writeOperation: boolean;
  }

  export interface ApiResourceSummary {
    authTypes: Array<{ authType: string; count: number }>;
    deprecatedCount: number;
    explicitAccessPolicyCount: number;
    lifecycles: Array<{ count: number; lifecycle: string }>;
    modules: Array<{
      loginCount: number;
      module: string;
      permissionCount: number;
      permissionMissingCount: number;
      publicCount: number;
      total: number;
      writeOperationCount: number;
    }>;
    permissionMissingCount: number;
    riskLevels: Array<{ count: number; riskLevel: string }>;
    total: number;
    writeOperationCount: number;
  }

  export interface ApiResourceGovernance {
    apiVersionPattern: string;
    errorCount: number;
    operationIdPattern: string;
    passed: boolean;
    permissionCodePattern: string;
    total: number;
    violationCount: number;
    violations: ApiResourceViolation[];
    warningCount: number;
  }

  export interface ApiResourceViolation {
    message: string;
    method: string;
    module: string;
    operationId: string;
    path: string;
    remediation: string;
    ruleCode: string;
    severity: 'ERROR' | 'WARN';
    summary?: string;
  }
}

interface PageResult<T> {
  empty: boolean;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
  total: number;
}

interface ApiResourceListParams {
  authType?: string;
  lifecycle?: string;
  method?: string;
  module?: string;
  page?: number;
  pageNum?: number;
  pageSize?: number;
  path?: string;
  permissionMissing?: boolean;
  riskLevel?: string;
}

export async function getApiResourceList(params: ApiResourceListParams) {
  const response = await requestClient.get<
    PageResult<SystemApiResourceApi.ApiResource>
  >('/system/api-resources', {
    params: {
      ...params,
      pageNum: params.page ?? params.pageNum,
    },
  });

  return {
    items: response.list,
    total: response.total,
  };
}

export async function getApiResourceSummary() {
  return requestClient.get<SystemApiResourceApi.ApiResourceSummary>(
    '/system/api-resources/summary',
  );
}

export async function getApiResourceGovernance() {
  return requestClient.get<SystemApiResourceApi.ApiResourceGovernance>(
    '/system/api-resources/governance',
  );
}
