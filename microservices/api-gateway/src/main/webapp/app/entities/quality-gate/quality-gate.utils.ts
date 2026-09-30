/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTest } from 'app/shared/model/api-test.model';

import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';

export interface QualityGateFilterParams {
  serviceName: string;
  apiName: string;
  apiVersion: string;
}

export function extractQualityGateFilterParams(search: string): QualityGateFilterParams {
  const params = new URLSearchParams(search);
  return {
    serviceName: params.get('serviceName') ?? '',
    apiName: params.get('apiName') ?? '',
    apiVersion: params.get('apiVersion') ?? '',
  };
}

export function countActiveFilters(params: QualityGateFilterParams): number {
  return [params.serviceName, params.apiName, params.apiVersion].filter(Boolean).length;
}

export interface ServiceGroup {
  serviceName: string;
  apiTests: IApiTest[];
}

const compareNullable = (a?: string, b?: string): number => (a ?? '').localeCompare(b ?? '');

export function groupByService(apiTests: IApiTest[]): ServiceGroup[] {
  const sorted = [...apiTests].sort(
    (a, b) =>
      compareNullable(a.serviceName, b.serviceName) || compareNullable(a.apiName, b.apiName) || compareNullable(a.apiVersion, b.apiVersion),
  );

  return sorted.reduce((groups: ServiceGroup[], apiTest: IApiTest) => {
    const serviceName = apiTest.serviceName ?? '';
    const last = groups.at(-1);
    if (last?.serviceName === serviceName) {
      last.apiTests.push(apiTest);
    } else {
      groups.push({ serviceName, apiTests: [apiTest] });
    }
    return groups;
  }, []);
}

export function calculateApiTestStatus(apiTest: IApiTest, qualityGateTimedOut: boolean): ReportStatus {
  if (apiTest.status && [ReportStatus.FAILED, ReportStatus.FINISHED_EXCEPTIONALLY, ReportStatus.PASSED].includes(apiTest.status)) {
    return apiTest.status;
  } else if (qualityGateTimedOut) {
    return ReportStatus.TIMED_OUT;
  } else {
    return ReportStatus.NOT_STARTED;
  }
}

// Most severe first: a run error outranks a failed gate, which outranks anything still unsettled.
const STATUS_SEVERITY: ReportStatus[] = [
  ReportStatus.FINISHED_EXCEPTIONALLY,
  ReportStatus.FAILED,
  ReportStatus.TIMED_OUT,
  ReportStatus.IN_PROGRESS,
  ReportStatus.NOT_STARTED,
  ReportStatus.PASSED,
];

export function mostSevereStatus(statuses: ReportStatus[]): ReportStatus {
  return STATUS_SEVERITY.find(status => statuses.includes(status)) ?? ReportStatus.NOT_STARTED;
}
