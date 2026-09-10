/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { openApiCriterion, qualityGateConfig, qualityGateReport } from './support/data';
import { criteria, qualityGateByName, reportById } from './support/endpoints';
import { dataCy } from './support/locators';
import { mockJson } from './support/mock-backend';
import { expect, test } from './support/test';

const CALCULATION_ID = 'calc-detail';

test.beforeEach(async ({ page }) => {
  const report = qualityGateReport({
    calculationId: CALCULATION_ID,
    interfaces: [
      {
        serviceName: 'order-service',
        apiName: 'orders-api',
        apiVersion: '1.0.0',
        apiType: 'OPENAPI',
        status: 'PASSED',
        testResults: [
          { id: 'PATH_COVERAGE', coverage: 1, isIncludedInQualityGate: true },
          { id: 'HTTP_METHOD_COVERAGE', coverage: 0.5, isIncludedInQualityGate: false },
        ],
      },
    ],
  });
  await mockJson(page, reportById(CALCULATION_ID), report);
  await mockJson(page, qualityGateByName('default'), qualityGateConfig({ name: 'default' }));
  await mockJson(page, criteria(), [
    openApiCriterion({ id: 'PATH_COVERAGE' }),
    openApiCriterion({ id: 'HTTP_METHOD_COVERAGE', name: 'HTTP Method Coverage' }),
  ]);

  await page.goto(`/quality-gate/${CALCULATION_ID}`);
});

test('renders the summary and, once expanded, the API test results', async ({ page }) => {
  await expect(page.getByText(CALCULATION_ID)).toBeVisible();
  await expect(dataCy(page, 'qualityGateResultsHeading')).toBeVisible();
  await expect(dataCy(page, 'allResultsHeading')).toBeVisible();

  // Only the included result is shown until "show only included" is switched off.
  await page.getByRole('heading', { level: 4, name: /order-service/ }).click();
  await expect(page.getByText('Path')).toBeVisible();
  await expect(page.getByText('HTTP Method')).not.toBeVisible();

  await page.getByRole('switch').uncheck();

  await expect(page.getByText('Path')).toBeVisible();
  await expect(page.getByText('HTTP Method')).toBeVisible();
});

test('links to the JUnit report download', async ({ page }) => {
  await expect(page.getByRole('link', { name: 'Download JUnit Report' })).toHaveAttribute(
    'href',
    `/api/rest/v1/reports/${CALCULATION_ID}/junit`,
  );
});
