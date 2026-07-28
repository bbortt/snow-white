/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';

import type { QualityGateApi } from '../../clients/quality-gate-api';
import type { ReportApi } from '../../clients/report-api';
import type { CalculateOptions } from '../../config/sanitized-options';

import { toDtos } from '../../entity/mapper/api-information.mapper';
import { calculateQualityGates } from './calculate-quality-gates';
import { persistJUnitXmlReport } from './persist-junit-xml-report';
import { pollCalculationResult } from './poll-calculation-result';

void mock.module('./persist-junit-xml-report', () => ({
  persistJUnitXmlReport: mock(() => undefined),
}));

void mock.module('./poll-calculation-result', () => ({
  // eslint-disable-next-line @typescript-eslint/require-await
  pollCalculationResult: mock(async () => true),
}));

void mock.module('../../entity/mapper/api-information.mapper', () => ({
  toDtos: mock(() => [{ id: 'mapped-api' }]),
}));

describe('calculateQualityGates', () => {
  let consoleLogSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleLogSpy = spyOn(console, 'log').mockImplementation(() => {});

    pollCalculationResult.mockClear();
    persistJUnitXmlReport.mockClear();
    toDtos.mockClear();
  });

  afterEach(() => {
    consoleLogSpy.mockClear();
  });

  const createOptions = (): CalculateOptions =>
    ({
      apiInformation: [{ id: 'api-1' }],
      async: false,
      attributeFilters: {
        domain: 'payments',
      },
      junitOutput: './report.xml',
      lookbackWindow: '30d',
      qualityGate: 'default',
      url: 'https://example.com',
    }) as unknown as CalculateOptions;

  it('should initiate quality gate calculation with correct payload', async () => {
    const apiResponse = {
      raw: {
        headers: {
          get: mock(() => 'https://example.com/report/123'),
        },
      },
      value: mock(() => ({
        calculationId: 'calc-123',
      })),
    };

    const qualityGateApi: QualityGateApi = {
      calculateQualityGateRaw: mock(() => apiResponse),
    } as unknown as QualityGateApi;

    const reportApi = {} as ReportApi;

    const options = createOptions();

    await calculateQualityGates(qualityGateApi, reportApi, options);

    expect(qualityGateApi.calculateQualityGateRaw).toHaveBeenCalledWith({
      calculateQualityGateRequest: {
        attributeFilters: options.attributeFilters,
        includeApis: [{ id: 'mapped-api' }],
        lookbackWindow: options.lookbackWindow,
      },
      qualityGateConfigName: options.qualityGate,
    });

    expect(toDtos).toHaveBeenCalledWith(options.apiInformation);
  });

  it('should poll calculation result when async mode is disabled', async () => {
    const apiResponse = {
      raw: {
        headers: {
          get: mock(() => null),
        },
      },
      value: mock(() => ({
        calculationId: 'calc-789',
      })),
    };

    const qualityGateApi: QualityGateApi = {
      calculateQualityGateRaw: mock(() => apiResponse),
    } as unknown as QualityGateApi;

    const reportApi = {} as ReportApi;

    const options = createOptions();
    options.junitOutput = './quality-report.xml';

    await calculateQualityGates(qualityGateApi, reportApi, options);

    expect(persistJUnitXmlReport).toHaveBeenCalledWith(reportApi, 'calc-789', './quality-report.xml');
  });

  it('should not poll calculation result in async mode', async () => {
    const apiResponse = {
      raw: {
        headers: {
          get: mock(() => null),
        },
      },
      value: mock(() => ({
        calculationId: 'calc-999',
      })),
    };

    const qualityGateApi: QualityGateApi = {
      calculateQualityGateRaw: mock(() => apiResponse),
    } as unknown as QualityGateApi;

    const reportApi = {} as ReportApi;

    const options = createOptions();
    options.async = true;

    await calculateQualityGates(qualityGateApi, reportApi, options);

    expect(pollCalculationResult).not.toHaveBeenCalled();
    expect(persistJUnitXmlReport).not.toHaveBeenCalled();
    expect(apiResponse.value).not.toHaveBeenCalled();
  });

  it('should log location header when available', async () => {
    const apiResponse = {
      raw: {
        headers: {
          get: mock(() => 'https://example.com/calculation/abc'),
        },
      },
      value: mock(() => ({
        calculationId: 'calc-log',
      })),
    };

    const qualityGateApi: QualityGateApi = {
      calculateQualityGateRaw: mock(() => apiResponse),
    } as unknown as QualityGateApi;

    const reportApi = {} as ReportApi;

    await calculateQualityGates(qualityGateApi, reportApi, createOptions());

    expect(consoleLogSpy).toHaveBeenCalledWith('Location: https://example.com/calculation/abc');
  });
});
