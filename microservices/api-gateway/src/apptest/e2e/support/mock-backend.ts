/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { Page, Route } from '@playwright/test';

import { managementInfo } from './endpoints';

export interface FulfillOptions {
  headers?: Record<string, string>;
  status?: number;
}

/**
 * Fulfils a route with a JSON body. Snow-White's generated axios clients only ever read
 * `response.data` and `response.headers['x-total-count']`, so this is the only shape mocked
 * responses need.
 */
export async function fulfillJson(route: Route, body: unknown, options: FulfillOptions = {}): Promise<void> {
  await route.fulfill({
    status: options.status ?? 200,
    headers: { 'content-type': 'application/json', ...options.headers },
    body: JSON.stringify(body),
  });
}

export async function mockJson(page: Page, url: RegExp | string, body: unknown, options?: FulfillOptions): Promise<void> {
  await page.route(url, route => fulfillJson(route, body, options));
}

export function totalCountHeaders(totalCount: number): Record<string, string> {
  return { 'x-total-count': String(totalCount) };
}

/**
 * Every page dispatches `getProfile()` on mount (see app.tsx), so this is installed by the
 * `test` fixture for every test. Override it in an individual test to exercise the
 * backend-unavailable banner.
 */
export async function mockManagementInfo(page: Page, overrides: Record<string, unknown> = {}): Promise<void> {
  await mockJson(page, managementInfo(), {
    'display-ribbon-on-profiles': '',
    activeProfiles: ['test'],
    ...overrides,
  });
}
