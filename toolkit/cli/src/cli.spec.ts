/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeAll, describe, expect, it } from 'bun:test';
import type { ChildProcess } from 'child_process';
import { spawn } from 'child_process';
import type { IWireMockRequest, IWireMockResponse } from 'wiremock-captain';
import { MatchingAttributes, WireMock } from 'wiremock-captain';

const WIREMOCK_PORT = process.env.WIREMOCK_PORT || 8080;

const executeCLICommand = async (
  args: string[],
): Promise<{
  exitCode: number;
  stdout: string;
  stderr: string;
}> => {
  console.info(`CLI command: ${args.join(' ')}`);

  return new Promise((resolve, reject) => {
    const child: ChildProcess = spawn('bun', ['run', 'src/index.ts', ...args], {
      stdio: ['pipe', 'pipe', 'pipe'],
      env: { ...process.env },
    });

    let stdout = '';
    let stderr = '';

    child.stdout?.on('data', data => {
      stdout += data.toString();
    });

    child.stderr?.on('data', data => {
      stderr += data.toString();
    });

    child.on('close', code => {
      resolve({
        exitCode: code || 0,
        stdout,
        stderr,
      });
    });

    child.on('error', error => {
      reject(error);
    });

    // Set timeout for the command execution
    setTimeout(() => {
      child.kill();
      reject(new Error('CLI command timed out'));
    }, 10000); // 10 second timeout
  });
};

describe('CLI', () => {
  let wiremock: WireMock;
  let WIREMOCK_URL: string;

  beforeAll(() => {
    WIREMOCK_URL = `http://localhost:${WIREMOCK_PORT}`;

    wiremock = new WireMock(WIREMOCK_URL);

    console.log(`WireMock started on port ${WIREMOCK_PORT}`);
  });

  afterEach(() => {
    return wiremock.clearAllExceptDefault();
  });

  describe('command: calculate', () => {
    const getWireMockRequest = (): {
      serviceName: string;
      apiName: string;
      apiVersion: string;
      wireMockRequest: IWireMockRequest;
    } => {
      const serviceName = 'user-service';
      const apiName = 'user-api';
      const apiVersion = '1.0.0';

      const wireMockRequest: IWireMockRequest = {
        method: 'POST',
        endpoint: '/api/rest/v1/quality-gates/test-quality-gate/calculate',
        headers: {
          'Content-Type': 'application/json',
        },
        body: {
          serviceName,
          apiName,
          apiVersion,
        },
      };
      return { serviceName, apiName, apiVersion, wireMockRequest };
    };

    const assertThatBasicInformationIsBeingPrinted = (
      cliResult: {
        exitCode: number;
        stdout: string;
        stderr: string;
      },
      expectedExitCode: number,
    ) => {
      expect(cliResult.exitCode, cliResult.stderr).toBe(expectedExitCode);
      expect(cliResult.stdout).toContain('🚀 Starting quality gate calculation...');
      expect(cliResult.stdout).toContain('Gate: test-quality-gate');
      expect(cliResult.stdout).toContain('Service: user-service');
      expect(cliResult.stdout).toContain('API: user-api');
      expect(cliResult.stdout).toContain('Version: 1.0.0');
    };

    async function invokeCalculateCommand(serviceName: string, apiName: string, apiVersion: string) {
      const cliResult = await executeCLICommand([
        'calculate',
        '--qualityGate',
        'test-quality-gate',
        '--serviceName',
        serviceName,
        '--apiName',
        apiName,
        '--apiVersion',
        apiVersion,
        '--url',
        WIREMOCK_URL,
      ]);
      return cliResult;
    }

    it('should successfully trigger quality gate calculation', async () => {
      const { serviceName, apiName, apiVersion, wireMockRequest } = getWireMockRequest();

      const mockResponse: IWireMockResponse = {
        status: 202,
        headers: {
          'Content-Type': 'application/json',
          Location: `${WIREMOCK_URL}/api/rest/v1/quality-gates/reports/550e8400-e29b-41d4-a716-446655440000`,
        },
        body: {
          id: '550e8400-e29b-41d4-a716-446655440000',
          status: 'INITIATED',
          qualityGateConfigName: 'test-quality-gate',
          serviceName,
          apiName,
          apiVersion,
          createdAt: '2025-06-21T10:30:00Z',
        },
      };

      await wiremock.register(wireMockRequest, mockResponse, {
        requestHeaderFeatures: {
          'Content-Type': MatchingAttributes.EqualTo,
        },
      });

      const cliResult = await invokeCalculateCommand(serviceName, apiName, apiVersion);

      console.debug(`Output: ${cliResult.stdout}`);

      assertThatBasicInformationIsBeingPrinted(cliResult, 0);

      expect(cliResult.stdout).toContain('✅ Quality gate calculation initiated successfully!');
      expect(cliResult.stdout).toContain(
        'Location: http://localhost:8080/api/rest/v1/quality-gates/reports/550e8400-e29b-41d4-a716-446655440000',
      );
      expect(cliResult.stdout).toContain('💡 Use the returned URL to check the calculation report.');

      const requests = await wiremock.getRequestsForAPI('POST', '/api/rest/v1/quality-gates/test-quality-gate/calculate');
      expect(requests.length).toBe(1);

      const unmatchedRequests = await wiremock.getUnmatchedRequests();
      expect(unmatchedRequests.length).toBe(0);
    });

    it('should exit when server responds with 404 bad request response', async () => {
      const { serviceName, apiName, apiVersion, wireMockRequest } = getWireMockRequest();

      const message = 'This is a forced error message!';
      const mockResponse: IWireMockResponse = {
        status: 400,
        headers: {
          'Content-Type': 'application/json',
        },
        body: {
          status: 'BAD_REQUEST',
          message,
        },
      };

      await wiremock.register(wireMockRequest, mockResponse, {
        requestHeaderFeatures: {
          'Content-Type': MatchingAttributes.EqualTo,
        },
      });

      const cliResult = await invokeCalculateCommand(serviceName, apiName, apiVersion);

      console.debug(`Output: ${cliResult.stdout}`);

      assertThatBasicInformationIsBeingPrinted(cliResult, 1);

      expect(cliResult.stderr).toContain('❌ Failed to trigger quality gate calculation!');
      expect(cliResult.stderr).toContain('Status: 400');
      expect(cliResult.stderr).toContain(`Details: ${message}`);

      const requests = await wiremock.getRequestsForAPI('POST', '/api/rest/v1/quality-gates/test-quality-gate/calculate');
      expect(requests.length).toBe(1);

      const unmatchedRequests = await wiremock.getUnmatchedRequests();
      expect(unmatchedRequests.length).toBe(0);
    });

    it('should exit when server responds with 404 not found response', async () => {
      const { serviceName, apiName, apiVersion, wireMockRequest } = getWireMockRequest();

      const message = 'Quality-Gate name not found';
      const mockResponse: IWireMockResponse = {
        status: 404,
        headers: {
          'Content-Type': 'application/json',
        },
        body: {
          code: 'NOT_FOUND',
          message,
        },
      };

      await wiremock.register(wireMockRequest, mockResponse, {
        requestHeaderFeatures: {
          'Content-Type': MatchingAttributes.EqualTo,
        },
      });

      const cliResult = await invokeCalculateCommand(serviceName, apiName, apiVersion);

      console.debug(`Output: ${cliResult.stdout}`);

      assertThatBasicInformationIsBeingPrinted(cliResult, 1);

      expect(cliResult.stderr).toContain('❌ Failed to trigger quality gate calculation!');
      expect(cliResult.stderr).toContain('Status: 404');
      expect(cliResult.stderr).toContain(`Details: ${message}`);

      const requests = await wiremock.getRequestsForAPI('POST', '/api/rest/v1/quality-gates/test-quality-gate/calculate');
      expect(requests.length).toBe(1);

      const unmatchedRequests = await wiremock.getUnmatchedRequests();
      expect(unmatchedRequests.length).toBe(0);
    });

    it('should exit when server responds with 500 internal server error response', async () => {
      const { serviceName, apiName, apiVersion, wireMockRequest } = getWireMockRequest();

      const mockResponse: IWireMockResponse = {
        status: 500,
        headers: {
          'Content-Type': 'application/json',
        },
        // Message body is not guaranteed with HTTP 500 errors
      };

      await wiremock.register(wireMockRequest, mockResponse, {
        requestHeaderFeatures: {
          'Content-Type': MatchingAttributes.EqualTo,
        },
      });

      const cliResult = await invokeCalculateCommand(serviceName, apiName, apiVersion);

      console.debug(`Output: ${cliResult.stdout}`);

      assertThatBasicInformationIsBeingPrinted(cliResult, 1);

      expect(cliResult.stderr).toContain('❌ Failed to trigger quality gate calculation!');
      expect(cliResult.stderr).toContain('Status: 500');
      expect(cliResult.stderr).toContain('Error: Server Error');

      const requests = await wiremock.getRequestsForAPI('POST', '/api/rest/v1/quality-gates/test-quality-gate/calculate');
      expect(requests.length).toBe(1);

      const unmatchedRequests = await wiremock.getUnmatchedRequests();
      expect(unmatchedRequests.length).toBe(0);
    });
  });
});
