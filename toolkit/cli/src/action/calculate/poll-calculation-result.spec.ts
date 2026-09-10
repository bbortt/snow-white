/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';
import { exit } from 'node:process';

import type { ReportApi } from '../../clients/report-api';

import { ListQualityGateReports200ResponseInnerStatusEnum } from '../../clients/report-api';
import { QUALITY_GATE_FAILED } from '../../common/exit-codes';
import { pollCalculationResult } from './poll-calculation-result';

await mock.module('node:process', () => ({
  exit: mock(),
}));

void mock.module('node:timers/promises', () => ({
  setTimeout: mock(() => undefined),
}));

// Override any leaked module mock from other test files with the real implementation.
// eslint-disable-next-line @typescript-eslint/no-require-imports
void mock.module('./poll-calculation-result', () => require('./poll-calculation-result'));

describe('pollCalculationResult', () => {
  let consoleLogSpy: ReturnType<typeof spyOn>;
  let consoleErrorSpy: ReturnType<typeof spyOn>;
  let consoleDebugSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleLogSpy = spyOn(console, 'log').mockImplementation(() => {});
    consoleErrorSpy = spyOn(console, 'error').mockImplementation(() => {});
    consoleDebugSpy = spyOn(console, 'debug').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleLogSpy.mockClear();
    consoleErrorSpy.mockClear();
    consoleDebugSpy.mockClear();

    (exit as any).mockClear();
  });

  it('should poll until calculation passes', async () => {
    const reportApi: ReportApi = {
      getReportByCalculationId: mock()
        .mockResolvedValueOnce({
          status: ListQualityGateReports200ResponseInnerStatusEnum.InProgress,
        })
        .mockResolvedValueOnce({
          status: ListQualityGateReports200ResponseInnerStatusEnum.Passed,
        }),
    } as unknown as ReportApi;

    await pollCalculationResult(reportApi, 'calc-123');

    expect(reportApi.getReportByCalculationId).toHaveBeenCalledTimes(2);

    expect(reportApi.getReportByCalculationId).toHaveBeenNthCalledWith(1, {
      calculationId: 'calc-123',
    });

    expect(consoleLogSpy).toHaveBeenCalledWith(expect.stringContaining('Quality-Gate passed'));

    expect(exit).not.toHaveBeenCalled();
  });

  it('should return false when calculation fails', async () => {
    const reportApi: ReportApi = {
      getReportByCalculationId: mock(() => ({
        status: ListQualityGateReports200ResponseInnerStatusEnum.Failed,
      })),
    } as unknown as ReportApi;

    const result = await pollCalculationResult(reportApi, 'calc-failed');

    expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('Quality-Gate calculation'));

    expect(result).toBe(false);
    expect(exit).not.toHaveBeenCalled();
  });

  it('should exit when calculation is erroneous', async () => {
    const reportApi: ReportApi = {
      getReportByCalculationId: mock(() => ({
        status: ListQualityGateReports200ResponseInnerStatusEnum.FinishedExceptionally,
      })),
    } as unknown as ReportApi;

    const result = await pollCalculationResult(reportApi, 'calc-failed');

    expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('Quality-Gate calculation'));

    expect(result).toBe(false);
    expect(exit).toHaveBeenCalledWith(QUALITY_GATE_FAILED);
  });

  it('should print stack trace when available', async () => {
    const reportApi: ReportApi = {
      getReportByCalculationId: mock(() => ({
        stackTrace: 'Something exploded',
        status: ListQualityGateReports200ResponseInnerStatusEnum.FinishedExceptionally,
      })),
    } as unknown as ReportApi;

    await pollCalculationResult(reportApi, 'calc-error');

    expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('Something exploded'));
  });

  it('should log intermediate polling states', async () => {
    const reportApi: ReportApi = {
      getReportByCalculationId: mock()
        .mockResolvedValueOnce({
          status: ListQualityGateReports200ResponseInnerStatusEnum.InProgress,
        })
        .mockResolvedValueOnce({
          status: ListQualityGateReports200ResponseInnerStatusEnum.Passed,
        }),
    } as unknown as ReportApi;

    await pollCalculationResult(reportApi, 'calc-progress');

    expect(consoleDebugSpy).toHaveBeenCalledWith(expect.stringContaining('IN_PROGRESS'));
    expect(consoleDebugSpy).toHaveBeenCalledWith(expect.stringContaining('PASSED'));
  });
});
