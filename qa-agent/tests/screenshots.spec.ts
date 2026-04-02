import { expect, test } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

type PageShot = {
  pathname: string;
  file: string;
};

const BASE_URL = 'http://localhost:25173';

const PAGES: PageShot[] = [
  { pathname: '/', file: 'home.png' },
  { pathname: '/system', file: 'system.png' },
  { pathname: '/merchant/login', file: 'merchant-login.png' },
  { pathname: '/merchant/register', file: 'merchant-register.png' },
  { pathname: '/merchant', file: 'merchant-dashboard.png' },
  { pathname: '/merchant/appointments', file: 'merchant-appointments.png' },
  { pathname: '/merchant/settings/schedule', file: 'merchant-schedule-settings.png' },
  { pathname: '/client', file: 'client.png' },
  { pathname: '/client/booking/demo-merchant', file: 'client-booking-demo-merchant.png' },
  { pathname: '/store/demo-merchant', file: 'store-redirect-demo-merchant.png' }
];

const screenshotsDir = path.join(process.cwd(), 'artifacts', 'screenshots');

function toUrl(pathname: string) {
  return `${BASE_URL}${pathname}`;
}

async function loginViaMerchantLoginUi(page, username: string, password: string) {
  await page.goto(toUrl('/merchant/login'), { waitUntil: 'networkidle' });

  const usernameInput =
    page.getByLabel('帳號').first().or(page.getByPlaceholder('例如：merchant')).first().or(page.locator('input[name="username"]')).first();
  const passwordInput =
    page.getByLabel('密碼').first().or(page.locator('input[type="password"]')).first().or(page.locator('input[name="password"]')).first();

  await usernameInput.fill(username);
  await passwordInput.fill(password);

  await page.getByRole('button', { name: '登入' }).click();

  // App uses SPA navigation + async nav refresh.
  await page.waitForURL(/\/(merchant|system)(\/|$)/, { timeout: 15_000 });
  await expect(page.getByRole('status')).toBeHidden({ timeout: 15_000 });
}

async function loginMerchant(page) {
  await loginViaMerchantLoginUi(page, 'merchant', 'merchant');
  await page.waitForURL(/\/merchant(\/|$)/, { timeout: 15_000 });
}

async function loginSystemAdmin(page) {
  await loginViaMerchantLoginUi(page, 'admin', 'admin');
  await page.waitForURL(/\/system(\/|$)/, { timeout: 15_000 });
}

test.describe('screenshots: keep artifacts/screenshots up to date', () => {
  test.beforeAll(async () => {
    await fs.rm(screenshotsDir, { recursive: true, force: true });
    await fs.mkdir(screenshotsDir, { recursive: true });
  });

  for (const pageShot of PAGES) {
    test(`capture ${pageShot.pathname} -> ${pageShot.file}`, async ({ page }) => {
      await page.setViewportSize({ width: 1440, height: 900 });

      if (pageShot.pathname === '/system') {
        await loginSystemAdmin(page);
      } else if (
        pageShot.pathname.startsWith('/merchant') &&
        pageShot.pathname !== '/merchant/login' &&
        pageShot.pathname !== '/merchant/register'
      ) {
        await loginMerchant(page);
      }

      await page.goto(toUrl(pageShot.pathname), { waitUntil: 'networkidle' });
      await page.waitForTimeout(200);
      await page.screenshot({
        path: path.join(screenshotsDir, pageShot.file),
        fullPage: true,
      });
    });
  }
});

