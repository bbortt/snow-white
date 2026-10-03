/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type {
  ListQualityGateReports200ResponseInner,
  ListQualityGateReports200ResponseInnerInterfacesInner,
  ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner,
} from '../../clients/report-api';

import {
  ListQualityGateReports200ResponseInnerInterfacesInnerApiTypeEnum,
  ListQualityGateReports200ResponseInnerInterfacesInnerStatusEnum,
} from '../../clients/report-api';

export const AGENTIC_SCHEMA_VERSION = '1';

export interface AgenticQualityGateResponse {
  schemaVersion: typeof AGENTIC_SCHEMA_VERSION;
  status: string;
  calculationId: string;
  qualityGateConfigName: string;
  apiLocation: string;
  initiatedAt: string;
  summary: {
    apiCount: number;
    failedApiCount: number;
    qualityGateFailureCount: number;
  };
  interfaces: AgenticQualityGateInterface[];
}

export interface AgenticQualityGateInterface {
  serviceName: string;
  apiName: string;
  apiVersion: string;
  apiType: string;
  status: string;
  qualityGateFailures: ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner[];
  testResults: ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner[];
}

export class AgenticQualityGateResponseTransformer {
  constructor(private readonly apiBaseUrl: string) {}

  private buildApiLocation(calculationId: string): string {
    return `${this.apiBaseUrl}/api/rest/v1/reports/${calculationId}`;
  }

  /**
   * A criterion fails when the gate included it and its coverage is below the bar the report was
   * scored against — the same comparison the JUnit export applies, so the failure set here names the
   * criteria that export emits as `<failure>` elements.
   *
   * The bar arrives as a percentage and `coverage` is a ratio, hence the division. A criterion the
   * gate excluded is never a failure whatever its coverage, and one that clears the bar without
   * reaching full coverage is not one either — the export passes it and explains the gap in
   * `system-out`.
   */
  private transformInterface(
    api: ListQualityGateReports200ResponseInnerInterfacesInner,
    minCoveragePercentage: number,
  ): AgenticQualityGateInterface {
    const threshold = minCoveragePercentage / 100;

    const qualityGateFailures = (api.testResults ?? []).filter(
      testResult => testResult.isIncludedInQualityGate && testResult.coverage < threshold,
    );

    return {
      apiName: api.apiName,
      apiType: api.apiType ?? ListQualityGateReports200ResponseInnerInterfacesInnerApiTypeEnum.Unspecified,
      apiVersion: api.apiVersion ?? '*',
      qualityGateFailures,
      serviceName: api.serviceName,
      status: api.status ?? ListQualityGateReports200ResponseInnerInterfacesInnerStatusEnum.InProgress,
      testResults: api.testResults ?? [],
    };
  }

  transform(response: ListQualityGateReports200ResponseInner): AgenticQualityGateResponse {
    const interfaces = (response.interfaces ?? []).map(api => this.transformInterface(api, response.minCoveragePercentage));

    const qualityGateFailureCount = interfaces.reduce((count, api) => count + api.qualityGateFailures.length, 0);

    return {
      apiLocation: this.buildApiLocation(response.calculationId),
      calculationId: response.calculationId,
      initiatedAt: response.initiatedAt.toISOString(),
      interfaces,
      qualityGateConfigName: response.qualityGateConfigName,
      schemaVersion: AGENTIC_SCHEMA_VERSION,
      status: response.status,
      summary: {
        apiCount: interfaces.length,
        failedApiCount: interfaces.filter(api => api.status === 'FAILED').length,
        qualityGateFailureCount,
      },
    };
  }
}
