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

  // Only the included result is shown until "show only included" is switched off.
  await page.getByRole('button', { name: /orders-api/ }).click();
  await expect(page.getByText('Path')).toBeVisible();
  await expect(page.getByText('HTTP Method')).not.toBeVisible();

  await page.getByRole('switch').uncheck();

  await expect(page.getByText('Path')).toBeVisible();
  await expect(page.getByText('HTTP Method')).toBeVisible();
});

test('names each service once, above the APIs it contributed', async ({ page }) => {
  const report = qualityGateReport({
    calculationId: CALCULATION_ID,
    interfaces: [
      { serviceName: 'order-service', apiName: 'orders-api', apiVersion: '1.0.0', status: 'PASSED' },
      { serviceName: 'order-service', apiName: 'payments-api', apiVersion: '2.0.0', status: 'FAILED' },
      { serviceName: 'inventory-service', apiName: 'stock-api', apiVersion: '1.0.0', status: 'PASSED' },
    ],
  });
  await mockJson(page, reportById(CALCULATION_ID), report);
  await page.reload();

  const orderService = page.getByRole('region', { name: 'order-service' });
  await expect(orderService.getByRole('button', { name: /orders-api/ })).toBeVisible();
  await expect(orderService.getByRole('button', { name: /payments-api/ })).toBeVisible();
  await expect(page.getByRole('region', { name: 'inventory-service' }).getByRole('button', { name: /stock-api/ })).toBeVisible();
  await expect(page.getByText('order-service', { exact: true })).toHaveCount(1);
});

test('spells out the parameters the calculation was triggered with', async ({ page }) => {
  const report = qualityGateReport({
    calculationId: CALCULATION_ID,
    calculationRequest: {
      includeApis: [{ serviceName: 'order-service', apiName: 'orders-api', apiVersion: '1.0.0' }],
      lookbackWindow: '24h',
      attributeFilters: { environment: 'production' },
    },
  });
  await mockJson(page, reportById(CALCULATION_ID), report);
  await page.reload();

  await expect(dataCy(page, 'lookbackWindow')).toHaveText('24h');
  await expect(dataCy(page, 'attributeFilters')).toHaveText('environment = production');

  await page.getByRole('button', { name: 'Show raw request' }).click();
  await expect(page.locator('pre.code-highlight-block')).toContainText('"environment": "production"');
});

test('toggles the raw request open and closed again without resizing the chart', async ({ page }) => {
  const report = qualityGateReport({
    calculationId: CALCULATION_ID,
    calculationRequest: { lookbackWindow: '24h', attributeFilters: { environment: 'production' } },
    interfaces: [
      {
        serviceName: 'order-service',
        apiName: 'orders-api',
        apiVersion: '1.0.0',
        status: 'PASSED',
        testResults: [{ id: 'PATH_COVERAGE', coverage: 1, isIncludedInQualityGate: true }],
      },
    ],
  });
  await mockJson(page, reportById(CALCULATION_ID), report);
  await page.reload();

  const chart = page.locator('.recharts-responsive-container').first();
  await expect(chart).toBeVisible();
  const chartHeight = (await chart.boundingBox())?.height;

  const toggle = page.getByRole('button', { name: /raw request/ });
  const rawRequest = page.locator('pre.code-highlight-block');

  await toggle.click();
  await expect(toggle).toHaveText('Hide raw request');
  await expect(toggle).toHaveAttribute('aria-expanded', 'true');
  await expect(rawRequest).toBeVisible();
  await expect.poll(async () => (await chart.boundingBox())?.height).toBe(chartHeight);

  await toggle.click();
  await expect(toggle).toHaveText('Show raw request');
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await expect(rawRequest).not.toBeVisible();
});

test('opens and closes an API test card', async ({ page }) => {
  const card = page.getByRole('button', { name: /orders-api/ });
  await expect(card).toHaveAttribute('aria-expanded', 'false');
  await expect(page.getByText('Path')).not.toBeVisible();

  await card.click();
  await expect(card).toHaveAttribute('aria-expanded', 'true');
  await expect(page.getByText('Path')).toBeVisible();

  await card.click();
  await expect(card).toHaveAttribute('aria-expanded', 'false');
  await expect(page.getByText('Path')).not.toBeVisible();
});

test('drills into the findings behind a criterion, uncovered ones first', async ({ page }) => {
  const report = qualityGateReport({
    calculationId: CALCULATION_ID,
    interfaces: [
      {
        serviceName: 'order-service',
        apiName: 'orders-api',
        apiVersion: '1.0.0',
        status: 'FAILED',
        testResults: [
          {
            id: 'PATH_COVERAGE',
            coverage: 0.5,
            isIncludedInQualityGate: true,
            findings: [
              { status: 'NOT_APPLICABLE', specPointer: '#/paths/~1health', httpPath: '/health', evidence: [] },
              {
                status: 'COVERED',
                specPointer: '#/paths/~1orders',
                httpPath: '/orders',
                evidence: [{ traceId: '4bf92f3577b34da6a3ce929d0e0e4736', testCaseName: 'OrderApiIT#listsOrders' }],
              },
              { status: 'UNCOVERED', specPointer: '#/paths/~1orders~1{id}', httpPath: '/orders/{id}', evidence: [] },
            ],
          },
        ],
      },
    ],
  });
  await mockJson(page, reportById(CALCULATION_ID), report);
  await page.reload();

  await page.getByRole('button', { name: /orders-api/ }).click();
  const toggle = page.getByRole('button', { name: 'Show findings for Path' });
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');

  await toggle.click();
  const findings = dataCy(page, 'apiTestFindings');
  await expect(page.getByRole('button', { name: 'Hide findings for Path' })).toHaveAttribute('aria-expanded', 'true');
  await expect(findings.locator('[data-cy^="findings-"]')).toHaveCount(3);
  await expect(findings.locator('[data-cy^="findings-"]').first()).toHaveAttribute('data-cy', 'findings-UNCOVERED');
  await expect(findings.getByText('/orders/{id}')).toBeVisible();
  await expect(dataCy(page, 'findingEvidence')).toHaveText('OrderApiIT#listsOrders');
  await expect(findings.getByText('/health')).not.toBeVisible();

  await findings.getByRole('button', { name: /Not applicable/ }).click();
  await expect(findings.getByText('/health')).toBeVisible();

  await page.getByRole('button', { name: 'Hide findings for Path' }).click();
  await expect(findings).toHaveCount(0);
});

test('offers no drilldown for a result without findings', async ({ page }) => {
  await page.getByRole('button', { name: /orders-api/ }).click();

  await expect(page.getByText('Path')).toBeVisible();
  await expect(page.getByRole('button', { name: /findings for/ })).toHaveCount(0);
});

test('links to the JUnit report download', async ({ page }) => {
  await expect(page.getByRole('link', { name: 'Download JUnit Report' })).toHaveAttribute(
    'href',
    `/api/rest/v1/reports/${CALCULATION_ID}/junit`,
  );
});
