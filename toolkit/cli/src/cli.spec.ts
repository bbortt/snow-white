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

import type { CalculateQualityGateRequest } from './clients/quality-gate-api';
import { QUALITY_GATE_CALCULATION_FAILED } from './common/exit-codes';

import { fileSync, setGracefulCleanup } from 'tmp';
import { writeFileSync } from 'node:fs';

const WIREMOCK_PORT = process.env.WIREMOCK_PORT || 8080;

const configureSuccessfulWireMockRequest = (): {
  serviceName: string;
  apiName: string;
  apiVersion: string;
  wireMockRequest: IWireMockRequest;
} => {
  const serviceName = 'user-service';
  const apiName = 'user-api';
  const apiVersion = '1.0.0';

  const wireMockRequest: IWireMockRequest & { body: CalculateQualityGateRequest } = {
    method: 'POST',
    endpoint: '/api/rest/v1/quality-gates/test-quality-gate/calculate',
    headers: {
      'Content-Type': 'application/json',
    },
    body: {
      includeApis: [{ serviceName, apiName, apiVersion }],
    },
  };

  return { serviceName, apiName, apiVersion, wireMockRequest };
};

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

    child.on('error', _ => {
      resolve({
        exitCode: 1,
        stdout,
        stderr,
      });
    });

    // Set timeout for the command execution
    setTimeout(() => {
      child.kill();
      reject(new Error('CLI command timed out'));
    }, 10000); // 10 second timeout
  });
};

describe('CLI', () => {
  const qualityGateConfigName = 'test-quality-gate';

  let wiremock: WireMock;
  let WIREMOCK_URL: string;

  const invokeCalculateCommandWithExplicitConfiguration = (serviceName: string, apiName: string, apiVersion: string) =>
    executeCLICommand([
      'calculate',
      '--qualityGate',
      qualityGateConfigName,
      '--serviceName',
      serviceName,
      '--apiName',
      apiName,
      '--apiVersion',
      apiVersion,
      '--url',
      WIREMOCK_URL,
    ]);

  const assertThatBasicInformationIsBeingPrinted = (
    cliResult: {
      exitCode: number;
      stdout: string;
      stderr: string;
    },
    expectedExitCode: number,
  ) => {
    expect(cliResult.exitCode, cliResult.stderr).toBe(expectedExitCode);
    expect(cliResult.stdout).toContain('🚀 Starting Quality-Gate calculation for 1 API(s)...');
    expect(cliResult.stdout).toContain(`Base URL: ${WIREMOCK_URL}`);
  };

  beforeAll(() => {
    setGracefulCleanup();

    WIREMOCK_URL = `http://localhost:${WIREMOCK_PORT}`;
    wiremock = new WireMock(WIREMOCK_URL);

    console.log(`WireMock started on port ${WIREMOCK_PORT}`);
  });

  afterEach(() => {
    return wiremock.clearAllExceptDefault();
  });

  [
    {
      title: 'with explicit configuration',
      cliInvocationCommand: (serviceName: string, apiName: string, apiVersion: string) =>
        invokeCalculateCommandWithExplicitConfiguration(serviceName, apiName, apiVersion),
    },
    {
      title: 'with configuration from file',
      cliInvocationCommand: async (serviceName: string, apiName: string, apiVersion: string) => {
        const tmpobj = fileSync();
        writeFileSync(
          tmpobj.name,
          JSON.stringify({
            qualityGate: qualityGateConfigName,
            apiInformation: [{ serviceName, apiName, apiVersion }],
            url: WIREMOCK_URL,
          }),
        );

        return executeCLICommand(['calculate', '--configFile', tmpobj.name, '--url', WIREMOCK_URL]);
      },
    },
  ].forEach(testConfiguration => {
    describe('command: calculate', () => {
      describe(testConfiguration.title, () => {
        it('should successfully trigger quality gate calculation', async () => {
          const { serviceName, apiName, apiVersion, wireMockRequest } = configureSuccessfulWireMockRequest();

          const mockResponse: IWireMockResponse = {
            status: 202,
            headers: {
              'Content-Type': 'application/json',
              Location: `${WIREMOCK_URL}/api/rest/v1/quality-gates/reports/550e8400-e29b-41d4-a716-446655440000`,
            },
            body: {
              id: '550e8400-e29b-41d4-a716-446655440000',
              status: 'INITIATED',
              qualityGateConfigName,
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

          const cliResult = await testConfiguration.cliInvocationCommand(serviceName, apiName, apiVersion);

          console.debug(`Output: ${cliResult.stdout}`);

          assertThatBasicInformationIsBeingPrinted(cliResult, 0);

          expect(cliResult.stdout).toContain('✅ Quality-Gate calculation initiated successfully!');
          expect(cliResult.stdout).toContain(
            `Location: ${WIREMOCK_URL}/api/rest/v1/quality-gates/reports/550e8400-e29b-41d4-a716-446655440000`,
          );
          expect(cliResult.stdout).toContain('💡 Use the returned URL to check the calculation report.');

          const requests = await wiremock.getRequestsForAPI('POST', `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`);
          expect(requests.length).toBe(1);

          const unmatchedRequests = await wiremock.getUnmatchedRequests();
          expect(unmatchedRequests.length).toBe(0);
        });
      });
    });

    describe('error handling', () => {
      it('should exit when server responds with 404 bad request response', async () => {
        const { serviceName, apiName, apiVersion, wireMockRequest } = configureSuccessfulWireMockRequest();

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

        const cliResult = await invokeCalculateCommandWithExplicitConfiguration(serviceName, apiName, apiVersion);

        console.debug(`Output: ${cliResult.stdout}`);

        assertThatBasicInformationIsBeingPrinted(cliResult, QUALITY_GATE_CALCULATION_FAILED);

        expect(cliResult.stderr).toContain('❌ Failed to trigger Quality-Gate calculation!');
        expect(cliResult.stderr).toContain('Status: 400');
        expect(cliResult.stderr).toContain(`Details: ${message}`);

        const requests = await wiremock.getRequestsForAPI('POST', `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`);
        expect(requests.length).toBe(1);

        const unmatchedRequests = await wiremock.getUnmatchedRequests();
        expect(unmatchedRequests.length).toBe(0);
      });

      it('should exit when server responds with 404 not found response', async () => {
        const { serviceName, apiName, apiVersion, wireMockRequest } = configureSuccessfulWireMockRequest();

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

        const cliResult = await invokeCalculateCommandWithExplicitConfiguration(serviceName, apiName, apiVersion);

        console.debug(`Output: ${cliResult.stdout}`);

        assertThatBasicInformationIsBeingPrinted(cliResult, QUALITY_GATE_CALCULATION_FAILED);

        expect(cliResult.stderr).toContain('❌ Failed to trigger Quality-Gate calculation!');
        expect(cliResult.stderr).toContain('Status: 404');
        expect(cliResult.stderr).toContain(`Details: ${message}`);

        const requests = await wiremock.getRequestsForAPI('POST', `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`);
        expect(requests.length).toBe(1);

        const unmatchedRequests = await wiremock.getUnmatchedRequests();
        expect(unmatchedRequests.length).toBe(0);
      });

      it('should exit when server responds with 500 internal server error response', async () => {
        const { serviceName, apiName, apiVersion, wireMockRequest } = configureSuccessfulWireMockRequest();

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

        const cliResult = await invokeCalculateCommandWithExplicitConfiguration(serviceName, apiName, apiVersion);

        console.debug(`Output: ${cliResult.stdout}`);

        assertThatBasicInformationIsBeingPrinted(cliResult, QUALITY_GATE_CALCULATION_FAILED);

        expect(cliResult.stderr).toContain('❌ Failed to trigger Quality-Gate calculation!');
        expect(cliResult.stderr).toContain('Status: 500');
        expect(cliResult.stderr).toContain('Error: Server Error');

        const requests = await wiremock.getRequestsForAPI('POST', `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`);
        expect(requests.length).toBe(1);

        const unmatchedRequests = await wiremock.getUnmatchedRequests();
        expect(unmatchedRequests.length).toBe(0);
      });
    });
  });
});
