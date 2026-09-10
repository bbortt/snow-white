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

  private transformInterface(api: ListQualityGateReports200ResponseInnerInterfacesInner): AgenticQualityGateInterface {
    const qualityGateFailures = (api.testResults ?? []).filter(testResult => testResult.isIncludedInQualityGate);

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
    const interfaces = (response.interfaces ?? []).map(api => this.transformInterface(api));

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
