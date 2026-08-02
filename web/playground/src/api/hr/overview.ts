import { requestClient } from '#/api/request';

export namespace HrOverviewApi {
  export interface MetricItem {
    code: string;
    name: string;
    value: number;
  }

  export interface Overview {
    employeeCount: number;
    activeEmployeeCount: number;
    probationEmployeeCount: number;
    resignedEmployeeCount: number;
    departmentCount: number;
    activeContractCount: number;
    expiringContractCount: number;
    expiringDocumentCount: number;
    expiredDocumentCount: number;
    probationDueCount: number;
    contractExpiringBefore: string;
    documentExpiringBefore: string;
    probationDueBefore: string;
    employmentStatusStats: MetricItem[];
    departmentStats: MetricItem[];
    lifecycleEventStats: MetricItem[];
    generatedAt: string;
  }
}

export async function getHrOverview() {
  return await requestClient.get<HrOverviewApi.Overview>('/hr/overview');
}
