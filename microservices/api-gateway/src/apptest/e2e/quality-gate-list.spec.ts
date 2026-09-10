/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { qualityGateConfig, qualityGateReport } from './support/data';
import { apiNames, criteria, qualityGateByName, reportById, reports, serviceNames } from './support/endpoints';
import { dataCy } from './support/locators';
import { mockJson, totalCountHeaders } from './support/mock-backend';
import { expect, test } from './support/test';

test.describe('quality-gate list', () => {
  test('shows the not-found alert when no report exists', async ({ page }) => {
    await mockJson(page, reports(), [], { headers: totalCountHeaders(0) });

    await page.goto('/quality-gate');

    await expect(page.getByText('No Quality-Gates found')).toBeVisible();
  });

  test('renders reports and links to their detail page', async ({ page }) => {
    const passed = qualityGateReport({ calculationId: 'calc-passed', status: 'PASSED' });
    const failed = qualityGateReport({ calculationId: 'calc-failed', status: 'FAILED' });
    await mockJson(page, reports(), [passed, failed], { headers: totalCountHeaders(2) });
    await mockJson(page, reportById('calc-passed'), passed);
    await mockJson(page, qualityGateByName('default'), qualityGateConfig({ name: 'default' }));
    await mockJson(page, criteria(), []);

    await page.goto('/quality-gate');

    const rows = dataCy(page, 'qualityGateTable');
    await expect(rows).toHaveCount(2);
    await expect(page.getByText('calc-passed')).toBeVisible();
    await expect(page.getByText('calc-failed')).toBeVisible();

    await rows.filter({ hasText: 'calc-passed' }).locator('[data-cy="entityDetailsButton"]').click();

    await expect(page).toHaveURL(/\/quality-gate\/calc-passed$/);
    await expect(dataCy(page, 'qualityGateDetailsHeading')).toBeVisible();
  });

  test('shows pagination once results span more than one page', async ({ page }) => {
    await mockJson(page, reports(), [qualityGateReport()], { headers: totalCountHeaders(12) });

    await page.goto('/quality-gate');

    await expect(page.locator('ul.pagination')).toBeVisible();
    await expect(page.getByRole('button', { name: '2', exact: true })).toBeVisible();
  });

  test('sorting a column updates the URL sort parameter', async ({ page }) => {
    // The URL only starts reflecting pagination/sort state once the user interacts with it (the
    // sort-sync effect doesn't re-run just because the async list fetch resolved) - so this
    // starts from a URL that already carries one, rather than asserting on the default.
    await mockJson(page, reports(), [qualityGateReport()], { headers: totalCountHeaders(1) });

    await page.goto('/quality-gate?page=1&sort=createdAt,desc');
    await page.getByRole('button', { name: 'Calculation Id' }).click();

    await expect(page).toHaveURL(/sort=calculationId%2Casc/);
  });

  // Filtering itself (debounced, cascading service/api/version autocomplete) is deliberately not
  // tested in depth here - this only confirms the filter card wires an input through to the URL
  // and the list request, not every debounce/cascade edge case.
  test('typing into a filter updates the URL and re-fetches the list', async ({ page }) => {
    await mockJson(page, reports(), [], { headers: totalCountHeaders(0) });
    await mockJson(page, serviceNames(), ['order-service']);
    await mockJson(page, apiNames(), []);

    await page.goto('/quality-gate');
    await page.getByRole('button', { name: 'Filters' }).click();

    const nextList = page.waitForRequest(request => request.url().includes('serviceName=order-service'));
    await page.getByPlaceholder('Service Name').fill('order-service');
    await nextList;

    await expect(page).toHaveURL(/serviceName=order-service/);
  });
});

test.describe('quality-gate detail navigation', () => {
  test('the back button returns to the list', async ({ page }) => {
    const report = qualityGateReport({ calculationId: 'calc-back' });
    await mockJson(page, reports(), [report], { headers: totalCountHeaders(1) });
    await mockJson(page, reportById('calc-back'), report);
    await mockJson(page, qualityGateByName('default'), qualityGateConfig({ name: 'default' }));
    await mockJson(page, criteria(), []);

    await page.goto('/quality-gate');
    await dataCy(page, 'entityDetailsButton').click();
    await expect(page).toHaveURL(/\/quality-gate\/calc-back$/);

    await dataCy(page, 'entityDetailsBackButton').click();

    await expect(page).toHaveURL(/\/quality-gate(\?.*)?$/);
    await expect(dataCy(page, 'QualityGateHeading')).toBeVisible();
  });
});
