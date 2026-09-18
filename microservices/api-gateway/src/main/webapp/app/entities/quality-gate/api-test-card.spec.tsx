/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';
import type { IApiTest } from 'app/shared/model/api-test.model';

import { render, screen } from '@testing-library/react';
import { ApiTestCard } from 'app/entities/quality-gate/api-test-card';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React from 'react';

jest.mock('uuid', () => ({ v4: () => 'test-uuid' }));

jest.mock('app/entities/quality-gate/api-test-result-table', () => ({
  __esModule: true,
  default: ({ apiTestResults }: { apiTestResults: IApiTestResult[] }) => (
    <div data-testid="api-test-result-table">{apiTestResults.length} results</div>
  ),
}));

jest.mock('react-jhipster', () => ({
  Translate: ({ contentKey }: { contentKey: string }) => <div data-testid="translate">{contentKey}</div>,
  translate: (key: string) => key,
}));

describe('ApiTestCard', () => {
  const apiTest = (status?: ReportStatus): IApiTest => ({
    serviceName: 'my-service',
    apiName: 'my-api',
    apiVersion: '1.0.0',
    apiType: 'REST',
    testResults: [],
    status,
  });

  const renderBadgeColor = (status: ReportStatus | undefined, qualityGateTimedOut: boolean): string => {
    const { container } = render(
      <ApiTestCard apiTest={apiTest(status)} showOnlyIncluded={false} qualityGateTimedOut={qualityGateTimedOut} />,
    );

    const badge = container.querySelector('.badge');
    return ['bg-success', 'bg-danger', 'bg-dark', 'bg-warning', 'bg-info'].find(className => badge?.classList.contains(className));
  };

  describe.each([
    { status: ReportStatus.PASSED, expectedColor: 'bg-success' },
    { status: ReportStatus.FAILED, expectedColor: 'bg-danger' },
    { status: ReportStatus.FINISHED_EXCEPTIONALLY, expectedColor: 'bg-dark' },
  ])('when the test already concluded with status: $status', ({ status, expectedColor }) => {
    it('should keep the concluded status regardless of quality-gate timeout', () => {
      expect(renderBadgeColor(status, true)).toEqual(expectedColor);
      expect(renderBadgeColor(status, false)).toEqual(expectedColor);
    });
  });

  describe('when the test has not concluded with a final status', () => {
    it.each([undefined, ReportStatus.IN_PROGRESS])('should show TIMED_OUT when the quality-gate timed out (status: %s)', status => {
      expect(renderBadgeColor(status, true)).toEqual('bg-warning');
    });

    it.each([undefined, ReportStatus.IN_PROGRESS])(
      'should show NOT_STARTED when the quality-gate has not timed out (status: %s)',
      status => {
        expect(renderBadgeColor(status, false)).toEqual('bg-info');
      },
    );
  });

  describe('card content', () => {
    it('should render the stack trace when present, regardless of test results', () => {
      render(
        <ApiTestCard
          apiTest={{ ...apiTest(), stackTrace: 'java.lang.Exception: boom', testResults: [{ isIncludedInQualityGate: true }] }}
          showOnlyIncluded={false}
          qualityGateTimedOut={false}
        />,
      );

      expect(screen.getByText('java.lang.Exception: boom')).toBeInTheDocument();
      expect(screen.queryByTestId('api-test-result-table')).not.toBeInTheDocument();
      expect(screen.queryByText('snowWhiteApp.apiTestResult.home.notFound')).not.toBeInTheDocument();
    });

    it('should render the result table when test results are present', () => {
      render(
        <ApiTestCard
          apiTest={{ ...apiTest(), testResults: [{ isIncludedInQualityGate: true }] }}
          showOnlyIncluded={false}
          qualityGateTimedOut={false}
        />,
      );

      expect(screen.getByTestId('api-test-result-table')).toHaveTextContent('1 results');
    });

    it('should render a not-found alert when there are no test results and no stack trace', () => {
      render(<ApiTestCard apiTest={apiTest()} showOnlyIncluded={false} qualityGateTimedOut={false} />);

      expect(screen.getByText('snowWhiteApp.apiTestResult.home.notFound')).toBeInTheDocument();
      expect(screen.queryByTestId('api-test-result-table')).not.toBeInTheDocument();
    });

    it('should only pass results included in the quality gate to the coverage progress bar and result table', () => {
      render(
        <ApiTestCard
          apiTest={{
            ...apiTest(),
            testResults: [{ isIncludedInQualityGate: true }, { isIncludedInQualityGate: false }],
          }}
          showOnlyIncluded={true}
          qualityGateTimedOut={false}
        />,
      );

      expect(screen.getByTestId('api-test-result-table')).toHaveTextContent('1 results');
    });
  });
});
