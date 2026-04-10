import { expect, test } from "@playwright/test";

test("client guard shows retryable error when navigation API fails", async ({ page }) => {
  // Role checks decode JWT payload only; signature is irrelevant for this UI-level guard test.
  const fakeClientToken =
    "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJyb2xlIjoiQ0xJRU5UIn0.signature";
  await page.addInitScript((token) => {
    localStorage.setItem("booking_core_access_token", token);
  }, fakeClientToken);

  await page.route("**/api/me/navigation", async (route) => {
    await route.fulfill({
      status: 500,
      contentType: "application/json",
      body: JSON.stringify({ message: "navigation failed" }),
    });
  });

  await page.goto("/client", { waitUntil: "domcontentloaded" });
  await expect(page.locator("text=/導覽載入失敗|Failed to load navigation/i")).toBeVisible();
  await expect(page.locator("button").filter({ hasText: /重新載入|Retry/i })).toBeVisible();
});
