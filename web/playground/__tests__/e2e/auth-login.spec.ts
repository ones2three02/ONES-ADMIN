import { expect, test } from '@playwright/test';

import { authLogin, completeSliderCaptcha } from './common/auth';

test.beforeEach(async ({ page }) => {
  await page.goto('/');
});

test.describe('Auth Login Page Tests', () => {
  test('check title and page elements', async ({ page }) => {
    // 获取页面标题并断言标题包含当前项目品牌
    const title = await page.title();
    expect(title).toContain('ONES-ADMIN');
  });

  test('should present enterprise login surface', async ({ page }) => {
    await expect(page.locator('.enterprise-login-brand')).toContainText(
      'ONES-ADMIN',
    );
    await expect(page.getByRole('heading', { name: '安全登录' })).toBeVisible();
    await expect(page.getByText('统一认证', { exact: true })).toHaveCount(0);
    await expect(page.getByText('锁定保护', { exact: true })).toHaveCount(0);
    await expect(page.getByText('审计追踪', { exact: true })).toHaveCount(0);
    await expect(page.getByText('连续失败将触发临时锁定')).toHaveCount(0);
    await expect(page.getByText('快速选择账号', { exact: true })).toHaveCount(0);
    await expect(page.getByText('创建账号', { exact: true })).toHaveCount(0);
    await expect(page.getByText('其他登录方式', { exact: true })).toHaveCount(0);
  });

  test('should present compact icon-only login methods', async ({ page }) => {
    const qrcodeCorner = page.locator('.enterprise-login-qrcode-corner');
    await expect(qrcodeCorner).toHaveAttribute('href', '/auth/qrcode-login');

    const methodEntries = page.locator(
      '.enterprise-login-methods .enterprise-login-icon-button',
    );
    await expect(methodEntries).toHaveCount(3);
    await expect(page.getByText('飞书', { exact: true })).toHaveCount(0);
    await expect(page.getByText('扫码', { exact: true })).toHaveCount(0);
    await expect(page.getByText('手机', { exact: true })).toHaveCount(0);

    for (let index = 0; index < 3; index += 1) {
      const radius = await methodEntries
        .nth(index)
        .evaluate((element) => getComputedStyle(element).borderRadius);
      expect(Number.parseFloat(radius)).toBeGreaterThanOrEqual(20);
    }
  });

  // 测试用例: 成功登录
  test('should successfully login with valid credentials', async ({ page }) => {
    await authLogin(page);
  });

  test('should show trace id when login fails', async ({ page }) => {
    await page.locator(`input[name='username']`).fill('admin');
    await page.locator(`input[name='password']`).fill('wrong-password');
    await completeSliderCaptcha(page);

    await page.waitForTimeout(300);
    await page.getByRole('button', { name: 'login' }).click();

    await expect(page.locator('.ant-message')).toContainText('用户名或密码错误');
    await expect(page.locator('.ant-message')).toContainText('追踪ID');
  });
});
