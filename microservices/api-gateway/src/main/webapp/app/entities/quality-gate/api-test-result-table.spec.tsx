/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';
import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { ApiTestResultTable } from 'app/entities/quality-gate/api-test-result-table';
import { FindingStatus } from 'app/shared/model/enumerations/finding-status.model';
import React from 'react';

jest.mock('app/config/store', () => ({
  useAppDispatch: jest.fn(),
  useAppSelector: jest.fn(),
}));

jest.mock('app/entities/open-api-criterion/open-api-criterion.reducer', () => ({
  getEntities: jest.fn(() => ({ type: 'GET_ENTITIES' })),
}));

jest.mock('app/entities/quality-gate/api-criterion-info', () => ({
  __esModule: true,
  default: ({ apiCriterion }: { apiCriterion: IOpenApiCriterion }) => <div data-testid="api-criterion-info">{apiCriterion.name}</div>,
}));

jest.mock('app/entities/quality-gate/api-test-findings', () => ({
  __esModule: true,
  default: ({ findings }: { findings: unknown[] }) => <div data-testid="api-test-findings">{findings.length}</div>,
}));

jest.mock('react-jhipster', () => ({
  Translate: ({ contentKey }: { contentKey: string }) => <div data-testid="translate">{contentKey}</div>,
  translate: (key: string, interpolate?: Record<string, unknown>) =>
    interpolate
      ? `${key}:${Object.entries(interpolate)
          .map(([name, value]) => `${name}=${String(value)}`)
          .join(',')}`
      : key,
}));

describe('ApiTestResultTable', () => {
  const openApiCriterion = (name: string): IOpenApiCriterion => ({ name });

  const apiTestResult = (overrides: Partial<IApiTestResult> = {}): IApiTestResult => ({
    id: 'CRITERION_A',
    coverage: 1,
    additionalInformation: 'some info',
    isIncludedInQualityGate: true,
    ...overrides,
  });

  const returnOpenApiCriterionList = (entities: IOpenApiCriterion[] | undefined) => {
    (useAppSelector as jest.MockedFn<(reducer: (state: any) => any) => any>).mockImplementation(reducer =>
      reducer({
        snowwhite: {
          openApiCriterion: {
            entities,
          },
        },
      }),
    );
  };

  beforeEach(() => {
    jest.clearAllMocks();

    (useAppDispatch as jest.MockedFn<() => any>).mockReturnValue(jest.fn());
  });

  it('should dispatch getEntities on mount', () => {
    const dispatch = jest.fn();
    (useAppDispatch as jest.MockedFn<() => any>).mockReturnValue(dispatch);
    returnOpenApiCriterionList([]);

    render(<ApiTestResultTable apiTestResults={[]} />);

    expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({ type: 'GET_ENTITIES' }));
  });

  it('should render no rows while the criterion list is undefined', () => {
    returnOpenApiCriterionList(undefined);

    render(<ApiTestResultTable apiTestResults={[apiTestResult()]} />);

    expect(screen.queryByText('some info')).not.toBeInTheDocument();
  });

  it('should render no rows while the criterion list is empty', () => {
    returnOpenApiCriterionList([]);

    render(<ApiTestResultTable apiTestResults={[apiTestResult()]} />);

    expect(screen.queryByText('some info')).not.toBeInTheDocument();
  });

  it('should skip results whose criterion is not found in the loaded list', () => {
    returnOpenApiCriterionList([openApiCriterion('SOME_OTHER_CRITERION')]);

    render(<ApiTestResultTable apiTestResults={[apiTestResult({ id: 'CRITERION_A' })]} />);

    expect(screen.queryByText('some info')).not.toBeInTheDocument();
  });

  it('should render a row for a matching, fully covered and included result', () => {
    returnOpenApiCriterionList([openApiCriterion('CRITERION_A')]);

    render(<ApiTestResultTable apiTestResults={[apiTestResult({ coverage: 1, isIncludedInQualityGate: true })]} />);

    expect(screen.getByTestId('api-criterion-info')).toHaveTextContent('CRITERION_A');
    expect(screen.getByText('some info')).toBeInTheDocument();
    expect(screen.getByText('100 %')).toBeInTheDocument();
    expect(screen.getByText('0 %')).toBeInTheDocument();

    const includedIcon = document.querySelector('#included-CRITERION_A svg');
    expect(includedIcon).toHaveClass('text-success');
  });

  it('should render a row for a partially covered and excluded result', () => {
    returnOpenApiCriterionList([openApiCriterion('CRITERION_A')]);

    render(<ApiTestResultTable apiTestResults={[apiTestResult({ coverage: 0.5, isIncludedInQualityGate: false })]} />);

    expect(screen.getAllByText('50 %')).toHaveLength(2);

    const excludedIcon = document.querySelector('#included-CRITERION_A svg');
    expect(excludedIcon).toHaveClass('text-muted');
  });

  it('should treat missing coverage as 0%', () => {
    returnOpenApiCriterionList([openApiCriterion('CRITERION_A')]);

    render(<ApiTestResultTable apiTestResults={[apiTestResult({ coverage: undefined })]} />);

    expect(screen.getByText('0 %')).toBeInTheDocument();
    expect(screen.getByText('100 %')).toBeInTheDocument();
  });

  describe('waived targets', () => {
    beforeEach(() => returnOpenApiCriterionList([openApiCriterion('CRITERION_A')]));

    it('should mark a criterion waivers emptied as fully waived, never as a 100 % bar', () => {
      render(<ApiTestResultTable apiTestResults={[apiTestResult({ coverage: 1, judgedTargetCount: 0, waivedTargetCount: 2 })]} />);

      expect(document.querySelector('[data-cy="fullyWaived"]')).toHaveTextContent('snowWhiteApp.apiTestResult.fullyWaived');
      expect(screen.queryByText('100 %')).not.toBeInTheDocument();
      expect(document.querySelector('[data-cy="waivedTargets"]')).toHaveTextContent(
        'snowWhiteApp.apiTestResult.waivedTargetsOnly:waived=2,judged=0',
      );
    });

    it('should keep the real bar of a partly waived criterion and say what it was measured over', () => {
      render(<ApiTestResultTable apiTestResults={[apiTestResult({ coverage: 0.75, judgedTargetCount: 4, waivedTargetCount: 1 })]} />);

      expect(screen.getByText('75 %')).toBeInTheDocument();
      expect(document.querySelector('[data-cy="fullyWaived"]')).not.toBeInTheDocument();
      expect(document.querySelector('[data-cy="waivedTargets"]')).toHaveTextContent(
        'snowWhiteApp.apiTestResult.waivedTargets:waived=1,judged=4',
      );
    });

    it('should show a criterion with nothing to judge and no waiver as it always has', () => {
      render(<ApiTestResultTable apiTestResults={[apiTestResult({ coverage: 1, judgedTargetCount: 0, waivedTargetCount: 0 })]} />);

      expect(screen.getByText('100 %')).toBeInTheDocument();
      expect(document.querySelector('[data-cy="fullyWaived"]')).not.toBeInTheDocument();
      expect(document.querySelector('[data-cy="waivedTargets"]')).not.toBeInTheDocument();
    });

    it('should show a report persisted before the counts existed as it always has', () => {
      render(<ApiTestResultTable apiTestResults={[apiTestResult({ coverage: 1 })]} />);

      expect(screen.getByText('100 %')).toBeInTheDocument();
      expect(document.querySelector('[data-cy="fullyWaived"]')).not.toBeInTheDocument();
      expect(document.querySelector('[data-cy="waivedTargets"]')).not.toBeInTheDocument();
    });
  });

  it('should sort rows by result id', () => {
    returnOpenApiCriterionList([openApiCriterion('CRITERION_A'), openApiCriterion('CRITERION_B')]);

    render(
      <ApiTestResultTable
        apiTestResults={[
          apiTestResult({ id: 'CRITERION_B', additionalInformation: 'info B' }),
          apiTestResult({ id: 'CRITERION_A', additionalInformation: 'info A' }),
        ]}
      />,
    );

    const infoCells = screen.getAllByTestId('api-criterion-info');
    expect(infoCells.map(cell => cell.textContent)).toEqual(['CRITERION_A', 'CRITERION_B']);
  });

  describe('findings', () => {
    const finding = { status: FindingStatus.UNCOVERED, specPointer: '#/paths/~1orders/get', evidence: [] };

    it('should offer no drilldown without findings', () => {
      returnOpenApiCriterionList([openApiCriterion('CRITERION_A')]);

      render(<ApiTestResultTable apiTestResults={[apiTestResult({ findings: [] }), apiTestResult({ id: 'CRITERION_B' })]} />);

      expect(screen.queryByRole('button', { name: /^snowWhiteApp\.apiTestResult\.findings\.show:/ })).not.toBeInTheDocument();
    });

    it('should open the findings below the criterion and close them again', async () => {
      returnOpenApiCriterionList([openApiCriterion('CRITERION_A')]);

      render(<ApiTestResultTable apiTestResults={[apiTestResult({ findings: [finding] })]} />);

      const toggle = screen.getByRole('button', { name: /^snowWhiteApp\.apiTestResult\.findings\.show:/ });
      expect(toggle).toHaveAttribute('aria-expanded', 'false');
      expect(screen.queryByTestId('api-test-findings')).not.toBeInTheDocument();

      fireEvent.click(toggle);

      expect(toggle).toHaveAttribute('aria-expanded', 'true');
      expect(toggle).toHaveAccessibleName(/^snowWhiteApp\.apiTestResult\.findings\.hide:/);
      expect(screen.getByTestId('api-test-findings')).toHaveTextContent('1');
      expect(screen.getByTestId('api-test-findings').closest('tr')).toHaveAttribute('id', toggle.getAttribute('aria-controls'));

      fireEvent.click(toggle);

      expect(toggle).toHaveAttribute('aria-expanded', 'false');
      await waitFor(() => expect(screen.queryByTestId('api-test-findings')).not.toBeInTheDocument());
    });
  });
});
