/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { expect, test as base } from '@playwright/test';

import { apiNames, serviceNames } from './endpoints';
import { mockJson, mockManagementInfo } from './mock-backend';

/**
 * Extends the base Playwright `test`/`page` fixtures so every test starts with:
 *
 * - A healthy `management/info` response — every page dispatches that call on mount, and without
 *   it the backend-unavailable banner would cover every unrelated test. Override it again within
 *   a test body (a later `page.route` call takes precedence) to test that banner itself.
 * - Empty service-name/API-name suggestion lists — `ResultFilterCard` (quality-gate page) and the
 *   api-index page's column filters both fetch these on mount regardless of whether a test cares
 *   about filtering. Leaving them unmocked doesn't fail softly: the request falls through to the
 *   static server's SPA-fallback HTML, `AutocompleteInput` receives that string as `suggestions`
 *   instead of an array, and `suggestions.filter` throws, crashing the whole page into its error
 *   boundary. Override with `mockJson` again for tests that exercise the suggestions themselves.
 */
export const test = base.extend({
  page: async ({ page }, use) => {
    await mockManagementInfo(page);
    await mockJson(page, serviceNames(), []);
    await mockJson(page, apiNames(), []);
    await use(page);
  },
});

export { expect };
