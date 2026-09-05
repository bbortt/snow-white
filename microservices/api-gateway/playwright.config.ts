/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { defineConfig, devices } from '@playwright/test';

// Black-box UI tests.
// Every backend call is intercepted in-browser via `page.route` before it ever leaves the page (see src/apptest/e2e/support),
// so these run unchanged against either target below - only the origin serving the frontend bundle differs.
//
// Default target: the dev bundle, self-served by webpack/e2e-server.cjs on :9060 (see that file for why it isn't `webpack-dev-server`).
// Used by `pnpm run e2e` / the `e2e` Maven profile on every PR.
//
// Alternate target: set PLAYWRIGHT_BASE_URL to point at an already-running,
// fully built api-gateway instead (e.g. the `apptest` profile's docker-maven-plugin container on http://localhost:8080) -
// this exercises the real production bundle and skips starting a second server.
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:9060';
const targetsExternalServer = !!process.env.PLAYWRIGHT_BASE_URL;

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
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: targetsExternalServer
    ? undefined
    : {
        // `npm run e2e:serve` (not `webapp:dev`) - see webpack/e2e-server.cjs for why.
        command: 'npm run e2e:serve',
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
