/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTest } from 'app/shared/model/api-test.model';

import { render, screen } from '@testing-library/react';
import { ApiTestServiceGroup } from 'app/entities/quality-gate/api-test-service-group';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React from 'react';

jest.mock('app/entities/quality-gate/api-test-card', () => ({
  ApiTestCard: ({ apiTest }: { apiTest: IApiTest }) => <li data-testid="api-test-card">{apiTest.apiName}</li>,
}));

jest.mock('react-jhipster', () => ({
  Translate: ({ contentKey, interpolate }: { contentKey: string; interpolate?: Record<string, unknown> }) => (
    <span data-testid="translate">
      {contentKey}
      {interpolate ? JSON.stringify(interpolate) : ''}
    </span>
  ),
  translate: (key: string) => key,
}));

describe('ApiTestServiceGroup', () => {
  const apiTests: IApiTest[] = [
    { serviceName: 'order-service', apiName: 'orders-api', apiVersion: '1.0.0', status: ReportStatus.PASSED },
    { serviceName: 'order-service', apiName: 'payments-api', apiVersion: '2.0.0', status: ReportStatus.FAILED },
  ];

  const renderGroup = (qualityGateTimedOut = false, tests: IApiTest[] = apiTests) =>
    render(
      <ApiTestServiceGroup
        serviceName="order-service"
        apiTests={tests}
        showOnlyIncluded={true}
        qualityGateTimedOut={qualityGateTimedOut}
      />,
    );

  it('should name the service once, as the heading of its section', () => {
    renderGroup();

    expect(screen.getAllByText('order-service')).toHaveLength(1);
    expect(screen.getByRole('region', { name: 'order-service' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 4, name: 'order-service' })).toBeInTheDocument();
  });

  it('should render one entry per API of the service', () => {
    renderGroup();

    expect(screen.getAllByTestId('api-test-card').map(card => card.textContent)).toEqual(['orders-api', 'payments-api']);
  });

  it('should state how many APIs the service contributed', () => {
    renderGroup();

    expect(screen.getByText(/snowWhiteApp\.apiTest\.serviceGroup\.apiCount/)).toHaveTextContent('{"count":2}');
  });

  it('should summarize the service with the most severe status of its APIs', () => {
    const { container } = renderGroup();

    expect(container.querySelector('.badge')).toHaveClass('bg-danger');
  });

  it('should account for a quality-gate timeout when summarizing unconcluded APIs', () => {
    const { container } = renderGroup(true, [
      { serviceName: 'order-service', apiName: 'orders-api', status: ReportStatus.PASSED },
      { serviceName: 'order-service', apiName: 'payments-api', status: ReportStatus.IN_PROGRESS },
    ]);

    expect(container.querySelector('.badge')).toHaveClass('bg-warning');
  });
});
