/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestFinding } from 'app/shared/model/api-test-finding.model';

export interface IApiTestResult {
  id?: string;
  coverage?: number;
  additionalInformation?: string;
  isIncludedInQualityGate: boolean;
  // Only the finished report by id carries findings; list entries and in-progress polls leave this undefined.
  findings?: IApiTestFinding[];
}

export const defaultValue: Readonly<IApiTestResult> = { isIncludedInQualityGate: false };
