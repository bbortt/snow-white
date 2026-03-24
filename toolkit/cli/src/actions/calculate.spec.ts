/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';
import { exit } from 'node:process';

import type { QualityGateApi } from '../clients/quality-gate-api';
import type { SanitizedOptions } from '../config/sanitized-options';

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

describe('calculate action', () => {
  const qualityGateApiMock = {
    calculateQualityGateRaw: mock(),
  };

  const defaultOptions: SanitizedOptions = {
    apiInformation: [{ apiName: 'test-api', apiVersion: '1.0.0', serviceName: 'test-service' }],
    qualityGate: 'test-gate',
    url: 'http://localhost:8080',
  };

  beforeEach(() => {
    mockConsoleLog.mockReset();
    mockConsoleError.mockReset();

    qualityGateApiMock.calculateQualityGateRaw.mockReset();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  describe('successful calculation', () => {
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

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

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

      const optionsWithLookback: SanitizedOptions = {
        ...defaultOptions,
        lookbackWindow: '24h',
      };

      await calculate(getQualityGateApi(qualityGateApiMock), optionsWithLookback);

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

      const optionsWithFilters: SanitizedOptions = {
        ...defaultOptions,
        attributeFilters: { environment: 'production', region: 'us-west-1' },
      };

      await calculate(getQualityGateApi(qualityGateApiMock), optionsWithFilters);

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

      const fullOptions: SanitizedOptions = {
        ...defaultOptions,
        attributeFilters: { environment: 'staging' },
        lookbackWindow: '7d',
      };

      await calculate(getQualityGateApi(qualityGateApiMock), fullOptions);

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

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

      expect(mockConsoleLog).toHaveBeenNthCalledWith(4, expect.stringContaining('✅ Quality-Gate calculation initiated successfully!'));
      expect(mockConsoleLog).not.toHaveBeenCalledWith(expect.stringContaining('Location:'));
      expect(mockConsoleLog).not.toHaveBeenCalledWith(expect.stringContaining('💡  Use the returned URL to check the calculation report.'));
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

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

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

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

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

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('No response received from server'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('Check if the service is running and accessible'));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle generic Error', async () => {
      const genericError = new Error('Something went wrong');
      qualityGateApiMock.calculateQualityGateRaw.mockRejectedValue(genericError);

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Error: ${genericError.message}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle non-Error objects', async () => {
      const unknownError = { custom: 'error object' };
      qualityGateApiMock.calculateQualityGateRaw.mockRejectedValue(unknownError);

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Error: ${JSON.stringify(unknownError)}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });
  });
});
