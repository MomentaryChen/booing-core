import { expect, test } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

test('smoke: landing page renders and capture key screenshot', async ({ page }, testInfo) => {
  const response = await page.goto('/', { waitUntil: 'domcontentloaded' });
  expect(response?.ok()).toBeTruthy();

  await expect(page.locator('body')).toBeVisible();

  const png = await page.screenshot({ fullPage: true });
  await testInfo.attach('landing.png', { body: png, contentType: 'image/png' });

  const screenshotsDir = path.join(process.cwd(), 'artifacts', 'screenshots');
  await fs.mkdir(screenshotsDir, { recursive: true });
  const filePath = path.join(screenshotsDir, `landing-${testInfo.project.name}.png`);
  await fs.writeFile(filePath, png);
  await fs.stat(filePath);
});

