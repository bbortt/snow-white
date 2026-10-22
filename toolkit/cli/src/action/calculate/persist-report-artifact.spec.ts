/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';

import { persistReportArtifact } from './persist-report-artifact';

const writeFileSyncMock = mock();

void mock.module('node:fs', () => ({
  writeFileSync: writeFileSyncMock,
}));

describe('persistReportArtifact', () => {
  let consoleLogSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleLogSpy = spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleLogSpy.mockClear();
    writeFileSyncMock.mockClear();
  });

  it('should write the body to the given path and announce it', () => {
    persistReportArtifact('./report.json', '{"a":1}', 'JSON report', false);

    expect(writeFileSyncMock).toHaveBeenCalledWith('./report.json', '{"a":1}', 'utf8');
    expect(consoleLogSpy).toHaveBeenCalledWith(expect.stringContaining('JSON report written to: ./report.json'));
  });

  it('should write the file but announce nothing in agentic mode', () => {
    persistReportArtifact('./report.json', '{"a":1}', 'JSON report', true);

    expect(writeFileSyncMock).toHaveBeenCalledWith('./report.json', '{"a":1}', 'utf8');
    expect(consoleLogSpy).not.toHaveBeenCalled();
  });

  it('should propagate file system write errors', () => {
    writeFileSyncMock.mockImplementationOnce(() => {
      throw new Error('Disk full');
    });

    expect(() => persistReportArtifact('./report.json', '{}', 'JSON report', false)).toThrow('Disk full');

    expect(consoleLogSpy).not.toHaveBeenCalled();
  });
});
