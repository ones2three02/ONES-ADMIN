import type { Page } from '@playwright/test';

import { expect } from '@playwright/test';

export async function authLogin(page: Page) {
  const usernameInput = page.locator(`input[name='username']`);
  const passwordInput = page.locator(`input[name='password']`);
  await expect(usernameInput).toBeVisible();
  await expect(passwordInput).toBeVisible();
  await usernameInput.fill('admin');
  await passwordInput.fill('admin123');
  await page.getByRole('button', { name: 'login' }).click();
  await expect(page).toHaveURL(/\/dashboard\/overview/);
}
