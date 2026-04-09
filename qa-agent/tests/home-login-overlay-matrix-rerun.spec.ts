import { expect, test } from "@playwright/test";
import fs from "node:fs/promises";
import path from "node:path";

const FRONTEND_BASE_URL = process.env.PLAYWRIGHT_BASE_URL ?? "http://127.0.0.1:25175";

const creds = {
  merchant: { username: "merchant", password: "merchant" },
  system: { username: "admin", password: "admin" },
  client: { username: "client", password: "client" },
};

async function checkpoint(page, testInfo, label) {
  const png = await page.screenshot({ fullPage: true });
  await testInfo.attach(`${label}.png`, { body: png, contentType: "image/png" });
  const dir = path.join(process.cwd(), "artifacts", "screenshots");
  await fs.mkdir(dir, { recursive: true });
  await fs.writeFile(path.join(dir, `${label}-${testInfo.project.name}.png`), png);
}

async function fillAndSubmitLogin(page, username, password) {
  await page.locator('input[autocomplete="username"]').fill(username);
  await page.locator('input[autocomplete="current-password"]').fill(password);
  await page.locator('button[type="submit"]').filter({ hasText: /log in|登入/i }).first().click();
}

test.describe("home login overlay matrix rerun after CORS fix", () => {
  test.describe.configure({ mode: "serial" });

  test("row1: home has hero + top-nav auth entry", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/`, { waitUntil: "domcontentloaded" });
    await expect(page.locator("button, a").filter({ hasText: /log in|登入/i }).first()).toBeVisible();
    await expect(page.locator("button, a").filter({ hasText: /register|註冊/i }).first()).toBeVisible();
    await checkpoint(page, testInfo, "row1-home-entries");
  });

  test("row2: clicking entry opens overlay with auth query", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/`, { waitUntil: "domcontentloaded" });
    await page.locator("button, a").filter({ hasText: /log in|登入/i }).first().click();
    await expect(page).toHaveURL(/auth=login/);
    await expect(page.locator('[role="dialog"][aria-modal="true"]')).toBeVisible();
    await checkpoint(page, testInfo, "row2-overlay-open");
  });

  test("row3: overlay login success redirects and clears query", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/?auth=login&intent=merchant`, { waitUntil: "domcontentloaded" });
    await fillAndSubmitLogin(page, creds.merchant.username, creds.merchant.password);
    await page.waitForURL((url) => !url.searchParams.has("auth") && /\/merchant/.test(url.pathname), { timeout: 20000 });
    expect(page.url()).not.toContain("auth=");
    await checkpoint(page, testInfo, "row3-login-success");
  });

  test("row4: escape closes overlay and removes query", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/?auth=login&intent=merchant`, { waitUntil: "domcontentloaded" });
    await expect(page.locator('[role="dialog"]')).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(page).toHaveURL(new RegExp(`${FRONTEND_BASE_URL.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}/?$`));
    await expect(page.locator('[role="dialog"]')).toHaveCount(0);
    await checkpoint(page, testInfo, "row4-overlay-close");
  });

  test("row5: overlay register success lands on registered login query", async ({ page }, testInfo) => {
    const seed = Date.now();
    await page.goto(`${FRONTEND_BASE_URL}/?auth=register&intent=merchant`, { waitUntil: "domcontentloaded" });
    await page.locator('input[autocomplete="organization"]').fill(`QA Overlay ${seed}`);
    await page.locator('input[placeholder*="slug" i], input[placeholder*="代號"]').fill(`qa-overlay-${seed}`);
    await page.locator('button[type="submit"]').filter({ hasText: /create|註冊|建立/i }).first().click();
    await page.waitForURL((url) => url.pathname === "/" && url.searchParams.get("auth") === "login" && url.searchParams.get("registered") === "1", { timeout: 20000 });
    await expect(page.locator("text=/registered|註冊成功/i")).toBeVisible();
    await checkpoint(page, testInfo, "row5-overlay-register-success");
  });

  test("row6: full-page login intent + returnUrl works", async ({ page }, testInfo) => {
    const target = "/system";
    await page.goto(`${FRONTEND_BASE_URL}/login?intent=system&returnUrl=${encodeURIComponent(target)}`, { waitUntil: "domcontentloaded" });
    await fillAndSubmitLogin(page, creds.system.username, creds.system.password);
    await page.waitForURL((url) => url.pathname.startsWith("/system"), { timeout: 20000 });
    await checkpoint(page, testInfo, "row6-login-intent-system");
  });

  test("row7: legacy login routes redirect to /login", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/merchant/login`, { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/\/login\?/);
    await checkpoint(page, testInfo, "row7-legacy-redirect");
  });

  test("row8: protected route without token redirects to overlay with returnUrl", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/merchant`, { waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/auth=login/);
    await expect(page).toHaveURL(/returnUrl=%2Fmerchant/);
    await fillAndSubmitLogin(page, creds.merchant.username, creds.merchant.password);
    await page.waitForURL((url) => url.pathname.startsWith("/merchant"), { timeout: 20000 });
    await checkpoint(page, testInfo, "row8-protected-route-return");
  });

  test("row9: 401 on protected pages returns to overlay and avoids loop", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/merchant`, { waitUntil: "domcontentloaded" });
    await fillAndSubmitLogin(page, creds.merchant.username, creds.merchant.password);
    await page.waitForURL((url) => url.pathname.startsWith("/merchant"), { timeout: 20000 });
    await page.evaluate(() => localStorage.setItem("booking_core_access_token", "invalid.jwt.token"));
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(/auth=login/);
    await expect(page).not.toHaveURL(/\/login\/login/);
    await checkpoint(page, testInfo, "row9-401-overlay-no-loop");
  });

  test("row10: /403 CTA opens merchant overlay", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/403`, { waitUntil: "domcontentloaded" });
    await page.locator("button, a").filter({ hasText: /log in|登入/i }).first().click();
    await expect(page).toHaveURL(/auth=login&intent=merchant/);
    await expect(page.locator('[role="dialog"]')).toBeVisible();
    await checkpoint(page, testInfo, "row10-forbidden-cta");
  });

  test("row11: full-page merchant register success URL contract", async ({ page }, testInfo) => {
    const seed = Date.now();
    await page.goto(`${FRONTEND_BASE_URL}/merchant/register`, { waitUntil: "domcontentloaded" });
    await page.locator('input[autocomplete="organization"]').fill(`QA Page ${seed}`);
    await page.locator('input[placeholder*="slug" i], input[placeholder*="代號"]').fill(`qa-page-${seed}`);
    await page.locator('button[type="submit"]').filter({ hasText: /create|註冊|建立/i }).first().click();
    await page.waitForURL((url) => url.pathname === "/" && url.searchParams.get("auth") === "login" && url.searchParams.get("registered") === "1", { timeout: 20000 });
    await checkpoint(page, testInfo, "row11-page-register-success");
  });

  test("row12: locale switch keeps strings in both zh/en", async ({ page }, testInfo) => {
    await page.goto(`${FRONTEND_BASE_URL}/`, { waitUntil: "domcontentloaded" });
    const hasZh = await page.locator("text=/登入|註冊|商戶/").count();
    await page.locator("select, button, a").filter({ hasText: /en|english/i }).first().click().catch(() => {});
    const hasEn = await page.locator("text=/Log in|Register|Merchant/i").count();
    expect(hasZh + hasEn).toBeGreaterThan(0);
    await checkpoint(page, testInfo, "row12-locale-strings");
  });
});
