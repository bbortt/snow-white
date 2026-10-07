/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { apiIndexEntry } from './support/data';
import { apiNames, apis, serviceNames } from './support/endpoints';
import { mockJson, totalCountHeaders } from './support/mock-backend';
import { expect, test } from './support/test';

test.beforeEach(async ({ page }) => {
  await mockJson(page, serviceNames(), ['order-service']);
  await mockJson(page, apiNames(), []);
});

test('shows the empty state when there are no APIs', async ({ page }) => {
  await mockJson(page, apis(), [], { headers: totalCountHeaders(0) });

  await page.goto('/api-index');

  await expect(page.getByText('No APIs found')).toBeVisible();
});

test('renders APIs and flags prereleases', async ({ page }) => {
  const stable = apiIndexEntry({ serviceName: 'order-service', apiName: 'orders-api', apiVersion: '1.0.0' });
  const prerelease = apiIndexEntry({ serviceName: 'order-service', apiName: 'orders-api', apiVersion: '2.0.0-beta.1', prerelease: true });
  await mockJson(page, apis(), [stable, prerelease], { headers: totalCountHeaders(2) });

  await page.goto('/api-index');

  const rows = page.getByTestId('apiIndexTable');
  await expect(rows).toHaveCount(2);
  await expect(rows.filter({ hasText: '2.0.0-beta.1' }).getByText('Pre')).toBeVisible();
  await expect(rows.filter({ hasText: '1.0.0' }).getByText('Pre')).not.toBeVisible();
});

// As with the quality-gate list, the cascading service/api/version filter debounce is
// intentionally only smoke-tested here, not exercised in full.
test('filtering by service name updates the URL and re-fetches the list', async ({ page }) => {
  await mockJson(page, apis(), [], { headers: totalCountHeaders(0) });

  await page.goto('/api-index');

  const nextList = page.waitForRequest(request => request.url().includes('serviceName=order-service'));
  await page.getByPlaceholder('Service Name').fill('order-service');
  await nextList;

  await expect(page).toHaveURL(/serviceName=order-service/);
});
