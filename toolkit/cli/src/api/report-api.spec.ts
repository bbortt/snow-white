/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it, mock } from 'bun:test';

import { Configuration, ReportApi } from '../clients/report-api';
import { getReportApi } from './report-api.ts';

await mock.module('../clients/report-api', () => {
  const MockConfiguration = mock((options: { basePath: string }) => ({ basePath: options.basePath }));
  const MockReportApi = mock(({ config }) => {
    return { config };
  });
  return { Configuration: MockConfiguration, ReportApi: MockReportApi };
});

describe('getReportApi', () => {
  it('should configure ReportApi with the provided baseUrl', () => {
    const baseUrl = 'http://localhost:8081';

    getReportApi(baseUrl);

    expect(Configuration).toHaveBeenCalledWith({ basePath: baseUrl, fetchApi: expect.anything() });
  });

  it('should pass the Configuration instance to ReportApi', () => {
    const baseUrl = 'http://localhost:8081';

    getReportApi(baseUrl);

    const configInstance = (Configuration as ReturnType<typeof mock>).mock.results[0].value;
    expect(ReportApi).toHaveBeenCalledWith(configInstance);
  });

  it('should return a distinct instance per call', () => {
    const first = getReportApi('http://localhost:8081');
    const second = getReportApi('http://localhost:8081');

    expect(first).not.toBe(second);
  });

  it('should use different baseUrls for different instances', () => {
    const urlA = 'http://host-a:8081';
    const urlB = 'http://host-b:9091';

    getReportApi(urlA);
    getReportApi(urlB);

    const calls = (Configuration as ReturnType<typeof mock>).mock.calls;
    expect(calls.at(-2)).toEqual([{ basePath: urlA, fetchApi: expect.anything() }]);
    expect(calls.at(-1)).toEqual([{ basePath: urlB, fetchApi: expect.anything() }]);
  });
});
