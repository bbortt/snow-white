/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { FindingStatus } from 'app/shared/model/enumerations/finding-status.model';

export interface IFindingEvidence {
  traceId: string;
  testCaseName?: string;
}

export interface IApiTestFinding {
  status: FindingStatus;
  specPointer: string;
  httpPath?: string;
  httpMethod?: string;
  responseCode?: string;
  parameterName?: string;
  contentType?: string;
  evidence: IFindingEvidence[];
  // Present only on a WAIVED finding: quoted from the waiver the calculation was triggered with.
  waiverReason?: string;
  // The last day the waiver is in force, as a calendar date (YYYY-MM-DD) - not an instant.
  waiverExpiresOn?: string;
}
