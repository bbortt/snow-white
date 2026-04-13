/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';
import { exit } from 'node:process';

import type { QualityGateApi } from '../clients/quality-gate-api';
import type { ReportApi } from '../clients/report-api';
import type { CalculateOptions } from '../config/sanitized-options';

import { FetchError, ResponseError } from '../clients/quality-gate-api/runtime';
import { QUALITY_GATE_CALCULATION_FAILED } from '../common/exit-codes';
import { calculate } from './calculate';

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:process', () => ({
  exit: mock(),
}));

const mockConsoleLog = spyOn(console, 'log');
const mockConsoleError = spyOn(console, 'error');

const getQualityGateApi = (qualityGateApiMock: unknown): QualityGateApi => {
  return qualityGateApiMock as QualityGateApi;
};

const getReportApi = (reportApiMock: unknown): ReportApi => {
  return reportApiMock as ReportApi;
};

describe('calculate action', () => {
  const qualityGateApiMock = {
    calculateQualityGateRaw: mock(),
  };

  const reportApiMock = {
    getReportByCalculationId: mock(),
  };

  const defaultOptions: CalculateOptions = {
    apiInformation: [{ apiName: 'test-api', apiVersion: '1.0.0', serviceName: 'test-service' }],
    async: true,
    qualityGate: 'test-gate',
    url: 'http://localhost:8080',
  };

  beforeEach(() => {
    mockConsoleLog.mockReset();
    mockConsoleError.mockReset();

    qualityGateApiMock.calculateQualityGateRaw.mockReset();
    reportApiMock.getReportByCalculationId.mockReset();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  describe('successful calculation (async: true)', () => {
    it('should successfully initiate quality gate calculation', async () => {
      const mockLocation = 'http://localhost:8080/api/rest/v1/quality-gates/reports/123-456-789';
      const mockApiResponse = {
        raw: {
          headers: {
            get: (name: string) => (name.toLowerCase() === 'location' ? mockLocation : null),
          },
        },
        // eslint-disable-next-line @typescript-eslint/require-await
        value: async () => ({
          calculationId: '123-456-789',
          calculationRequest: {},
          initiatedAt: new Date(),
          qualityGateConfigName: 'test-gate',
          status: 'IN_PROGRESS',
        }),
      };

      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(mockApiResponse);

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), defaultOptions);

      expect(qualityGateApiMock.calculateQualityGateRaw).toHaveBeenCalledWith({
        calculateQualityGateRequest: {
          attributeFilters: undefined,
          includeApis: [...defaultOptions.apiInformation],
          lookbackWindow: undefined,
        },
        qualityGateConfigName: defaultOptions.qualityGate,
      });

      expect(mockConsoleLog).toHaveBeenNthCalledWith(1, expect.stringContaining('🚀  Starting Quality-Gate calculation for 1 API(s)...'));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(2, expect.stringContaining(`Base URL: ${defaultOptions.url}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(4, expect.stringContaining('✅ Quality-Gate calculation initiated successfully!'));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(6, expect.stringContaining(`Location: ${mockLocation}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(
        8,
        expect.stringContaining('💡  Use the returned URL to check the calculation report.'),
      );
    });

    it('should include lookbackWindow in request when provided', async () => {
      const mockApiResponse = {
        raw: { headers: { get: () => null } },
        // eslint-disable-next-line @typescript-eslint/require-await
        value: async () => ({ calculationId: '123-456-789', status: 'IN_PROGRESS' }),
      };

      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(mockApiResponse);

      const optionsWithLookback: CalculateOptions = {
        ...defaultOptions,
        lookbackWindow: '24h',
      };

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), optionsWithLookback);

      expect(qualityGateApiMock.calculateQualityGateRaw).toHaveBeenCalledWith({
        calculateQualityGateRequest: {
          attributeFilters: undefined,
          includeApis: [...defaultOptions.apiInformation],
          lookbackWindow: '24h',
        },
        qualityGateConfigName: defaultOptions.qualityGate,
      });

      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('Lookback window: 24h'));
    });

    it('should include attributeFilters in request when provided', async () => {
      const mockApiResponse = {
        raw: { headers: { get: () => null } },
        // eslint-disable-next-line @typescript-eslint/require-await
        value: async () => ({ calculationId: '123-456-789', status: 'IN_PROGRESS' }),
      };

      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(mockApiResponse);

      const optionsWithFilters: CalculateOptions = {
        ...defaultOptions,
        attributeFilters: { environment: 'production', region: 'us-west-1' },
      };

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), optionsWithFilters);

      expect(qualityGateApiMock.calculateQualityGateRaw).toHaveBeenCalledWith({
        calculateQualityGateRequest: {
          attributeFilters: { environment: 'production', region: 'us-west-1' },
          includeApis: [...defaultOptions.apiInformation],
          lookbackWindow: undefined,
        },
        qualityGateConfigName: defaultOptions.qualityGate,
      });

      expect(mockConsoleLog).toHaveBeenCalledWith(
        expect.stringContaining('Attribute filters: {"environment":"production","region":"us-west-1"}'),
      );
    });

    it('should include both lookbackWindow and attributeFilters when provided', async () => {
      const mockApiResponse = {
        raw: { headers: { get: () => null } },
        // eslint-disable-next-line @typescript-eslint/require-await
        value: async () => ({ calculationId: '123-456-789', status: 'IN_PROGRESS' }),
      };

      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(mockApiResponse);

      const fullOptions: CalculateOptions = {
        ...defaultOptions,
        attributeFilters: { environment: 'staging' },
        lookbackWindow: '7d',
      };

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), fullOptions);

      expect(qualityGateApiMock.calculateQualityGateRaw).toHaveBeenCalledWith({
        calculateQualityGateRequest: {
          attributeFilters: { environment: 'staging' },
          includeApis: [...defaultOptions.apiInformation],
          lookbackWindow: '7d',
        },
        qualityGateConfigName: defaultOptions.qualityGate,
      });
    });

    it('should handle response without location header', async () => {
      const mockApiResponse = {
        raw: { headers: { get: () => null } },
        // eslint-disable-next-line @typescript-eslint/require-await
        value: async () => ({ calculationId: '123-456-789', status: 'IN_PROGRESS' }),
      };

      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(mockApiResponse);

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), defaultOptions);

      expect(mockConsoleLog).toHaveBeenNthCalledWith(4, expect.stringContaining('✅ Quality-Gate calculation initiated successfully!'));
      expect(mockConsoleLog).not.toHaveBeenCalledWith(expect.stringContaining('Location:'));
      expect(mockConsoleLog).not.toHaveBeenCalledWith(expect.stringContaining('💡  Use the returned URL to check the calculation report.'));
    });
  });

  describe('synchronous polling (async: false)', () => {
    const syncOptions: CalculateOptions = {
      ...defaultOptions,
      async: false,
    };

    const makeMockApiResponse = (calculationId = '123-456-789') => ({
      raw: {
        headers: {
          get: (name: string) =>
            name.toLowerCase() === 'location' ? `http://localhost:8080/api/rest/v1/quality-gates/reports/${calculationId}` : null,
        },
      },
      // eslint-disable-next-line @typescript-eslint/require-await
      value: async () => ({
        calculationId,
        calculationRequest: {},
        initiatedAt: new Date(),
        qualityGateConfigName: 'test-gate',
        status: 'IN_PROGRESS',
      }),
    });

    it('should poll until PASSED and exit successfully', async () => {
      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(makeMockApiResponse());

      reportApiMock.getReportByCalculationId
        .mockResolvedValueOnce({ calculationId: '123-456-789', status: 'IN_PROGRESS' })
        .mockResolvedValueOnce({ calculationId: '123-456-789', status: 'PASSED' });

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), syncOptions);

      expect(reportApiMock.getReportByCalculationId).toHaveBeenCalledTimes(2);
      expect(reportApiMock.getReportByCalculationId).toHaveBeenCalledWith({ calculationId: '123-456-789' });
      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('⏳  Polling for calculation result...'));
      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('✅ Quality-Gate passed!'));
      expect(exit).not.toHaveBeenCalled();
    });

    it.each(['FAILED', 'TIMED_OUT'])('should poll until %s and exit with non-zero code', async (status: string) => {
      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(makeMockApiResponse());

      reportApiMock.getReportByCalculationId.mockResolvedValueOnce({ calculationId: '123-456-789', status });

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), syncOptions);

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`❌  Quality-Gate calculation ${status}!`));
      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should poll until  and exit with non-zero code', async () => {
      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(makeMockApiResponse());

      reportApiMock.getReportByCalculationId.mockResolvedValueOnce({
        calculationId: '123-456-789',
        stackTrace: 'java.lang.NullPointerException at ...',
        status: 'FINISHED_EXCEPTIONALLY',
      });

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), syncOptions);

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  Quality-Gate calculation FINISHED_EXCEPTIONALLY!'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('java.lang.NullPointerException'));
      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should use calculationId from the response body', async () => {
      const specificId = 'abc-def-123';
      qualityGateApiMock.calculateQualityGateRaw.mockResolvedValue(makeMockApiResponse(specificId));

      reportApiMock.getReportByCalculationId.mockResolvedValueOnce({ calculationId: specificId, status: 'PASSED' });

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), syncOptions);

      expect(reportApiMock.getReportByCalculationId).toHaveBeenCalledWith({ calculationId: specificId });
    });
  });

  describe('error handling', () => {
    it('should handle ResponseError with response', async () => {
      const message = 'Quality-Gate configuration not found';
      const mockErrorResponse = {
        // eslint-disable-next-line @typescript-eslint/require-await
        json: async () => ({ message }),
        status: 404,
        statusText: 'Not Found',
      };

      const responseError = new ResponseError(mockErrorResponse as any, 'Response returned an error code');

      qualityGateApiMock.calculateQualityGateRaw.mockRejectedValue(responseError);

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), defaultOptions);

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('Status: 404'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining(`Details: ${message}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle ResponseError with response but no data', async () => {
      const mockErrorResponse = {
        // eslint-disable-next-line @typescript-eslint/require-await
        json: async () => null,
        status: 501,
        statusText: 'Internal Server Error',
      };

      const responseError = new ResponseError(mockErrorResponse as any, 'Response returned an error code');

      qualityGateApiMock.calculateQualityGateRaw.mockRejectedValue(responseError);

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), defaultOptions);

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('Status: 501'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('Error: Internal Server Error'));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle FetchError (network error)', async () => {
      const fetchError = new FetchError(
        new Error('Network Error'),
        'The request failed and the interceptors did not return an alternative response',
      );

      qualityGateApiMock.calculateQualityGateRaw.mockRejectedValue(fetchError);

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), defaultOptions);

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('No response received from server'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('Check if the service is running and accessible'));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle generic Error', async () => {
      const genericError = new Error('Something went wrong');
      qualityGateApiMock.calculateQualityGateRaw.mockRejectedValue(genericError);

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), defaultOptions);

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Error: ${genericError.message}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle non-Error objects', async () => {
      const unknownError = { custom: 'error object' };
      qualityGateApiMock.calculateQualityGateRaw.mockRejectedValue(unknownError);

      await calculate(getQualityGateApi(qualityGateApiMock), getReportApi(reportApiMock), defaultOptions);

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Error: ${JSON.stringify(unknownError)}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });
  });
});
