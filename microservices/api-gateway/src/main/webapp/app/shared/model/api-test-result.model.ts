/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestFinding } from 'app/shared/model/api-test-finding.model';

export interface IApiTestResult {
  id?: string;
  coverage?: number;
  // Absent on a report persisted before the counts existed - never read as 0.
  judgedTargetCount?: number;
  // A non-zero count beside a judgedTargetCount of 0 is a criterion waivers emptied: its coverage
  // of 1 is what an empty denominator returns, not full coverage.
  waivedTargetCount?: number;
  additionalInformation?: string;
  isIncludedInQualityGate: boolean;
  // Only the finished report by id carries findings; list entries and in-progress polls leave this undefined.
  findings?: IApiTestFinding[];
}

export const defaultValue: Readonly<IApiTestResult> = { isIncludedInQualityGate: false };
