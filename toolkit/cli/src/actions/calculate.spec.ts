/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { AxiosResponse } from 'axios';

import { AxiosError } from 'axios';
import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';
import { exit } from 'node:process';

import type { QualityGateApi } from '../clients/quality-gate-api';
import type { SanitizedOptions } from '../config/sanitized-options';

import { QUALITY_GATE_CALCULATION_FAILED } from '../common/exit-codes';
import { calculate } from './calculate';

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:process', () => ({
  exit: mock(),
}));

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('../common/config', () => ({
  resolveSnowWhiteConfig: mock(),
}));

const mockConsoleLog = spyOn(console, 'log');
const mockConsoleError = spyOn(console, 'error');

const getQualityGateApi = (qualityGateApiMock: unknown): QualityGateApi => {
  return qualityGateApiMock as QualityGateApi;
};

describe('calculate action', () => {
  const qualityGateApiMock = {
    calculateQualityGate: mock(),
  };

  const defaultOptions: SanitizedOptions = {
    apiInformation: [{ apiName: 'test-api', apiVersion: '1.0.0', serviceName: 'test-service' }],
    qualityGate: 'test-gate',
    url: 'http://localhost:8080',
  };

  beforeEach(() => {
    mockConsoleLog.mockReset();
    mockConsoleError.mockReset();

    qualityGateApiMock.calculateQualityGate.mockReset();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  describe('successful calculation', () => {
    it('should successfully initiate quality gate calculation', async () => {
      const mockResponse: AxiosResponse = {
        config: {} as any,
        data: {
          apiName: 'test-api',
          apiVersion: '1.0.0',
          createdAt: '2025-06-21T10:30:00Z',
          id: '123-456-789',
          qualityGateConfigName: 'test-gate',
          serviceName: 'test-service',
          status: 'INITIATED',
        },
        headers: {
          location: 'http://localhost:8080/api/rest/v1/quality-gates/reports/123-456-789',
        },
        status: 202,
        statusText: 'Accepted',
      };

      qualityGateApiMock.calculateQualityGate.mockResolvedValue(mockResponse);

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

      expect(qualityGateApiMock.calculateQualityGate).toHaveBeenCalledWith(defaultOptions.qualityGate, {
        attributeFilters: undefined,
        includeApis: [...defaultOptions.apiInformation],
        lookbackWindow: undefined,
      });

      expect(mockConsoleLog).toHaveBeenNthCalledWith(1, expect.stringContaining('🚀  Starting Quality-Gate calculation for 1 API(s)...'));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(2, expect.stringContaining(`Base URL: ${defaultOptions.url}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(4, expect.stringContaining('✅ Quality-Gate calculation initiated successfully!'));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(6, expect.stringContaining(`Location: ${mockResponse.headers.location}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(
        8,
        expect.stringContaining('💡  Use the returned URL to check the calculation report.'),
      );
    });

    it('should include lookbackWindow in request when provided', async () => {
      const mockResponse: AxiosResponse = {
        config: {} as any,
        data: { id: '123-456-789', status: 'INITIATED' },
        headers: {},
        status: 202,
        statusText: 'Accepted',
      };

      qualityGateApiMock.calculateQualityGate.mockResolvedValue(mockResponse);

      const optionsWithLookback: SanitizedOptions = {
        ...defaultOptions,
        lookbackWindow: '24h',
      };

      await calculate(getQualityGateApi(qualityGateApiMock), optionsWithLookback);

      expect(qualityGateApiMock.calculateQualityGate).toHaveBeenCalledWith(defaultOptions.qualityGate, {
        attributeFilters: undefined,
        includeApis: [...defaultOptions.apiInformation],
        lookbackWindow: '24h',
      });

      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('Lookback window: 24h'));
    });

    it('should include attributeFilters in request when provided', async () => {
      const mockResponse: AxiosResponse = {
        config: {} as any,
        data: { id: '123-456-789', status: 'INITIATED' },
        headers: {},
        status: 202,
        statusText: 'Accepted',
      };

      qualityGateApiMock.calculateQualityGate.mockResolvedValue(mockResponse);

      const optionsWithFilters: SanitizedOptions = {
        ...defaultOptions,
        attributeFilters: { environment: 'production', region: 'us-west-1' },
      };

      await calculate(getQualityGateApi(qualityGateApiMock), optionsWithFilters);

      expect(qualityGateApiMock.calculateQualityGate).toHaveBeenCalledWith(defaultOptions.qualityGate, {
        attributeFilters: { environment: 'production', region: 'us-west-1' },
        includeApis: [...defaultOptions.apiInformation],
        lookbackWindow: undefined,
      });

      expect(mockConsoleLog).toHaveBeenCalledWith(
        expect.stringContaining('Attribute filters: {"environment":"production","region":"us-west-1"}'),
      );
    });

    it('should include both lookbackWindow and attributeFilters when provided', async () => {
      const mockResponse: AxiosResponse = {
        config: {} as any,
        data: { id: '123-456-789', status: 'INITIATED' },
        headers: {},
        status: 202,
        statusText: 'Accepted',
      };

      qualityGateApiMock.calculateQualityGate.mockResolvedValue(mockResponse);

      const fullOptions: SanitizedOptions = {
        ...defaultOptions,
        attributeFilters: { environment: 'staging' },
        lookbackWindow: '7d',
      };

      await calculate(getQualityGateApi(qualityGateApiMock), fullOptions);

      expect(qualityGateApiMock.calculateQualityGate).toHaveBeenCalledWith(defaultOptions.qualityGate, {
        attributeFilters: { environment: 'staging' },
        includeApis: [...defaultOptions.apiInformation],
        lookbackWindow: '7d',
      });
    });

    it('should handle response without location header', async () => {
      const mockResponse: AxiosResponse = {
        config: {} as any,
        data: {
          id: '123-456-789',
          status: 'INITIATED',
        },
        headers: {}, // No location header
        status: 202,
        statusText: 'Accepted',
      };

      qualityGateApiMock.calculateQualityGate.mockResolvedValue(mockResponse);

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

      expect(mockConsoleLog).toHaveBeenNthCalledWith(4, expect.stringContaining('✅ Quality-Gate calculation initiated successfully!'));
      expect(mockConsoleLog).not.toHaveBeenCalledWith(expect.stringContaining('Location:'));
      expect(mockConsoleLog).not.toHaveBeenCalledWith(expect.stringContaining('💡  Use the returned URL to check the calculation report.'));
    });
  });

  describe('error handling', () => {
    it('should handle AxiosError with response', () => {
      const mockErrorResponse = {
        data: {
          message: 'Quality-Gate configuration not found',
        },
        status: 404,
        statusText: 'Not Found',
      };

      const axiosError = new AxiosError('Request failed with status code 404', 'ERR_BAD_REQUEST', {} as any, {}, mockErrorResponse as any);

      qualityGateApiMock.calculateQualityGate.mockRejectedValue(axiosError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).resolves.toBeUndefined();

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Status: ${mockErrorResponse.status}`));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining(`Details: ${mockErrorResponse.data.message}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle AxiosError with response but no data', () => {
      const mockErrorResponse = {
        data: null,
        status: 501,
        statusText: 'Internal Server Error',
      };

      const axiosError = new AxiosError(
        'Request failed with status code 500',
        'ERR_INTERNAL_SERVER_ERROR',
        {} as any,
        {},
        mockErrorResponse as any,
      );

      qualityGateApiMock.calculateQualityGate.mockRejectedValue(axiosError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).resolves.toBeUndefined();

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Status: ${mockErrorResponse.status}`));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining(`Error: ${mockErrorResponse.statusText}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle AxiosError with no response (network error)', () => {
      const axiosError = new AxiosError('Network Error', 'ERR_NETWORK', {} as any, { timeout: 5000 });

      qualityGateApiMock.calculateQualityGate.mockRejectedValue(axiosError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).resolves.toBeUndefined();

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('No response received from server'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('Check if the service is running and accessible'));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle generic Error', () => {
      const genericError = new Error('Something went wrong');
      qualityGateApiMock.calculateQualityGate.mockRejectedValue(genericError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).resolves.toBeUndefined();

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Error: ${genericError.message}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });

    it('should handle non-Error objects', () => {
      const unknownError = { custom: 'error object' };
      qualityGateApiMock.calculateQualityGate.mockRejectedValue(unknownError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).resolves.toBeUndefined();

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌  Failed to trigger Quality-Gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Error: ${JSON.stringify(unknownError)}`));

      expect(exit).toHaveBeenCalledWith(QUALITY_GATE_CALCULATION_FAILED);
    });
  });
});
