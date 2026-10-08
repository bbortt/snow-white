/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { Route } from '@playwright/test';

import { openApiCriterion, qualityGateConfig } from './support/data';
import { criteria, qualityGateByName, qualityGates } from './support/endpoints';
import { fulfillJson, mockJson, totalCountHeaders } from './support/mock-backend';
import { expect, test } from './support/test';

test('renders the list, hiding edit/delete actions for predefined configs', async ({ page }) => {
  const builtIn = qualityGateConfig({ name: 'built-in', isPredefined: true });
  const custom = qualityGateConfig({ name: 'custom', isPredefined: false });
  await mockJson(page, qualityGates(), [builtIn, custom], { headers: totalCountHeaders(2) });

  await page.goto('/quality-gate-config');

  const rows = page.getByTestId('qualityGateConfigTable');
  await expect(rows).toHaveCount(2);

  const builtInRow = rows.filter({ hasText: 'built-in' });
  await expect(builtInRow.getByTestId('entityEditButton')).toHaveCount(0);
  await expect(builtInRow.getByTestId('entityDeleteButton')).toHaveCount(0);

  const customRow = rows.filter({ hasText: 'custom' });
  await expect(customRow.getByTestId('entityEditButton')).toHaveCount(1);
  await expect(customRow.getByTestId('entityDeleteButton')).toHaveCount(1);
});

test('creates a new quality gate config', async ({ page }) => {
  await mockJson(page, qualityGates(), [], { headers: totalCountHeaders(0) });
  await mockJson(page, criteria(), [openApiCriterion({ id: 'PATH_COVERAGE' })]);

  let createRequestBody: unknown;
  await page.route(qualityGates(), async (route: Route) => {
    if (route.request().method() === 'POST') {
      createRequestBody = JSON.parse(route.request().postData() ?? '{}');
      await fulfillJson(route, createRequestBody, { status: 201 });
    } else {
      await fulfillJson(route, [], { headers: totalCountHeaders(0) });
    }
  });

  await page.goto('/quality-gate-config');
  await page.getByTestId('entityCreateButton').click();
  await expect(page).toHaveURL(/\/quality-gate-config\/new$/);

  await page.getByTestId('name').fill('release-gate');
  await page.getByTestId('description').fill('Gate for release readiness');
  await page.getByTestId('openApiCoverageCriteria').selectOption(['PATH_COVERAGE']);
  await page.getByTestId('entityCreateSaveButton').click();

  await expect(page).toHaveURL(/\/quality-gate-config(\?.*)?$/);
  expect(createRequestBody).toMatchObject({
    name: 'release-gate',
    description: 'Gate for release readiness',
    isPredefined: false,
    openApiCoverageCriteria: ['PATH_COVERAGE'],
  });
});

test('edits an existing quality gate config', async ({ page }) => {
  const existing = qualityGateConfig({ name: 'release-gate', description: 'Old description' });
  await mockJson(page, qualityGates(), [existing], { headers: totalCountHeaders(1) });
  await mockJson(page, criteria(), [openApiCriterion({ id: 'PATH_COVERAGE' })]);

  let updateRequestBody: unknown;
  await page.route(qualityGateByName('release-gate'), async (route: Route) => {
    if (route.request().method() === 'PUT') {
      updateRequestBody = JSON.parse(route.request().postData() ?? '{}');
      await fulfillJson(route, updateRequestBody);
    } else {
      await fulfillJson(route, existing);
    }
  });

  await page.goto('/quality-gate-config/release-gate/edit');

  await expect(page.getByTestId('name')).toHaveValue('release-gate');
  await page.getByTestId('description').fill('Updated description');
  await page.getByTestId('entityCreateSaveButton').click();

  await expect(page).toHaveURL(/\/quality-gate-config(\?.*)?$/);
  expect(updateRequestBody).toMatchObject({ name: 'release-gate', description: 'Updated description' });
});

test('deletes a quality gate config', async ({ page }) => {
  const existing = qualityGateConfig({ name: 'release-gate' });
  await mockJson(page, qualityGates(), [existing], { headers: totalCountHeaders(1) });

  let deleteCalled = false;
  await page.route(qualityGateByName('release-gate'), async (route: Route) => {
    if (route.request().method() === 'DELETE') {
      deleteCalled = true;
      await route.fulfill({ status: 204 });
    } else {
      await fulfillJson(route, existing);
    }
  });

  await page.goto('/quality-gate-config');
  await page.getByTestId('entityDeleteButton').click();

  await expect(page.getByTestId('qualityGateConfigDeleteDialogHeading')).toBeVisible();
  await mockJson(page, qualityGates(), [], { headers: totalCountHeaders(0) });
  await page.getByTestId('entityConfirmDeleteButton').click();

  await expect(page).toHaveURL(/\/quality-gate-config(\?.*)?$/);
  expect(deleteCalled).toBe(true);
});
