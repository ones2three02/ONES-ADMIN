import { expect, test } from '@playwright/test';

import { authLogin } from './common/auth';

test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await authLogin(page);
});

test.describe('System API Resources Page', () => {
  test('should show API governance summary and resource list', async ({
    page,
  }) => {
    await page.goto('/system/api-resources');

    const pageContent = page.getByTestId('api-resource-page');
    await expect(pageContent.getByText('接口管理', { exact: true })).toBeVisible();
    await expect(pageContent.getByText('治理状态', { exact: true })).toBeVisible();
    await expect(pageContent.getByText('接口总数', { exact: true })).toBeVisible();
    await expect(
      pageContent.getByText('接口资源清单', { exact: true }),
    ).toBeVisible();
    await expect(pageContent.getByText('发布门禁', { exact: true })).toBeVisible();
    await expect(pageContent.getByText('治理规则', { exact: true })).toBeVisible();
    await expect(pageContent.getByText('API_GOVERNANCE_ERROR')).toBeVisible();
    await expect(
      pageContent.getByText('/api/system/api-resources', { exact: true }),
    ).toBeVisible();
    await expect(
      pageContent.locator('thead').getByText('调用方', { exact: true }),
    ).toBeVisible();
    await expect(
      pageContent.locator('tbody').getByText('ADMIN_PORTAL').first(),
    ).toBeVisible();
  });
});
