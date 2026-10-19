/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestResult } from 'app/shared/model/api-test-result.model';
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

/**
 * A criterion whose every target was waived has nothing left to judge: its coverage of 1 is what an
 * empty denominator returns, not full coverage (CON-012). A report persisted before the counts
 * existed carries neither, and is never fully waived.
 */
export function isFullyWaived(apiTestResult: IApiTestResult): boolean {
  return apiTestResult.judgedTargetCount === 0 && (apiTestResult.waivedTargetCount ?? 0) > 0;
}

export interface CoverageSplit {
  total: number;
  covered: number;
  waived: number;
  uncovered: number;
}

/**
 * Splits a set of criteria into covered, waived and uncovered shares, one unit per criterion.
 *
 * Each result contributes its fractional coverage rather than being bucketed by a pass/fail
 * threshold. A criterion waivers emptied contributes its whole unit to the waived share instead:
 * counting the coverage of 1 it publishes as covered would present a waiver as coverage. A partly
 * waived criterion keeps its ratio over what it did judge.
 */
export function splitCoverage(apiTestResults: IApiTestResult[]): CoverageSplit {
  const waived = apiTestResults.filter(isFullyWaived).length;
  const covered = apiTestResults
    .filter(apiTestResult => !isFullyWaived(apiTestResult))
    .reduce((sum, apiTestResult) => sum + (apiTestResult.coverage ?? 0), 0);

  return { total: apiTestResults.length, covered, waived, uncovered: apiTestResults.length - covered - waived };
}
