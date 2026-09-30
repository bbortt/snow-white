/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IQualityGate } from 'app/shared/model/quality-gate.model';

import { fireEvent, render, screen } from '@testing-library/react';
import { QualityGateSummary } from 'app/entities/quality-gate/quality-gate-summary';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React from 'react';
import { MemoryRouter } from 'react-router';

jest.mock('app/entities/quality-gate/shape-pie-chart', () => ({
  ShapePieChart: () => <div data-testid="shape-pie-chart" />,
}));

jest.mock('app/entities/quality-gate/code-highlight-block', () => ({
  CodeHighlightBlock: ({ code }: { code: string }) => <pre data-testid="code-highlight-block">{code}</pre>,
}));

jest.mock('react-jhipster', () => ({
  TextFormat: ({ value }: { value: string }) => <span>{value}</span>,
  Translate: ({ contentKey }: { contentKey: string }) => <span>{contentKey}</span>,
  translate: (key: string) => key,
}));

describe('QualityGateSummary', () => {
  const qualityGate = (calculationRequest?: IQualityGate['calculationRequest']): IQualityGate => ({
    calculationId: 'calc-id',
    qualityGateConfig: { name: 'default' },
    status: ReportStatus.PASSED,
    createdAt: '2026-08-01T10:00:00Z',
    apiTests: [
      { serviceName: 'order-service', apiName: 'orders-api' },
      { serviceName: 'order-service', apiName: 'payments-api' },
    ],
    calculationRequest,
  });

  const renderSummary = (entity: IQualityGate) =>
    render(
      <MemoryRouter>
        <QualityGateSummary qualityGate={entity} />
      </MemoryRouter>,
    );

  it('should render the lookback window the calculation was triggered with', () => {
    const { container } = renderSummary(qualityGate({ lookbackWindow: '24h' }));

    expect(container.querySelector('[data-cy="lookbackWindow"]')).toHaveTextContent('24h');
  });

  it('should point at the service default when no lookback window was sent', () => {
    const { container } = renderSummary(qualityGate({}));

    expect(container.querySelector('[data-cy="lookbackWindow"]')).toHaveTextContent(
      'snowWhiteApp.qualityGate.trigger.lookbackWindowDefault',
    );
  });

  it('should render every attribute filter as its own key-value pair', () => {
    const { container } = renderSummary(qualityGate({ attributeFilters: { environment: 'production', region: 'us-west-1' } }));

    const filters = container.querySelectorAll('[data-cy="attributeFilters"] .badge');
    expect(Array.from(filters).map(filter => filter.textContent)).toEqual(['environment = production', 'region = us-west-1']);
  });

  it('should say so when no attribute filters were sent', () => {
    const { container } = renderSummary(qualityGate({ attributeFilters: {} }));

    expect(container.querySelector('[data-cy="attributeFilters"]')).toHaveTextContent(
      'snowWhiteApp.qualityGate.trigger.noAttributeFilters',
    );
  });

  it('should count the tested APIs', () => {
    const { container } = renderSummary(qualityGate({}));

    expect(container.querySelector('[data-cy="testedAPIs"]')).toHaveTextContent('2');
  });

  it('should keep the raw request behind a toggle', () => {
    renderSummary(qualityGate({ lookbackWindow: '24h' }));

    const toggle = screen.getByRole('button', { name: 'snowWhiteApp.qualityGate.trigger.showRawRequest' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(toggle);

    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(toggle).toHaveTextContent('snowWhiteApp.qualityGate.trigger.hideRawRequest');
    expect(screen.getByTestId('code-highlight-block')).toHaveTextContent('"lookbackWindow": "24h"');

    fireEvent.click(toggle);

    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveTextContent('snowWhiteApp.qualityGate.trigger.showRawRequest');
  });

  it('should offer no raw request toggle without a calculation request', () => {
    renderSummary(qualityGate());

    expect(screen.queryByRole('button', { name: /RawRequest/ })).not.toBeInTheDocument();
  });
});
