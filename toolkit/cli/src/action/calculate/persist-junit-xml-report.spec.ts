/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';

import type { ReportApi } from '../../clients/report-api';

import { persistJUnitXmlReport } from './persist-junit-xml-report';

const writeFileSyncMock = mock();

void mock.module('node:fs', () => ({
  writeFileSync: writeFileSyncMock,
}));

describe('persistJUnitXmlReport', () => {
  let consoleLogSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleLogSpy = spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleLogSpy.mockClear();
    writeFileSyncMock.mockClear();
  });

  it('should fetch junit xml report and write it to disk', async () => {
    const blob = {
      text: mock(() => '<testsuite></testsuite>'),
    };

    const reportApi = {
      getReportByCalculationIdAsJUnit: mock().mockResolvedValueOnce(blob),
    };

    await persistJUnitXmlReport(reportApi, 'calc-123', './report.xml');

    expect(reportApi.getReportByCalculationIdAsJUnit).toHaveBeenCalledWith({
      calculationId: 'calc-123',
    });

    expect(blob.text).toHaveBeenCalled();

    expect(writeFileSyncMock).toHaveBeenCalledWith('./report.xml', '<testsuite></testsuite>', 'utf8');
  });

  it('should log success message after writing report', async () => {
    const blob = {
      text: mock(() => '<xml />'),
    };

    const reportApi: ReportApi = {
      getReportByCalculationIdAsJUnit: mock().mockResolvedValueOnce(blob),
    } as unknown as ReportApi;

    await persistJUnitXmlReport(reportApi, 'calc-log', './quality.xml');

    expect(consoleLogSpy).toHaveBeenCalledWith(expect.stringContaining('./quality.xml'));
  });

  it('should propagate errors from report api', () => {
    const reportApi: ReportApi = {
      getReportByCalculationIdAsJUnit: mock().mockRejectedValueOnce(new Error('API failure')),
    } as unknown as ReportApi;

    expect(persistJUnitXmlReport(reportApi, 'calc-error', './report.xml')).rejects.toThrow('API failure');

    expect(writeFileSyncMock).not.toHaveBeenCalled();
  });

  it('should propagate file system write errors', () => {
    writeFileSyncMock.mockImplementationOnce(() => {
      throw new Error('Disk full');
    });

    const blob = {
      text: mock().mockResolvedValueOnce('<testsuite />'),
    };

    const reportApi: ReportApi = {
      getReportByCalculationIdAsJUnit: mock().mockResolvedValueOnce(blob),
    } as unknown as ReportApi;

    expect(persistJUnitXmlReport(reportApi, 'calc-write-error', './report.xml')).rejects.toThrow('Disk full');
  });
});
