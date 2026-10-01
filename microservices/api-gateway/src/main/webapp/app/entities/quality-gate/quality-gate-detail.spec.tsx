/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IQualityGate } from 'app/shared/model/quality-gate.model';

import { fireEvent, render, screen } from '@testing-library/react';
import { QualityGateDetail } from 'app/entities/quality-gate/quality-gate-detail';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router';

const dispatch = jest.fn();
let state: { loading: boolean; entity: IQualityGate };

jest.mock('app/config/store', () => ({
  useAppDispatch: () => dispatch,
  useAppSelector: (selector: (state: unknown) => unknown) => selector({ snowwhite: { qualityGate: state } }),
}));

jest.mock('app/entities/quality-gate/quality-gate.reducer', () => ({
  getEntity: jest.fn((id: string) => ({ type: 'getEntity', payload: id })),
}));

jest.mock('app/entities/quality-gate/quality-gate-summary', () => ({
  QualityGateSummary: () => <div data-testid="quality-gate-summary" />,
}));

jest.mock('app/entities/quality-gate/api-test-service-group', () => ({
  ApiTestServiceGroup: ({ serviceName }: { serviceName: string }) => <div data-testid="api-test-service-group">{serviceName}</div>,
}));

jest.mock('react-jhipster', () => ({
  Translate: ({ contentKey }: { contentKey: string }) => <span>{contentKey}</span>,
  translate: (key: string) => key,
}));

describe('QualityGateDetail', () => {
  const CALCULATION_ID = 'calc-id';

  const renderDetail = (entity: IQualityGate, loading = false) => {
    state = { loading, entity };
    return render(
      <MemoryRouter initialEntries={[`/quality-gate/${CALCULATION_ID}`]}>
        <Routes>
          <Route path="/quality-gate/:id" element={<QualityGateDetail />} />
        </Routes>
      </MemoryRouter>,
    );
  };

  // Named by `aria-label`, because the visible label is hidden below the `md` breakpoint.
  const downloadLink = (action: 'junitDownload' | 'reportDownload') =>
    screen.getByRole('link', { name: `snowWhiteApp.qualityGate.action.${action}` });

  beforeEach(() => {
    dispatch.mockClear();
  });

  it('should download the report read itself, named after the calculation it belongs to', () => {
    renderDetail({ calculationId: CALCULATION_ID, status: ReportStatus.PASSED });

    // The endpoint the page already read, so the saved file is that response body rather than the
    // Redux entity the drilldown renders from - see ADR-0003.
    expect(downloadLink('reportDownload')).toHaveAttribute('href', `/api/rest/v1/reports/${CALCULATION_ID}`);
    expect(downloadLink('reportDownload')).toHaveAttribute('download', `snow-white-report-${CALCULATION_ID}.json`);
  });

  it('should keep the JUnit download beside it', () => {
    renderDetail({ calculationId: CALCULATION_ID, status: ReportStatus.PASSED });

    expect(downloadLink('junitDownload')).toHaveAttribute('href', `/api/rest/v1/reports/${CALCULATION_ID}/junit`);
  });

  it('should name each download where the label is hidden, leaving one tab stop per control', () => {
    renderDetail({ calculationId: CALCULATION_ID });

    // A `<button>` nested in an `<a>` would be two focusable controls for one action.
    expect(downloadLink('reportDownload').querySelector('button')).toBeNull();
    expect(downloadLink('junitDownload').querySelector('button')).toBeNull();
  });

  it('should offer neither download before the report has been read', () => {
    renderDetail({}, true);

    expect(screen.queryByText('snowWhiteApp.qualityGate.action.reportDownload')).not.toBeInTheDocument();
    expect(screen.queryByText('snowWhiteApp.qualityGate.action.junitDownload')).not.toBeInTheDocument();
  });

  it('should read the report named in the route', () => {
    renderDetail({ calculationId: CALCULATION_ID });

    expect(dispatch).toHaveBeenCalledWith({ type: 'getEntity', payload: CALCULATION_ID });
  });

  it('should group the API tests by the service that contributed them', () => {
    renderDetail({
      calculationId: CALCULATION_ID,
      apiTests: [
        { serviceName: 'order-service', apiName: 'orders-api' },
        { serviceName: 'order-service', apiName: 'payments-api' },
        { serviceName: 'shipping-service', apiName: 'shipments-api' },
      ],
    });

    expect(screen.getAllByTestId('api-test-service-group').map(group => group.textContent)).toEqual(['order-service', 'shipping-service']);
  });

  it('should say so when the report holds no API tests', () => {
    renderDetail({ calculationId: CALCULATION_ID });

    expect(screen.getByText('snowWhiteApp.qualityGate.home.notFound')).toBeInTheDocument();
  });

  it('should show the excluded results once the filter is switched off', () => {
    renderDetail({ calculationId: CALCULATION_ID, apiTests: [{ serviceName: 'order-service', apiName: 'orders-api' }] });

    const filter = screen.getByRole('switch');
    expect(filter).toBeChecked();

    fireEvent.click(filter);

    expect(filter).not.toBeChecked();
  });
});
