/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it, mock } from 'bun:test';

import { Configuration, QualityGateApi } from '../clients/quality-gate-api';
import { getQualityGateApi } from './quality-gate-api';

await mock.module('../clients/quality-gate-api', () => {
  const MockConfiguration = mock((options: { basePath: string }) => ({ basePath: options.basePath }));
  const MockQualityGateApi = mock((config: object) => ({ config }));
  return { Configuration: MockConfiguration, QualityGateApi: MockQualityGateApi };
});

describe('getQualityGateApi', () => {
  it('should return an instance of QualityGateApi', () => {
    const result = getQualityGateApi('http://localhost:8081');

    expect(result).toBeInstanceOf(QualityGateApi);
  });

  it('should configure QualityGateApi with the provided baseUrl', () => {
    const baseUrl = 'http://localhost:8081';

    getQualityGateApi(baseUrl);

    expect(Configuration).toHaveBeenCalledWith({ basePath: baseUrl });
  });

  it('should pass the Configuration instance to QualityGateApi', () => {
    const baseUrl = 'http://localhost:8081';

    getQualityGateApi(baseUrl);

    const configInstance = (Configuration as ReturnType<typeof mock>).mock.results[0].value;
    expect(QualityGateApi).toHaveBeenCalledWith(configInstance);
  });

  it('should return a distinct instance per call', () => {
    const first = getQualityGateApi('http://localhost:8081');
    const second = getQualityGateApi('http://localhost:8081');

    expect(first).not.toBe(second);
  });

  it('should use different baseUrls for different instances', () => {
    const urlA = 'http://host-a:8081';
    const urlB = 'http://host-b:9091';

    getQualityGateApi(urlA);
    getQualityGateApi(urlB);

    const calls = (Configuration as ReturnType<typeof mock>).mock.calls;
    expect(calls.at(-2)).toEqual([{ basePath: urlA }]);
    expect(calls.at(-1)).toEqual([{ basePath: urlB }]);
  });
});
