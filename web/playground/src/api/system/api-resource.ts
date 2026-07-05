import { requestClient } from '#/api/request';

export namespace SystemApiResourceApi {
  export type ApiResourceActionCategory =
    | 'API_GOVERNANCE'
    | 'AUDIT'
    | 'CATALOG'
    | 'CONTRACT'
    | 'DOCUMENTATION'
    | 'HRMS'
    | 'LIFECYCLE'
    | 'PERMISSION'
    | 'RELEASE'
    | 'SECURITY'
    | 'UNKNOWN';

  export type ApiResourceQualityCategory =
    | 'AUDIT'
    | 'CATALOG'
    | 'CONTRACT'
    | 'DOCUMENTATION'
    | 'LIFECYCLE'
    | 'SECURITY';

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
    operationAuditProtected: boolean;
    operationId: string;
    owner?: string;
    audience?: string;
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
    audiences: Array<{
      audience: string;
      permissionCount: number;
      publicCount: number;
      total: number;
      writeOperationCount: number;
    }>;
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
    owners: Array<{
      highRiskCount: number;
      owner: string;
      permissionMissingCount: number;
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
    categorySummaries: ApiResourceGovernanceCategorySummary[];
    errorCount: number;
    operationIdPattern: string;
    passed: boolean;
    permissionCodePattern: string;
    ruleSummaries: ApiResourceGovernanceRuleSummary[];
    total: number;
    violationCount: number;
    violations: ApiResourceViolation[];
    warningCount: number;
  }

  export interface ApiResourceGovernanceRuleSummary {
    blocking: boolean;
    category: string;
    count: number;
    description: string;
    remediation: string;
    ruleCode: string;
    severity: 'ERROR' | 'INFO' | 'WARN';
  }

  export interface ApiResourceGovernanceCategorySummary {
    category: string;
    errorCount: number;
    violationCount: number;
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

  export interface ApiResourceGovernanceRule {
    blocking: boolean;
    category: string;
    description: string;
    remediation: string;
    ruleCode: string;
    severity: 'ERROR' | 'INFO' | 'WARN';
  }

  export interface ApiResourceGovernanceRules {
    apiVersionPattern: string;
    operationIdPattern: string;
    permissionCodePattern: string;
    rules: ApiResourceGovernanceRule[];
  }

  export interface ApiResourceManifest {
    applicationVersion: string;
    checksum: string;
    checksumAlgorithm: string;
    resources: ApiResourceManifestResource[];
    total: number;
  }

  export interface ApiResourceManifestResource {
    apiKey: string;
    authType: string;
    deprecated: boolean;
    handler: string;
    lifecycle: string;
    method: string;
    operationAuditProtected: boolean;
    operationId: string;
    owner?: string;
    audience?: string;
    path: string;
    permissionCodes: string[];
    permissionMode?: string;
    repeatSubmitProtected: boolean;
    replacementApiKey?: string;
    riskLevel: string;
    sinceVersion?: string;
    sunsetVersion?: string;
    writeOperation: boolean;
  }

  export interface ApiResourceManifestGate {
    breakingChangeCount: number;
    checks: ApiResourceManifestGateCheck[];
    currentVersion?: string;
    governanceErrorCount: number;
    governanceWarningCount: number;
    passed: boolean;
    previousVersion?: string;
    reasons: string[];
    requiredManualReview: boolean;
    reviewReasonRequired: boolean;
    status: string;
  }

  export interface ApiResourceManifestGateCheck {
    blocking: boolean;
    checkCode: string;
    message: string;
    passed: boolean;
    remediation: string;
    severity: 'ERROR' | 'INFO' | 'WARN';
  }

  export interface ApiResourceManifestLatestGate {
    baselineAvailable: boolean;
    baselineChecksum?: string;
    baselineSnapshotId?: number;
    baselineVersion?: string;
    gate: ApiResourceManifestGate;
  }

  export interface ApiResourceReferenceBenchmark {
    category: 'API_CATALOG' | 'API_GATEWAY' | 'API_MANAGEMENT' | 'HRMS';
    lesson: string;
    project: string;
    url: string;
  }

  export interface ApiResourceRecommendedAction {
    actionCode: string;
    category: 'API_GOVERNANCE' | 'HRMS' | 'PERMISSION' | 'RELEASE';
    description: string;
    priority: 'P0' | 'P1' | 'P2';
    title: string;
    verification: string;
  }

  export interface ApiResourceActionItem {
    actionCode: string;
    apiKey: string;
    blocking: boolean;
    category: ApiResourceActionCategory;
    description: string;
    module: string;
    owner: string;
    priority: 'P0' | 'P1' | 'P2';
    sourceCode: string;
    sourceType: 'GATE' | 'GATE_CHECK' | 'GOVERNANCE_RULE' | 'ROADMAP';
    status: 'DONE' | 'OPEN';
    title: string;
    verification: string;
  }

  export interface ApiResourceOwnerActionSummary {
    blockingActionCount: number;
    categories: ApiResourceActionCategory[];
    nextActionCode?: string;
    nextActionTitle?: string;
    openActionCount: number;
    owner: string;
    p0ActionCount: number;
    p1ActionCount: number;
    p2ActionCount: number;
    priority: 'P0' | 'P1' | 'P2';
    recommendation: string;
    status: 'BLOCKED' | 'DONE' | 'TRACKING';
    totalActionCount: number;
  }

  export interface ApiResourceReleaseReadiness {
    baselineAvailable: boolean;
    blockingActionCount: number;
    blockingCheckCount: number;
    gateStatus?: string;
    message: string;
    nextActionCode?: string;
    nextActionTitle?: string;
    openActionCount: number;
    priority: 'P0' | 'P1' | 'P2';
    qualityScore: number;
    ready: boolean;
    status:
      | 'BASELINE_REQUIRED'
      | 'BLOCKED'
      | 'MANUAL_REVIEW_REQUIRED'
      | 'READY'
      | 'READY_WITH_WARNINGS'
      | 'UNKNOWN';
  }

  export interface ApiResourceQualityDimension {
    benchmark: string;
    category: ApiResourceQualityCategory;
    dimensionCode: ApiResourceQualityCategory;
    errorCount: number;
    passed: boolean;
    recommendation: string;
    score: number;
    title: string;
    violationCount: number;
    warningCount: number;
  }

  export interface ApiResourceGovernanceReport {
    applicationVersion: string;
    actionItems: ApiResourceActionItem[];
    generatedAt: string;
    governance: ApiResourceGovernance;
    latestGate: ApiResourceManifestLatestGate;
    manifest: ApiResourceManifest;
    ownerActionSummaries: ApiResourceOwnerActionSummary[];
    qualityDimensions: ApiResourceQualityDimension[];
    qualityScore: number;
    recommendedActions: ApiResourceRecommendedAction[];
    referenceBenchmarks: ApiResourceReferenceBenchmark[];
    releaseReadiness: ApiResourceReleaseReadiness;
    rules: ApiResourceGovernanceRules;
    summary: ApiResourceSummary;
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
  audience?: string;
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

export async function getApiResourceGovernanceReport() {
  return requestClient.get<SystemApiResourceApi.ApiResourceGovernanceReport>(
    '/system/api-resources/governance/report',
  );
}
