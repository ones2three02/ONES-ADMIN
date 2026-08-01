import { expect, test } from '@playwright/test';

import { authLogin } from './common/auth';

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 1536, height: 1024 });
  await page.goto('/');
});

test.describe('Auth Login Page Tests', () => {
  test('check title and page elements', async ({ page }) => {
    // 获取页面标题并断言标题包含当前项目品牌
    const title = await page.title();
    expect(title).toContain('ONES-ADMIN');
  });

  test('should present the reference login composition', async ({ page }) => {
    await expect(page.getByTestId('login-shell')).toBeVisible();
    await expect(page.getByTestId('brand-visual')).toBeVisible();
    await expect(page.getByTestId('login-card')).toBeVisible();
    await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible();
    await expect(page.getByText('新一代企业协同管理平台')).toBeVisible();

    for (const capability of [
      '高效协同',
      '流程驱动',
      '数据洞察',
      '安全可靠',
    ]) {
      await expect(page.getByText(capability, { exact: true })).toBeVisible();
    }
    for (const metric of ['20+', '100+', '10W+', '99.9%']) {
      await expect(page.getByText(metric, { exact: true })).toBeVisible();
    }

    const socialLoginList = page.getByTestId('social-login-list');
    await expect(socialLoginList.getByRole('button')).toHaveCount(3);
    for (const provider of ['飞书', '企业微信', 'Google']) {
      await expect(
        socialLoginList.getByText(provider, { exact: true }),
      ).toBeVisible();
    }
    await expect(
      page.getByText('安全加密登录，保障数据安全'),
    ).toBeVisible();

    await expect(page.locator(`div[name='captcha']`)).toHaveCount(0);
    await expect(
      page.locator('.enterprise-login-qrcode-corner'),
    ).toHaveCount(0);
    await expect(page.locator(`input[name='username']`)).toHaveAttribute(
      'autocomplete',
      'username',
    );
    await expect(page.locator(`input[name='password']`)).toHaveAttribute(
      'autocomplete',
      'current-password',
    );

    const cardRadius = await page
      .getByTestId('login-card')
      .evaluate((element) => Number.parseFloat(getComputedStyle(element).borderRadius));
    expect(cardRadius).toBeGreaterThanOrEqual(20);
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth),
    ).toBeLessThanOrEqual(1536);
  });

  // 测试用例: 成功登录
  test('should successfully login with valid credentials', async ({ page }) => {
    await authLogin(page);
  });

  test('should show trace id when login fails', async ({ page }) => {
    await page.locator(`input[name='username']`).fill('admin');
    await page.locator(`input[name='password']`).fill('wrong-password');
    await page.getByRole('button', { name: 'login' }).click();

    await expect(page.locator('.ant-message')).toContainText('用户名或密码错误');
    await expect(page.locator('.ant-message')).toContainText('追踪ID');
  });
});
