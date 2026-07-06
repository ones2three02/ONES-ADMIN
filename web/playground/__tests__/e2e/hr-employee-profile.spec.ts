import type { Page } from '@playwright/test';

import { expect, test } from '@playwright/test';

import { authLogin } from './common/auth';

async function createEmployeeFixture(page: Page) {
  const loginResponse = await page.request.post('/api/auth/login', {
    data: {
      password: 'admin123',
      username: 'admin',
    },
  });
  expect(loginResponse.ok()).toBeTruthy();
  const loginResult = await loginResponse.json();
  const token = loginResult.data.token.tokenValue;
  const employeeNo = `E2E${Date.now()}`;
  const response = await page.request.post('/api/hr/employees', {
    data: {
      deptId: 1,
      email: `${employeeNo.toLowerCase()}@ones.local`,
      employeeNo,
      employmentStatus: 'PROBATION',
      employmentType: 'FULL_TIME',
      gender: 'MALE',
      hireDate: '2026-07-01',
      mobile: '13800138000',
      probationEndDate: '2026-10-01',
      realName: `档案测试员工${employeeNo}`,
      remark: '员工档案 E2E 专用数据',
    },
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  expect(response.ok()).toBeTruthy();
  return employeeNo;
}

test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await authLogin(page);
});

test.describe('HRMS Employee Profile', () => {
  test('should open employee profile drawer from employee list', async ({ page }) => {
    const employeeNo = await createEmployeeFixture(page);

    await page.goto('/hr/employee');

    await expect(page.getByText(employeeNo, { exact: true })).toBeVisible();
    await page.getByRole('button', { name: /档案/ }).first().click();

    await expect(page.getByText(/员工档案 -/)).toBeVisible();
    const profileDrawer = page.getByLabel(`员工档案 - 档案测试员工${employeeNo}`);
    await expect(
      profileDrawer.getByText(employeeNo, { exact: true }),
    ).toBeVisible();
    await expect(page.getByRole('tab', { name: '基础信息' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '合同' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '任职记录' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '生命周期' })).toBeVisible();

    await page.getByRole('tab', { name: '任职记录' }).click();
    await expect(
      profileDrawer.getByText('员工入职初始化任职记录'),
    ).toBeVisible();
  });
});
