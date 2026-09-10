/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it } from 'bun:test';

import type {
  ListQualityGateReports200ResponseInner,
  ListQualityGateReports200ResponseInnerInterfacesInner,
  ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner,
} from '../../clients/report-api';

import {
  ListQualityGateReports200ResponseInnerInterfacesInnerApiTypeEnum,
  ListQualityGateReports200ResponseInnerInterfacesInnerStatusEnum,
  ListQualityGateReports200ResponseInnerStatusEnum,
} from '../../clients/report-api';
import { AGENTIC_SCHEMA_VERSION, AgenticQualityGateResponseTransformer } from './agent-quality-gate-response-transformer';

const API_BASE_URL = 'http://localhost:8080';

const testResult = (
  overrides: Partial<ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner> = {},
): ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner => ({
  coverage: 1,
  id: 'criterion-1',
  ...overrides,
});

const apiInterface = (
  overrides: Partial<ListQualityGateReports200ResponseInnerInterfacesInner> = {},
): ListQualityGateReports200ResponseInnerInterfacesInner => ({
  apiName: 'test-api',
  apiVersion: '1.0.0',
  serviceName: 'test-service',
  status: ListQualityGateReports200ResponseInnerInterfacesInnerStatusEnum.Passed,
  ...overrides,
});

const response = (overrides: Partial<ListQualityGateReports200ResponseInner> = {}): ListQualityGateReports200ResponseInner => ({
  calculationId: 'calc-123',
  calculationRequest: {} as ListQualityGateReports200ResponseInner['calculationRequest'],
  initiatedAt: new Date('2026-01-01T00:00:00.000Z'),
  qualityGateConfigName: 'test-gate',
  status: ListQualityGateReports200ResponseInnerStatusEnum.Passed,
  ...overrides,
});

describe('AgenticQualityGateResponseTransformer', () => {
  const transformer = new AgenticQualityGateResponseTransformer(API_BASE_URL);

  it('should transform top-level response fields', () => {
    const result = transformer.transform(response());

    expect(result.schemaVersion).toBe(AGENTIC_SCHEMA_VERSION);
    expect(result.status).toBe(ListQualityGateReports200ResponseInnerStatusEnum.Passed);
    expect(result.calculationId).toBe('calc-123');
    expect(result.qualityGateConfigName).toBe('test-gate');
    expect(result.initiatedAt).toBe('2026-01-01T00:00:00.000Z');
  });

  it('should build the API location from the base URL and calculation id', () => {
    const result = transformer.transform(response({ calculationId: 'calc-456' }));

    expect(result.apiLocation).toBe(`${API_BASE_URL}/api/rest/v1/reports/calc-456`);
  });

  it('should default to an empty interfaces array when none are provided', () => {
    const result = transformer.transform(response({ interfaces: undefined }));

    expect(result.interfaces).toEqual([]);
    expect(result.summary).toEqual({ apiCount: 0, failedApiCount: 0, qualityGateFailureCount: 0 });
  });

  it('should transform interfaces and default missing apiVersion, apiType and status', () => {
    const result = transformer.transform(
      response({
        interfaces: [apiInterface({ apiType: undefined, apiVersion: undefined, status: undefined })],
      }),
    );

    expect(result.interfaces).toEqual([
      {
        apiName: 'test-api',
        apiType: ListQualityGateReports200ResponseInnerInterfacesInnerApiTypeEnum.Unspecified,
        apiVersion: '*',
        qualityGateFailures: [],
        serviceName: 'test-service',
        status: ListQualityGateReports200ResponseInnerInterfacesInnerStatusEnum.InProgress,
        testResults: [],
      },
    ]);
  });

  it('should only include test results marked as included in the quality gate as failures', () => {
    const included = testResult({ id: 'included', isIncludedInQualityGate: true });
    const excluded = testResult({ id: 'excluded', isIncludedInQualityGate: false });
    const unspecified = testResult({ id: 'unspecified' });

    const result = transformer.transform(
      response({
        interfaces: [apiInterface({ testResults: [included, excluded, unspecified] })],
      }),
    );

    expect(result.interfaces[0].qualityGateFailures).toEqual([included]);
    expect(result.interfaces[0].testResults).toEqual([included, excluded, unspecified]);
  });

  it('should count APIs with a FAILED status in the summary', () => {
    const result = transformer.transform(
      response({
        interfaces: [
          apiInterface({ apiName: 'passing-api', status: ListQualityGateReports200ResponseInnerInterfacesInnerStatusEnum.Passed }),
          apiInterface({ apiName: 'failing-api', status: ListQualityGateReports200ResponseInnerInterfacesInnerStatusEnum.Failed }),
        ],
      }),
    );

    expect(result.summary).toEqual({ apiCount: 2, failedApiCount: 1, qualityGateFailureCount: 0 });
  });

  it('should sum quality gate failures across all interfaces', () => {
    const result = transformer.transform(
      response({
        interfaces: [
          apiInterface({
            apiName: 'api-one',
            testResults: [
              testResult({ id: 'one-a', isIncludedInQualityGate: true }),
              testResult({ id: 'one-b', isIncludedInQualityGate: false }),
            ],
          }),
          apiInterface({
            apiName: 'api-two',
            testResults: [
              testResult({ id: 'two-a', isIncludedInQualityGate: true }),
              testResult({ id: 'two-b', isIncludedInQualityGate: true }),
            ],
          }),
        ],
      }),
    );

    expect(result.summary.qualityGateFailureCount).toBe(3);
  });
});
