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
  // The bar the report was scored against. 80 rather than the domain default of 100, so a case that
  // relies on the threshold cannot pass by coincidence against a hardcoded 100.
  minCoveragePercentage: 80,
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

  it('should report an included test result below the pinned threshold as a failure', () => {
    const belowTheBar = testResult({ coverage: 0.5, id: 'below', isIncludedInQualityGate: true });
    const atTheBar = testResult({ coverage: 0.8, id: 'at-the-bar', isIncludedInQualityGate: true });

    const result = transformer.transform(
      response({
        interfaces: [apiInterface({ testResults: [belowTheBar, atTheBar] })],
      }),
    );

    expect(result.interfaces[0].qualityGateFailures).toEqual([belowTheBar]);
    expect(result.summary.qualityGateFailureCount).toBe(1);
  });

  it('should never report a test result the quality gate excluded, whatever its coverage', () => {
    const excluded = testResult({ coverage: 0, id: 'excluded', isIncludedInQualityGate: false });
    const unspecified = testResult({ coverage: 0, id: 'unspecified' });
    const included = testResult({ coverage: 0, id: 'included', isIncludedInQualityGate: true });

    const result = transformer.transform(
      response({
        interfaces: [apiInterface({ testResults: [excluded, unspecified, included] })],
      }),
    );

    expect(result.interfaces[0].qualityGateFailures).toEqual([included]);
    expect(result.interfaces[0].testResults).toEqual([excluded, unspecified, included]);
  });

  /**
   * The case the previous rule got wrong: the JUnit export passes this criterion and explains the gap
   * in `system-out`, so the agentic failure set must not name it.
   */
  it('should not report a test result that clears the threshold without reaching full coverage', () => {
    const inTheBand = testResult({ coverage: 0.9, id: 'in-the-band', isIncludedInQualityGate: true });

    const result = transformer.transform(
      response({
        interfaces: [apiInterface({ testResults: [inTheBand] })],
      }),
    );

    expect(result.interfaces[0].qualityGateFailures).toEqual([]);
    expect(result.interfaces[0].testResults).toEqual([inTheBand]);
    expect(result.summary.qualityGateFailureCount).toBe(0);
  });

  it('should emit no failures for a report whose every included criterion clears the bar', () => {
    const result = transformer.transform(
      response({
        interfaces: [
          apiInterface({
            testResults: [
              testResult({ coverage: 1, id: 'full', isIncludedInQualityGate: true }),
              testResult({ coverage: 0.8, id: 'exactly-at-the-bar', isIncludedInQualityGate: true }),
            ],
          }),
        ],
      }),
    );

    expect(result.interfaces[0].qualityGateFailures).toEqual([]);
    expect(result.summary.qualityGateFailureCount).toBe(0);
  });

  it('should apply the threshold the report was scored against rather than full coverage', () => {
    const result = transformer.transform(
      response({
        interfaces: [apiInterface({ testResults: [testResult({ coverage: 0.9, id: 'nine-tenths', isIncludedInQualityGate: true })] })],
        minCoveragePercentage: 95,
      }),
    );

    expect(result.interfaces[0].qualityGateFailures.map(failure => failure.id)).toEqual(['nine-tenths']);
  });

  /**
   * `minCoveragePercentage` is `required`, but the generated client assigns the JSON through without
   * validating it, so a server that omits it would leave the comparison against `NaN`. Reporting the
   * criterion as a failure is wrong and visible; reporting a failing gate as clean would not be.
   */
  it('should not clear an included criterion when the response omits the threshold entirely', () => {
    const result = transformer.transform(
      response({
        interfaces: [apiInterface({ testResults: [testResult({ coverage: 0.5, id: 'unmeasurable', isIncludedInQualityGate: true })] })],
        minCoveragePercentage: undefined as unknown as number,
      }),
    );

    expect(result.interfaces[0].qualityGateFailures.map(failure => failure.id)).toEqual(['unmeasurable']);
    expect(result.summary.qualityGateFailureCount).toBe(1);
  });

  /**
   * A `null` threshold is the other way the field arrives unusable, and it is the dangerous one: it
   * divides to `0` rather than `NaN`, so every coverage clears it and the report would come back
   * clean. The gate constrains the percentage to `[80, 100]`, so zero is never a bar to score against.
   */
  it('should not clear an included criterion when the response sends a null threshold', () => {
    const result = transformer.transform(
      response({
        interfaces: [apiInterface({ testResults: [testResult({ coverage: 0.5, id: 'unmeasurable', isIncludedInQualityGate: true })] })],
        minCoveragePercentage: null as unknown as number,
      }),
    );

    expect(result.interfaces[0].qualityGateFailures.map(failure => failure.id)).toEqual(['unmeasurable']);
    expect(result.summary.qualityGateFailureCount).toBe(1);
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
              // Below the bar and included: a failure.
              testResult({ coverage: 0.5, id: 'one-a', isIncludedInQualityGate: true }),
              // Below the bar but excluded: not one.
              testResult({ coverage: 0.5, id: 'one-b', isIncludedInQualityGate: false }),
            ],
          }),
          apiInterface({
            apiName: 'api-two',
            testResults: [
              testResult({ coverage: 0, id: 'two-a', isIncludedInQualityGate: true }),
              testResult({ coverage: 0.79, id: 'two-b', isIncludedInQualityGate: true }),
              // Clears the bar, so it does not count.
              testResult({ coverage: 1, id: 'two-c', isIncludedInQualityGate: true }),
            ],
          }),
        ],
      }),
    );

    expect(result.interfaces[0].qualityGateFailures.map(failure => failure.id)).toEqual(['one-a']);
    expect(result.interfaces[1].qualityGateFailures.map(failure => failure.id)).toEqual(['two-a', 'two-b']);
    expect(result.summary.qualityGateFailureCount).toBe(3);
  });
});
