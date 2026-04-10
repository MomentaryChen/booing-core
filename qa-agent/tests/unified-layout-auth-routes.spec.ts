import { expect, test } from "@playwright/test";
import fs from "node:fs/promises";
import path from "node:path";

async function checkpoint(page, testInfo, label) {
  const png = await page.screenshot({ fullPage: true });
  await testInfo.attach(`${label}.png`, { body: png, contentType: "image/png" });
  const dir = path.join(process.cwd(), "artifacts", "screenshots");
  await fs.mkdir(dir, { recursive: true });
  await fs.writeFile(path.join(dir, `${label}-${testInfo.project.name}.png`), png);
}

async function login(page, username, password) {
  await page.locator('input[autocomplete="username"]').fill(username);
  await page.locator('input[autocomplete="current-password"]').fill(password);
  await page.locator('button[type="submit"]').filter({ hasText: /log in|login|登入/i }).first().click();
}

const SYSTEM_ADMIN_USERNAME = process.env.QA_SYSTEM_ADMIN_USERNAME || "admin";
const SYSTEM_ADMIN_PASSWORD = process.env.QA_SYSTEM_ADMIN_PASSWORD || "Admin#2026!Safe";

test.describe("requirement validation: unified top-right login and layout", () => {
  test("case 1: / has top-right login/register entries", async ({ page }, testInfo) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await expect(page.locator("button, a").filter({ hasText: /log in|login|登入/i }).first()).toBeVisible();
    await expect(page.locator("button, a").filter({ hasText: /register|signup|註冊/i }).first()).toBeVisible();
    await checkpoint(page, testInfo, "req-home-top-right-auth");
  });

  test("case 2: /demo/saas-dashboard keeps unified shell and auth entry", async ({ page }, testInfo) => {
    const response = await page.goto("/demo/saas-dashboard", { waitUntil: "domcontentloaded" });
    expect(response?.ok() ?? false).toBeTruthy();
    await expect(page.locator("body")).toBeVisible();
    await expect(page.locator("button, a").filter({ hasText: /log in|login|登入/i }).first()).toBeVisible();
    await checkpoint(page, testInfo, "req-demo-saas-dashboard-shell");
  });

  test("case 3: /client/booking/demo-merchant renders and keeps auth entry", async ({ page }, testInfo) => {
    const response = await page.goto("/client/booking/demo-merchant", { waitUntil: "domcontentloaded" });
    expect(response?.ok() ?? false).toBeTruthy();
    await expect(page.locator("body")).toBeVisible();
    await expect(page.locator("button, a").filter({ hasText: /log in|login|登入/i }).first()).toBeVisible();
    await checkpoint(page, testInfo, "req-client-booking-demo-merchant");
  });

  test("case 4: /merchant unauth opens login overlay; close clears query", async ({ page }, testInfo) => {
    await page.goto("/merchant", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/auth=login/);
    await expect(page).toHaveURL(/returnUrl=%2Fmerchant/);
    await expect(page.locator('[role="dialog"][aria-modal="true"]')).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(page).not.toHaveURL(/auth=login/);
    await checkpoint(page, testInfo, "req-merchant-unauth-overlay");
  });

  test("case 5: /system unauth full-page login flow succeeds", async ({ page }, testInfo) => {
    await page.goto("/system", { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/(\/login\?|auth=login)/);
    await expect(page).toHaveURL(/intent=system/);
    await login(page, SYSTEM_ADMIN_USERNAME, SYSTEM_ADMIN_PASSWORD);
    const invalidCredential = page.locator("text=/Invalid credentials|登入失敗/i").first();
    await Promise.race([
      page.waitForURL(/\/system|\/login\/context/, { timeout: 20000 }),
      invalidCredential.waitFor({ state: "visible", timeout: 20000 }),
    ]);
    if (await invalidCredential.isVisible().catch(() => false)) {
      throw new Error("System login should not show invalid credential message");
    }
    if (page.url().includes("/login/context")) {
      await expect(page.locator("h1")).toContainText(/選擇登入情境|Select/i);
      const systemContextButton = page
        .locator("button")
        .filter({ hasText: /系統管理員|SYSTEM_ADMIN|System admin/i })
        .first();
      const reachedSystemDirectly = await page
        .waitForURL((url) => url.pathname.startsWith("/system"), { timeout: 3000 })
        .then(() => true)
        .catch(() => false);
      if (!reachedSystemDirectly) {
        await expect(systemContextButton).toBeVisible({ timeout: 15000 });
        await systemContextButton.click();
        await page.waitForURL((url) => url.pathname.startsWith("/system"), { timeout: 20000 });
      }
    }
    await expect(page).toHaveURL(/\/system(\/|$)/);
    await checkpoint(page, testInfo, "req-system-unauth-login-flow");
  });

  test("case 6: /login page does not render overlay dialog", async ({ page }, testInfo) => {
    await page.goto("/login?intent=merchant&returnUrl=%2Fmerchant", { waitUntil: "domcontentloaded" });
    await expect(page.locator('input[autocomplete="username"]')).toBeVisible();
    await expect(page.locator('[role="dialog"][aria-modal="true"]')).toHaveCount(0);
    await checkpoint(page, testInfo, "req-login-page-no-overlay");
  });

  test("case 7: overlay query params open and close contract", async ({ page }, testInfo) => {
    await page.goto("/?auth=login&intent=merchant", { waitUntil: "domcontentloaded" });
    await expect(page.locator('[role="dialog"][aria-modal="true"]')).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(page).not.toHaveURL(/auth=login/);
    await checkpoint(page, testInfo, "req-overlay-query-open-close");
  });

  test("case 8: i18n labels expose auth actions in zh/en", async ({ page }, testInfo) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const authTextCount = await page.locator("text=/登入|註冊|Log in|Register/i").count();
    expect(authTextCount).toBeGreaterThan(0);
    await checkpoint(page, testInfo, "req-i18n-auth-labels");
  });
});
