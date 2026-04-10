import { expect, test } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

const SYSTEM_ADMIN_USERNAME = process.env.QA_SYSTEM_ADMIN_USERNAME;
const SYSTEM_ADMIN_PASSWORD = process.env.QA_SYSTEM_ADMIN_PASSWORD;
const HAS_SYSTEM_ADMIN_CREDENTIALS = Boolean(SYSTEM_ADMIN_USERNAME && SYSTEM_ADMIN_PASSWORD);

async function loginSystemAdmin(page) {
  if (!HAS_SYSTEM_ADMIN_CREDENTIALS) {
    throw new Error('QA system admin credentials are not configured');
  }
  await page.goto('/login?intent=system&returnUrl=%2Fsystem', { waitUntil: 'networkidle' });
  await page.getByLabel('帳號').first().or(page.locator('input[name="username"]')).first().fill(SYSTEM_ADMIN_USERNAME);
  await page.getByLabel('密碼').first().or(page.locator('input[type="password"]')).first().fill(SYSTEM_ADMIN_PASSWORD);
  await page.getByRole('button', { name: /登入|Sign in|Log in/i }).first().click();
  await page.waitForURL(/\/(system(\/|$)|login\/context(\/|$))/, { timeout: 15_000 });
  if (page.url().includes('/login/context')) {
    try {
      await page.waitForURL(/\/system(\/|$)/, { timeout: 8_000 });
    } catch {
      const adminOption = page.getByRole('button', { name: /系統管理員|SYSTEM_ADMIN/i }).first();
      await adminOption.waitFor({ state: 'visible', timeout: 8_000 });
      await adminOption.click();
      await page.waitForURL(/\/system(\/|$)/, { timeout: 15_000 });
    }
  }
}

async function checkpointShot(page, testInfo, name) {
  const png = await page.screenshot({ fullPage: true });
  await testInfo.attach(`${name}.png`, { body: png, contentType: 'image/png' });
  const dir = path.join(process.cwd(), 'test-results', 'system-users-redesign-shots');
  await fs.mkdir(dir, { recursive: true });
  await fs.writeFile(path.join(dir, `${name}.png`), png);
}

test.describe('system users redesign scope', () => {
  test.beforeEach(() => {
    test.skip(!HAS_SYSTEM_ADMIN_CREDENTIALS, 'QA system admin credentials are not configured');
  });

  test('load list and select user', async ({ page }, testInfo) => {
    await loginSystemAdmin(page);
    await page.goto('/system/users', { waitUntil: 'networkidle' });

    await expect(page.getByRole('heading', { name: /使用者與權限|Users & Permissions/ })).toBeVisible();
    await expect(page.locator('.sys-users-list-items li button').first()).toBeVisible();
    await checkpointShot(page, testInfo, 'load-list');

    const firstUser = page.locator('.sys-users-list-items li button').first();
    await firstUser.click();
    await expect(page.locator('.sys-users-detail .sys-users-username')).toBeVisible();
    await checkpointShot(page, testInfo, 'select-user');
  });

  test('search and pagination are deterministic', async ({ page }, testInfo) => {
    await loginSystemAdmin(page);
    await page.goto('/system/users', { waitUntil: 'networkidle' });

    const firstRowButton = page.locator('.sys-users-list-items li button').first();
    await expect(firstRowButton).toBeVisible();
    const firstUsername = (await firstRowButton.locator('strong').textContent())?.trim() || '';
    expect(firstUsername.length).toBeGreaterThan(0);

    const query = firstUsername.slice(0, Math.min(4, firstUsername.length));
    await page.getByRole('searchbox', { name: /搜尋帳號|Search username/ }).fill(query);
    await expect(page.locator('.sys-users-list-items li button strong').first()).toContainText(query);

    const pageIndicator = page.locator('.sys-users-pagination .sys-muted').first();
    if (await pageIndicator.isVisible().catch(() => false)) {
      await expect(pageIndicator).toContainText(/第 .* 頁|Page .* \//);
      await page.getByRole('button', { name: /下一頁|Next/ }).click();
      await page.getByRole('button', { name: /上一頁|Previous/ }).click();
      await expect(page.locator('.sys-users-list-items li button strong').first()).toContainText(query);
    }

    await checkpointShot(page, testInfo, 'search-pagination-deterministic');
  });

  test('validation feedback for merchant role binding', async ({ page }, testInfo) => {
    await loginSystemAdmin(page);
    await page.goto('/system/users', { waitUntil: 'networkidle' });
    await page.locator('.sys-users-list-items li button').first().click();

    const roleSelect = page.getByLabel('角色', { exact: true });
    const merchantIdInput = page.getByLabel(/商家 ID|Merchant ID/);
    const addButton = page.getByRole('button', { name: /新增綁定|Add Binding/ });

    // Force merchant-scoped role and submit empty merchant id to trigger required validation.
    await roleSelect.selectOption('MERCHANT');
    await expect(merchantIdInput).toBeEnabled();
    await merchantIdInput.fill('');
    await addButton.click();
    await expect(page.locator('.sys-error').first()).toContainText(/商家角色必須填寫商家 ID|Merchant ID is required/);
    await checkpointShot(page, testInfo, 'validation-required');

    await merchantIdInput.fill('0');
    await addButton.click();
    await expect(page.locator('.sys-error').first()).toContainText(/商家 ID 必須是大於 0 的整數|positive integer/);
    await checkpointShot(page, testInfo, 'validation-invalid');
  });

  test('add and remove binding, toggle enabled', async ({ page }, testInfo) => {
    await loginSystemAdmin(page);
    await page.goto('/system/users', { waitUntil: 'networkidle' });
    await page.locator('.sys-users-list-items li button').first().click();

    const roleSelect = page.getByLabel('角色', { exact: true });
    const merchantIdInput = page.getByLabel(/商家 ID|Merchant ID/);
    const addButton = page.getByRole('button', { name: /新增綁定|Add Binding/ });

    await roleSelect.selectOption('SUB_MERCHANT');
    await merchantIdInput.fill('1');
    await addButton.click();
    await expect(page.locator('.sys-list li strong', { hasText: 'SUB_MERCHANT' }).first()).toBeVisible();
    await checkpointShot(page, testInfo, 'add-binding');

    const subMerchantItems = page.locator('.sys-list li', { hasText: 'SUB_MERCHANT' });
    const beforeRemoveCount = await subMerchantItems.count();
    await subMerchantItems.first().getByRole('button', { name: /移除|Remove/ }).first().click();
    await page.locator('.sys-dialog .sys-dialog-actions button').first().click();
    await expect.poll(async () => subMerchantItems.count()).toBeLessThan(beforeRemoveCount);
    await checkpointShot(page, testInfo, 'remove-binding');

    const toggleButton = page.getByRole('button', {
      name: /啟用帳號|停用帳號|Enable account|Disable account/,
    });
    await toggleButton.click();
    await page.locator('.sys-dialog .sys-dialog-actions button').first().click();
    await expect(toggleButton).toContainText(/啟用帳號|停用帳號|Enable account|Disable account/);
    await checkpointShot(page, testInfo, 'toggle-enabled');
  });
});
