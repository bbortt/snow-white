/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterAll, beforeEach, describe, expect, it, mock, spyOn, jest } from 'bun:test';
import { AxiosError, AxiosResponse } from 'axios';
import { calculate } from './calculate';
import type { QualityGateApi } from '../clients/quality-gate-api';
import { CalculateOptions } from './calculate.options';

const mockConsoleLog = spyOn(console, 'log');
const mockConsoleError = spyOn(console, 'error');

const getQualityGateApi = (qualityGateApiMock: unknown): QualityGateApi => {
  return qualityGateApiMock as QualityGateApi;
};

describe('calculate', () => {
  let qualityGateApiMock = {
    calculateQualityGate: mock(),
  };

  const defaultOptions = {
    qualityGate: 'test-gate',
    serviceName: 'test-service',
    apiName: 'test-api',
    apiVersion: '1.0.0',
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
        status: 202,
        statusText: 'Accepted',
        headers: {
          location: 'http://localhost:8080/api/rest/v1/quality-gates/reports/123-456-789',
        },
        config: {} as any,
        data: {
          id: '123-456-789',
          status: 'INITIATED',
          qualityGateConfigName: 'test-gate',
          serviceName: 'test-service',
          apiName: 'test-api',
          apiVersion: '1.0.0',
          createdAt: '2025-06-21T10:30:00Z',
        },
      };

      qualityGateApiMock.calculateQualityGate.mockResolvedValue(mockResponse);

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

      expect(qualityGateApiMock.calculateQualityGate).toHaveBeenCalledWith(defaultOptions.qualityGate, {
        serviceName: defaultOptions.serviceName,
        apiName: defaultOptions.apiName,
        apiVersion: defaultOptions.apiVersion,
      });

      expect(mockConsoleLog).toHaveBeenNthCalledWith(1, expect.stringContaining('🚀 Starting quality gate calculation...'));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(2, expect.stringContaining(`Gate: ${defaultOptions.qualityGate}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(3, expect.stringContaining(`Service: ${defaultOptions.serviceName}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(4, expect.stringContaining(`API: ${defaultOptions.apiName}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(5, expect.stringContaining(`Version: ${defaultOptions.apiVersion}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(6, expect.stringContaining(`Base URL: ${defaultOptions.url}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(8, expect.stringContaining('✅ Quality gate calculation initiated successfully!'));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(10, expect.stringContaining(`Location: ${mockResponse.headers.location}`));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(
        12,
        expect.stringContaining('💡 Use the returned URL to check the calculation report.'),
      );
    });

    it('should handle response without location header', async () => {
      const mockResponse: AxiosResponse = {
        status: 202,
        statusText: 'Accepted',
        headers: {}, // No location header
        config: {} as any,
        data: {
          id: '123-456-789',
          status: 'INITIATED',
        },
      };

      qualityGateApiMock.calculateQualityGate.mockResolvedValue(mockResponse);

      await calculate(getQualityGateApi(qualityGateApiMock), defaultOptions);

      expect(mockConsoleLog).not.toHaveBeenCalledWith(expect.stringContaining('Location:'));
      expect(mockConsoleLog).toHaveBeenNthCalledWith(8, expect.stringContaining('✅ Quality gate calculation initiated successfully!'));
    });

    it('should work with default URL when not provided', async () => {
      const optionsWithoutUrl: Omit<CalculateOptions, 'url'> = {
        qualityGate: 'test-gate',
        serviceName: 'test-service',
        apiName: 'test-api',
        apiVersion: '1.0.0',
      };

      const mockResponse: AxiosResponse = {
        status: 202,
        statusText: 'Accepted',
        headers: {},
        config: {} as any,
        data: {},
      };

      qualityGateApiMock.calculateQualityGate.mockResolvedValue(mockResponse);

      await calculate(getQualityGateApi(qualityGateApiMock), optionsWithoutUrl);

      expect(mockConsoleLog).toHaveBeenNthCalledWith(6, expect.stringContaining('Base URL: undefined'));
    });
  });

  describe('error handling', () => {
    it('should handle AxiosError with response', () => {
      const mockErrorResponse = {
        status: 404,
        statusText: 'Not Found',
        data: {
          message: 'Quality gate configuration not found',
        },
      };

      const axiosError = new AxiosError('Request failed with status code 404', 'ERR_BAD_REQUEST', {} as any, {}, mockErrorResponse as any);

      qualityGateApiMock.calculateQualityGate.mockRejectedValue(axiosError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).rejects.toThrow('Failed to calculate quality-gate!');

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌ Failed to trigger quality gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Status: ${mockErrorResponse.status}`));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining(`Details: ${mockErrorResponse.data.message}`));
    });

    it('should handle AxiosError with response but no data', () => {
      const mockErrorResponse = {
        status: 501,
        statusText: 'Internal Server Error',
        data: null,
      };

      const axiosError = new AxiosError(
        'Request failed with status code 500',
        'ERR_INTERNAL_SERVER_ERROR',
        {} as any,
        {},
        mockErrorResponse as any,
      );

      qualityGateApiMock.calculateQualityGate.mockRejectedValue(axiosError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).rejects.toThrow('Failed to calculate quality-gate!');

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌ Failed to trigger quality gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Status: ${mockErrorResponse.status}`));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining(`Error: ${mockErrorResponse.statusText}`));
    });

    it('should handle AxiosError with no response (network error)', () => {
      const axiosError = new AxiosError('Network Error', 'ERR_NETWORK', {} as any, { timeout: 5000 });

      qualityGateApiMock.calculateQualityGate.mockRejectedValue(axiosError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).rejects.toThrow('Failed to calculate quality-gate!');

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌ Failed to trigger quality gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('No response received from server'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('Check if the service is running and accessible'));
    });

    it('should handle generic Error', () => {
      const genericError = new Error('Something went wrong');
      qualityGateApiMock.calculateQualityGate.mockRejectedValue(genericError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).rejects.toThrow('Failed to calculate quality-gate!');

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌ Failed to trigger quality gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Error: ${genericError.message}`));
    });

    it('should handle non-Error objects', () => {
      const unknownError = { custom: 'error object' };
      qualityGateApiMock.calculateQualityGate.mockRejectedValue(unknownError);

      expect(calculate(getQualityGateApi(qualityGateApiMock), defaultOptions)).rejects.toThrow('Failed to calculate quality-gate!');

      expect(mockConsoleError).toHaveBeenNthCalledWith(1, expect.stringContaining('❌ Failed to trigger quality gate calculation!'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining(`Error: ${JSON.stringify(unknownError)}`));
    });
  });
});
