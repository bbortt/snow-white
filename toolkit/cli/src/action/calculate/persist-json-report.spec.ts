/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';

import type { ReportApi } from '../../clients/report-api';

import { persistJsonReport } from './persist-json-report';

const writeFileSyncMock = mock();

void mock.module('node:fs', () => ({
  writeFileSync: writeFileSyncMock,
}));

describe('persistJsonReport', () => {
  let consoleLogSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleLogSpy = spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleLogSpy.mockClear();
    writeFileSyncMock.mockClear();
  });

  it('should fetch the report and write the response body to disk', async () => {
    const apiResponse = {
      raw: {
        text: mock(() => '{"calculationId":"calc-123"}'),
      },
      value: mock(),
    };

    const reportApi = {
      getReportByCalculationIdRaw: mock().mockResolvedValueOnce(apiResponse),
    } as unknown as ReportApi;

    await persistJsonReport(reportApi, 'calc-123', './report.json');

    expect(reportApi.getReportByCalculationIdRaw).toHaveBeenCalledWith({
      calculationId: 'calc-123',
    });

    expect(apiResponse.raw.text).toHaveBeenCalled();

    expect(writeFileSyncMock).toHaveBeenCalledWith('./report.json', '{"calculationId":"calc-123"}', 'utf8');
  });

  it('should write the response body verbatim, never the deserialized model', async () => {
    const apiResponse = {
      raw: {
        text: mock(() => '{"unknownToTheClient":true}'),
      },
      value: mock(() => ({ knownToTheClient: true })),
    };

    const reportApi = {
      getReportByCalculationIdRaw: mock().mockResolvedValueOnce(apiResponse),
    } as unknown as ReportApi;

    await persistJsonReport(reportApi, 'calc-verbatim', './report.json');

    expect(apiResponse.value).not.toHaveBeenCalled();

    expect(writeFileSyncMock).toHaveBeenCalledWith('./report.json', '{"unknownToTheClient":true}', 'utf8');
  });

  it('should log success message after writing report', async () => {
    const apiResponse = {
      raw: {
        text: mock(() => '{}'),
      },
    };

    const reportApi: ReportApi = {
      getReportByCalculationIdRaw: mock().mockResolvedValueOnce(apiResponse),
    } as unknown as ReportApi;

    await persistJsonReport(reportApi, 'calc-log', './quality.json');

    expect(consoleLogSpy).toHaveBeenCalledWith(expect.stringContaining('./quality.json'));
  });

  it('should propagate errors from report api', () => {
    const reportApi: ReportApi = {
      getReportByCalculationIdRaw: mock().mockRejectedValueOnce(new Error('API failure')),
    } as unknown as ReportApi;

    expect(persistJsonReport(reportApi, 'calc-error', './report.json')).rejects.toThrow('API failure');

    expect(writeFileSyncMock).not.toHaveBeenCalled();
  });

  it('should propagate file system write errors', () => {
    writeFileSyncMock.mockImplementationOnce(() => {
      throw new Error('Disk full');
    });

    const apiResponse = {
      raw: {
        text: mock().mockResolvedValueOnce('{}'),
      },
    };

    const reportApi: ReportApi = {
      getReportByCalculationIdRaw: mock().mockResolvedValueOnce(apiResponse),
    } as unknown as ReportApi;

    expect(persistJsonReport(reportApi, 'calc-write-error', './report.json')).rejects.toThrow('Disk full');
  });
});
