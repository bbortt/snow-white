/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it, mock } from 'bun:test';

import { ApiIndexApi, Configuration } from '../clients/api-index-api';
import { getApiIndexApi } from './api-index-api';

await mock.module('../clients/api-index-api', () => {
  const MockConfiguration = mock((options: { basePath: string }) => ({ basePath: options.basePath }));
  const MockApiIndexApi = mock((config: object) => ({ config }));
  return { ApiIndexApi: MockApiIndexApi, Configuration: MockConfiguration };
});

describe('getApiIndexApi', () => {
  it('should return an instance of ApiIndexApi', () => {
    const result = getApiIndexApi('http://localhost:8080');

    expect(result).toBeInstanceOf(ApiIndexApi);
  });

  it('should configure ApiIndexApi with the provided baseUrl', () => {
    const baseUrl = 'http://localhost:8080';

    getApiIndexApi(baseUrl);

    expect(Configuration).toHaveBeenCalledWith({ basePath: baseUrl });
  });

  it('should pass the Configuration instance to ApiIndexApi', () => {
    const baseUrl = 'http://localhost:8080';

    getApiIndexApi(baseUrl);

    const configInstance = (Configuration as ReturnType<typeof mock>).mock.results[0].value;
    expect(ApiIndexApi).toHaveBeenCalledWith(configInstance);
  });

  it('should return a distinct instance per call', () => {
    const first = getApiIndexApi('http://localhost:8080');
    const second = getApiIndexApi('http://localhost:8080');

    expect(first).not.toBe(second);
  });

  it('should use different baseUrls for different instances', () => {
    const urlA = 'http://host-a:8080';
    const urlB = 'http://host-b:9090';

    getApiIndexApi(urlA);
    getApiIndexApi(urlB);

    const calls = (Configuration as ReturnType<typeof mock>).mock.calls;
    expect(calls.at(-2)).toEqual([{ basePath: urlA }]);
    expect(calls.at(-1)).toEqual([{ basePath: urlB }]);
  });
});
