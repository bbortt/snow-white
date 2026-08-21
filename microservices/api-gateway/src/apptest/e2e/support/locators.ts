/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { Locator, Page } from '@playwright/test';

/**
 * Some components carry a leftover `data-cy` attribute (this app predates its migration from
 * Cypress) instead of `data-testid`, which is what Playwright's `getByTestId` reads by default.
 * Use this for those; use `page.getByTestId` for the rest.
 */
export function dataCy(page: Page, value: string): Locator {
  return page.locator(`[data-cy="${value}"]`);
}
