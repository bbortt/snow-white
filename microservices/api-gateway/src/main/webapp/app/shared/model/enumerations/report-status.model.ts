/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export enum ReportStatus {
  NOT_STARTED = 'NOT_STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  FAILED = 'FAILED',
  FINISHED_EXCEPTIONALLY = 'FINISHED_EXCEPTIONALLY',
  PASSED = 'PASSED',
  TIMED_OUT = 'TIMED_OUT',
}
