/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';
import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';

import { render, screen } from '@testing-library/react';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { ApiTestResultTable } from 'app/entities/quality-gate/api-test-result-table';
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

jest.mock('react-jhipster', () => ({
  Translate: ({ contentKey }: { contentKey: string }) => <div data-testid="translate">{contentKey}</div>,
  translate: (key: string) => key,
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
});
