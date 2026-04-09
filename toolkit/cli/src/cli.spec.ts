/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ChildProcess } from 'node:child_process';
import type { IWireMockRequest, IWireMockResponse } from 'wiremock-captain';

import { afterEach, beforeAll, describe, expect, it } from 'bun:test';
import { spawn } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { unlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { MatchingAttributes, WireMock } from 'wiremock-captain';

import type { CalculateQualityGateRequest } from './clients/quality-gate-api';

import { QUALITY_GATE_CALCULATION_FAILED } from './common/exit-codes';

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
    body: {
      includeApis: [{ apiName, apiVersion, serviceName }],
    },
    endpoint: '/api/rest/v1/quality-gates/test-quality-gate/calculate',
    headers: {
      'Content-Type': 'application/json',
    },
    method: 'POST',
  };

  return { apiName, apiVersion, serviceName, wireMockRequest };
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
      env: { ...process.env },
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    let stdout = '';
    let stderr = '';

    child.stdout?.on('data', data => {
      // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
      stdout += data.toString();
    });

    child.stderr?.on('data', data => {
      // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
      stderr += data.toString();
    });

    child.on('close', code => {
      resolve({
        exitCode: code || 0,
        stderr,
        stdout,
      });
    });

    child.on('error', e => {
      console.log('error:', e.message);
      resolve({
        exitCode: 1,
        stderr,
        stdout,
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
      '--quality-gate',
      qualityGateConfigName,
      '--service-name',
      serviceName,
      '--api-name',
      apiName,
      '--api-version',
      apiVersion,
      '--url',
      WIREMOCK_URL,
      '--async',
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
    expect(cliResult.stdout).toContain('🚀  Starting Quality-Gate calculation for 1 API(s)...');
    expect(cliResult.stdout).toContain(`Base URL: ${WIREMOCK_URL}`);
  };

  beforeAll(() => {
    WIREMOCK_URL = `http://localhost:${WIREMOCK_PORT}`;
    wiremock = new WireMock(WIREMOCK_URL);

    console.log(`Connecting to WireMock on port ${WIREMOCK_PORT}`);
  });

  afterEach(() => {
    return wiremock.clearAllExceptDefault();
  });

  [
    {
      cliInvocationCommand: (serviceName: string, apiName: string, apiVersion: string) =>
        invokeCalculateCommandWithExplicitConfiguration(serviceName, apiName, apiVersion),
      title: 'with explicit configuration',
    },
    {
      cliInvocationCommand: async (serviceName: string, apiName: string, apiVersion: string) => {
        const tmpPath = join(tmpdir(), `temp-${randomBytes(16).toString('hex')}.json`);
        writeFileSync(
          tmpPath,
          JSON.stringify({
            apiInformation: [{ apiName, apiVersion, serviceName }],
            qualityGate: qualityGateConfigName,
            url: WIREMOCK_URL,
          }),
        );

        return executeCLICommand(['calculate', '--config-file', tmpPath, '--url', WIREMOCK_URL, '--async']);
      },
      title: 'with configuration from file',
    },
  ].forEach(testConfiguration => {
    describe('command: calculate (fire-and-forget)', () => {
      describe(testConfiguration.title, () => {
        it('should successfully trigger quality gate calculation', async () => {
          const { apiName, apiVersion, serviceName, wireMockRequest } = configureSuccessfulWireMockRequest();

          const mockResponse: IWireMockResponse = {
            body: {
              apiName,
              apiVersion,
              createdAt: '2025-06-21T10:30:00Z',
              id: '550e8400-e29b-41d4-a716-446655440000',
              qualityGateConfigName,
              serviceName,
              status: 'INITIATED',
            },
            headers: {
              'Content-Type': 'application/json',
              Location: `${WIREMOCK_URL}/api/rest/v1/quality-gates/reports/550e8400-e29b-41d4-a716-446655440000`,
            },
            status: 202,
          };

          await wiremock.register(wireMockRequest, mockResponse, {
            requestHeaderFeatures: {
              'Content-Type': MatchingAttributes.EqualTo,
            },
          });

          const cliResult = await testConfiguration.cliInvocationCommand(serviceName, apiName, apiVersion);

          assertThatBasicInformationIsBeingPrinted(cliResult, 0);

          expect(cliResult.stdout).toContain('✅ Quality-Gate calculation initiated successfully!');
          expect(cliResult.stdout).toContain(
            `Location: ${WIREMOCK_URL}/api/rest/v1/quality-gates/reports/550e8400-e29b-41d4-a716-446655440000`,
          );
          expect(cliResult.stdout).toContain('💡  Use the returned URL to check the calculation report.');

          const requests = await wiremock.getRequestsForAPI('POST', `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`);
          expect(requests.length).toBe(1);

          const unmatchedRequests = await wiremock.getUnmatchedRequests();
          expect(unmatchedRequests.length).toBe(0);
        });
      });
    });

    describe('error handling', () => {
      it('should exit when server responds with 400 bad request response', async () => {
        const { apiName, apiVersion, serviceName, wireMockRequest } = configureSuccessfulWireMockRequest();

        const message = 'This is a forced error message!';
        const mockResponse: IWireMockResponse = {
          body: {
            message,
            status: 'BAD_REQUEST',
          },
          headers: {
            'Content-Type': 'application/json',
          },
          status: 400,
        };

        await wiremock.register(wireMockRequest, mockResponse, {
          requestHeaderFeatures: {
            'Content-Type': MatchingAttributes.EqualTo,
          },
        });

        const cliResult = await invokeCalculateCommandWithExplicitConfiguration(serviceName, apiName, apiVersion);

        assertThatBasicInformationIsBeingPrinted(cliResult, QUALITY_GATE_CALCULATION_FAILED);

        expect(cliResult.stderr).toContain('❌  Failed to trigger Quality-Gate calculation!');
        expect(cliResult.stderr).toContain('Status: 400');
        expect(cliResult.stderr).toContain(`Details: ${message}`);

        const requests = await wiremock.getRequestsForAPI('POST', `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`);
        expect(requests.length).toBe(1);

        const unmatchedRequests = await wiremock.getUnmatchedRequests();
        expect(unmatchedRequests.length).toBe(0);
      });

      it('should exit when server responds with 404 not found response', async () => {
        const { apiName, apiVersion, serviceName, wireMockRequest } = configureSuccessfulWireMockRequest();

        const message = 'Quality-Gate name not found';
        const mockResponse: IWireMockResponse = {
          body: {
            code: 'NOT_FOUND',
            message,
          },
          headers: {
            'Content-Type': 'application/json',
          },
          status: 404,
        };

        await wiremock.register(wireMockRequest, mockResponse, {
          requestHeaderFeatures: {
            'Content-Type': MatchingAttributes.EqualTo,
          },
        });

        const cliResult = await invokeCalculateCommandWithExplicitConfiguration(serviceName, apiName, apiVersion);

        assertThatBasicInformationIsBeingPrinted(cliResult, QUALITY_GATE_CALCULATION_FAILED);

        expect(cliResult.stderr).toContain('❌  Failed to trigger Quality-Gate calculation!');
        expect(cliResult.stderr).toContain('Status: 404');
        expect(cliResult.stderr).toContain(`Details: ${message}`);

        const requests = await wiremock.getRequestsForAPI('POST', `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`);
        expect(requests.length).toBe(1);

        const unmatchedRequests = await wiremock.getUnmatchedRequests();
        expect(unmatchedRequests.length).toBe(0);
      });

      it('should exit when server responds with 500 internal server error response', async () => {
        const { apiName, apiVersion, serviceName, wireMockRequest } = configureSuccessfulWireMockRequest();

        const mockResponse: IWireMockResponse = {
          headers: {
            'Content-Type': 'application/json',
          },
          status: 500,
          // Message body is not guaranteed with HTTP 500 errors
        };

        await wiremock.register(wireMockRequest, mockResponse, {
          requestHeaderFeatures: {
            'Content-Type': MatchingAttributes.EqualTo,
          },
        });

        const cliResult = await invokeCalculateCommandWithExplicitConfiguration(serviceName, apiName, apiVersion);

        assertThatBasicInformationIsBeingPrinted(cliResult, QUALITY_GATE_CALCULATION_FAILED);

        expect(cliResult.stderr).toContain('❌  Failed to trigger Quality-Gate calculation!');
        expect(cliResult.stderr).toContain('Status: 500');
        expect(cliResult.stderr).toContain('Error: Server Error');

        const requests = await wiremock.getRequestsForAPI('POST', `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`);
        expect(requests.length).toBe(1);

        const unmatchedRequests = await wiremock.getUnmatchedRequests();
        expect(unmatchedRequests.length).toBe(0);
      });
    });
  });

  describe('command: calculate (synchronous polling)', () => {
    const calculationId = '550e8400-e29b-41d4-a716-446655440000';

    const calculateWireMockRequest: IWireMockRequest & { body: CalculateQualityGateRequest } = {
      body: { includeApis: [{ apiName: 'user-api', apiVersion: '1.0.0', serviceName: 'user-service' }] },
      endpoint: `/api/rest/v1/quality-gates/${qualityGateConfigName}/calculate`,
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
    };

    const makeCalculateMockResponse = (): IWireMockResponse => ({
      body: {
        calculationId,
        calculationRequest: {
          includeApis: [{ apiName: 'user-api', apiVersion: '1.0.0', serviceName: 'user-service' }],
        },
        initiatedAt: '2026-01-01T00:00:00Z',
        qualityGateConfigName,
        status: 'IN_PROGRESS',
      },
      headers: {
        'Content-Type': 'application/json',
        Location: `${WIREMOCK_URL}/api/rest/v1/quality-gates/reports/${calculationId}`,
      },
      status: 202,
    });

    it('should poll until PASSED and exit with code 0', async () => {
      await wiremock.register(calculateWireMockRequest, makeCalculateMockResponse(), {
        requestHeaderFeatures: { 'Content-Type': MatchingAttributes.EqualTo },
      });

      await wiremock.register(
        { endpoint: `/api/rest/v1/reports/${calculationId}`, method: 'GET' },
        {
          body: { calculationId, qualityGateConfigName, status: 'PASSED' },
          headers: { 'Content-Type': 'application/json' },
          status: 200,
        },
      );

      const cliResult = await executeCLICommand([
        'calculate',
        '--quality-gate',
        qualityGateConfigName,
        '--service-name',
        'user-service',
        '--api-name',
        'user-api',
        '--api-version',
        '1.0.0',
        '--url',
        WIREMOCK_URL,
      ]);

      console.debug(`Output: ${JSON.stringify(cliResult)}`);

      expect(cliResult.exitCode, cliResult.stderr).toBe(0);
      expect(cliResult.stdout).toContain('⏳  Polling for calculation result...');
      expect(cliResult.stdout).toContain('✅ Quality-Gate passed!');
    });

    it('should poll until FAILED and exit with non-zero code', async () => {
      await wiremock.register(calculateWireMockRequest, makeCalculateMockResponse(), {
        requestHeaderFeatures: { 'Content-Type': MatchingAttributes.EqualTo },
      });

      await wiremock.register(
        { endpoint: `/api/rest/v1/reports/${calculationId}`, method: 'GET' },
        {
          body: { calculationId, qualityGateConfigName, status: 'FAILED' },
          headers: { 'Content-Type': 'application/json' },
          status: 200,
        },
      );

      const cliResult = await executeCLICommand([
        'calculate',
        '--quality-gate',
        qualityGateConfigName,
        '--service-name',
        'user-service',
        '--api-name',
        'user-api',
        '--api-version',
        '1.0.0',
        '--url',
        WIREMOCK_URL,
      ]);

      expect(cliResult.exitCode).toBe(QUALITY_GATE_CALCULATION_FAILED);
      expect(cliResult.stdout).toContain('⏳  Polling for calculation result...');
      expect(cliResult.stderr).toContain('❌  Quality-Gate calculation FAILED!');
    });
  });

  describe('command: upload-prereleases', () => {
    const VALID_OPENAPI_YAML = `
openapi: 3.1.0
info:
  title: Integration Test API
  version: 2.0.0
  x-service-name: integration-test-service
`.trim();

    it('should successfully upload a prerelease spec', async () => {
      const tmpSpecFilename = `temp-spec-${randomBytes(8).toString('hex')}.yaml`;
      writeFileSync(tmpSpecFilename, VALID_OPENAPI_YAML);

      try {
        const mockResponse: IWireMockResponse = { status: 201 };
        await wiremock.register({ endpoint: '/api/rest/v1/apis', method: 'POST' }, mockResponse);

        const cliResult = await executeCLICommand(['upload-prereleases', '--prerelease-specs', tmpSpecFilename, '--url', WIREMOCK_URL]);

        expect(cliResult.exitCode, cliResult.stderr).toBe(0);
        expect(cliResult.stdout).toContain('🚀  Uploading prerelease API specifications matching:');
        expect(cliResult.stdout).toContain(`Base URL: ${WIREMOCK_URL}`);
        expect(cliResult.stdout).toContain('⚠️  Prerelease uploads are temporary');
        expect(cliResult.stdout).toContain('✅');
        expect(cliResult.stdout).toContain('integration-test-service/Integration Test API@2.0.0');
        expect(cliResult.stdout).toContain('Upload complete: 1 succeeded, 0 failed.');

        const requests = await wiremock.getRequestsForAPI('POST', '/api/rest/v1/apis');
        expect(requests.length).toBe(1);
      } finally {
        unlinkSync(tmpSpecFilename);
      }
    });

    it('should exit with non-zero code when the server rejects a spec', async () => {
      const tmpSpecFilename = `temp-spec-${randomBytes(8).toString('hex')}.yaml`;
      writeFileSync(tmpSpecFilename, VALID_OPENAPI_YAML);

      try {
        const mockResponse: IWireMockResponse = {
          body: { message: 'API already exists as a stable release' },
          headers: { 'Content-Type': 'application/json' },
          status: 409,
        };
        await wiremock.register({ endpoint: '/api/rest/v1/apis', method: 'POST' }, mockResponse);

        const cliResult = await executeCLICommand(['upload-prereleases', '--prerelease-specs', tmpSpecFilename, '--url', WIREMOCK_URL]);

        console.debug(`Output: ${cliResult.stderr}`);

        expect(cliResult.exitCode).not.toBe(0);
        expect(cliResult.stderr).toContain('❌');
        expect(cliResult.stderr).toContain('Upload failed.');
        expect(cliResult.stderr).toContain('Status: 409');
        expect(cliResult.stderr).toContain('Details: API already exists as a stable release');
        expect(cliResult.stdout).toContain('Upload complete: 0 succeeded, 1 failed.');
      } finally {
        unlinkSync(tmpSpecFilename);
      }
    });

    it('should read the base URL from a config file', async () => {
      const tmpSpecFilename = `temp-spec-${randomBytes(8).toString('hex')}.yaml`;
      const tmpConfigPath = join(tmpdir(), `temp-${randomBytes(16).toString('hex')}.json`);

      writeFileSync(tmpSpecFilename, VALID_OPENAPI_YAML);
      writeFileSync(tmpConfigPath, JSON.stringify({ url: WIREMOCK_URL }));

      try {
        const mockResponse: IWireMockResponse = { status: 201 };
        await wiremock.register({ endpoint: '/api/rest/v1/apis', method: 'POST' }, mockResponse);

        const cliResult = await executeCLICommand([
          'upload-prereleases',
          '--prerelease-specs',
          tmpSpecFilename,
          '--config-file',
          tmpConfigPath,
        ]);

        expect(cliResult.exitCode, cliResult.stderr).toBe(0);
        expect(cliResult.stdout).toContain(`Base URL: ${WIREMOCK_URL}`);
        expect(cliResult.stdout).toContain('Upload complete: 1 succeeded, 0 failed.');
      } finally {
        unlinkSync(tmpSpecFilename);
      }
    });
  });
});
