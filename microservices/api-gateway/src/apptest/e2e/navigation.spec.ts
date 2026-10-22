/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { openApiCriterion, qualityGateConfig } from './support/data';
import { apis, criteria, managementInfo, qualityGates, reports } from './support/endpoints';
import { dataCy } from './support/locators';
import { mockJson, totalCountHeaders } from './support/mock-backend';
import { expect, test } from './support/test';

test.describe('app shell', () => {
  test.beforeEach(async ({ page }) => {
    await mockJson(page, reports(), [], { headers: totalCountHeaders(0) });
    await mockJson(page, qualityGates(), [qualityGateConfig()], { headers: totalCountHeaders(1) });
    await mockJson(page, criteria(), [openApiCriterion()]);
    await mockJson(page, apis(), [], { headers: totalCountHeaders(0) });
  });

  test('renders the home page with the header and the results widget', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByTestId('navbar')).toBeVisible();
    await expect(page.getByText('Welcome to Snow-White')).toBeVisible();
    await expect(dataCy(page, 'QualityGateHeading')).toBeVisible();
  });

  test('navigates to every top-level page via the header menu', async ({ page }) => {
    await page.goto('/');

    await page.getByTestId('results-menu').getByRole('link').click();
    await expect(dataCy(page, 'QualityGateHeading')).toBeVisible();
    await expect(page).toHaveURL(/\/quality-gate(\?.*)?$/);

    await page.getByTestId('quality-gates-menu').getByRole('link').click();
    await expect(page.getByTestId('QualityGateConfigHeading')).toBeVisible();
    await expect(page).toHaveURL(/\/quality-gate-config(\?.*)?$/);

    await page.getByTestId('criteria-menu').getByRole('link').click();
    await page.getByRole('menuitem', { name: 'OpenAPI Coverage Criteria' }).click();
    await expect(page.getByTestId('OpenApiCriterionHeading')).toBeVisible();
    await expect(page).toHaveURL(/\/open-api-criterion$/);

    await page.getByTestId('api-index-menu').getByRole('link').click();
    await expect(page.getByTestId('ApiIndexHeading')).toBeVisible();
    await expect(page).toHaveURL(/\/api-index(\?.*)?$/);
  });

  test('switches the UI language', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByTestId('navbar').getByRole('link', { name: 'Home' })).toBeVisible();

    await page.getByTestId('locale-menu').getByRole('link').click();
    await page.getByRole('menuitem', { name: 'Deutsch' }).click();

    await expect(page.getByTestId('navbar').getByRole('link', { name: 'Startseite' })).toBeVisible();
  });

  test('shows a not-found page for unknown routes', async ({ page }) => {
    await page.goto('/this-route-does-not-exist');

    await expect(page.getByText('The page does not exist.')).toBeVisible();
  });
});

test.describe('backend availability', () => {
  test('shows the backend-unavailable banner when management/info fails', async ({ page }) => {
    await page.route(managementInfo(), route => route.fulfill({ status: 503, contentType: 'application/json', body: '{}' }));

    await page.goto('/');

    await expect(page.locator('#backend-unavailable-banner')).toBeVisible();
    await expect(page.getByText('Snow-White is currently unavailable.')).toBeVisible();
  });
});
