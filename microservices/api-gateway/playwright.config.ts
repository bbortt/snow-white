/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { defineConfig, devices } from '@playwright/test';

// Black-box UI tests: the built React app is served for real (see webpack/e2e-server.cjs), but
// every backend call is intercepted in-browser via `page.route` before it ever leaves the page -
// see src/apptest/e2e/support.
export default defineConfig({
  testDir: './src/apptest/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [
    ...(process.env.CI ? [['list'] as const] : []),
    ['html', { open: process.env.CI ? 'never' : 'on-failure', outputFolder: 'target/playwright-report' }],
  ],
  outputDir: 'target/playwright-results',
  use: {
    baseURL: 'http://localhost:9060',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    // `npm run e2e:serve` (not `webapp:dev`) - see webpack/e2e-server.cjs for why.
    command: 'npm run e2e:serve',
    url: 'http://localhost:9060',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
