/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { openApiCriterion } from './support/data';
import { criteria } from './support/endpoints';
import { mockJson } from './support/mock-backend';
import { expect, test } from './support/test';

test('shows the not-found alert when no criteria are configured', async ({ page }) => {
  await mockJson(page, criteria(), []);

  await page.goto('/open-api-criterion');

  await expect(page.getByText('No OpenAPI Coverage Criteria found')).toBeVisible();
});

test('renders the list of criteria with their translated names', async ({ page }) => {
  await mockJson(page, criteria(), [
    openApiCriterion({ id: 'PATH_COVERAGE' }),
    openApiCriterion({ id: 'HTTP_METHOD_COVERAGE', name: 'HTTP Method Coverage' }),
  ]);

  await page.goto('/open-api-criterion');

  const rows = page.getByTestId('openApiCoverageCriteriaTable');
  await expect(rows).toHaveCount(2);
  // Exact cell match: several criteria descriptions legitimately contain "path" as a substring
  // (e.g. HTTP_METHOD_COVERAGE's "...for each path has been tested"), so `hasText` would collide.
  await expect(page.getByRole('cell', { name: 'Path', exact: true })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'HTTP Method', exact: true })).toBeVisible();
});

// There is currently no in-app link into this detail page (see open-api-coverage-criteria.tsx
// and open-api-criterion-badge.tsx, neither of which render one) - it is only reachable by
// direct URL. Testing it directly still covers real, working route/reducer behaviour.
test('navigating to a criterion detail page directly renders it, and back returns to the list', async ({ page }) => {
  await mockJson(page, criteria(), [openApiCriterion({ id: 'PATH_COVERAGE', name: 'Path Coverage (raw label)' })]);

  await page.goto('/open-api-criterion');
  await page.goto('/open-api-criterion/PATH_COVERAGE');

  await expect(page.getByTestId('openApiCriterionDetailsHeading')).toBeVisible();
  await expect(page.getByText('PATH_COVERAGE')).toBeVisible();
  await expect(page.getByText('Path Coverage (raw label)')).toBeVisible();

  await page.getByTestId('entityDetailsBackButton').click();

  await expect(page).toHaveURL(/\/open-api-criterion$/);
  await expect(page.getByTestId('OpenApiCriterionHeading')).toBeVisible();
});
