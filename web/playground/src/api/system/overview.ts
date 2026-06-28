import { requestClient } from '#/api/request';

export namespace SystemOverviewApi {
  export interface SystemOverview {
    deptCount: number;
    enabledUserCount: number;
    generatedAt: string;
    menuCount: number;
    roleCount: number;
    userCount: number;
  }
}

async function getSystemOverview() {
  return requestClient.get<SystemOverviewApi.SystemOverview>(
    '/system/overview',
  );
}

export { getSystemOverview };
