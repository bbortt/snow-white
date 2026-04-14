/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';

import { GetAllApis200ResponseInnerApiTypeEnum } from '../../clients/api-index-api';
import { uploadApiSpec } from './upload-api-spec';

const ingestApiMock = mock(async () => {});

describe('uploadApiSpec', () => {
  let consoleLogSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleLogSpy = spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleLogSpy.mockRestore();
  });

  it('uploads the API spec and logs success', async () => {
    const apiSpecMetadata = {
      apiName: 'payments',
      apiVersion: '1.2.3',
      serviceName: 'billing-service',
    };

    const content = '{"openapi":"3.1.0"}';
    const url = 'https://api-index.example.com';
    const file = 'payments.yaml';

    const apiIndexApi = {
      ingestApi: ingestApiMock,
    };

    await uploadApiSpec(apiSpecMetadata, content, url, apiIndexApi as any, file);

    expect(ingestApiMock).toHaveBeenCalledTimes(1);
    expect(ingestApiMock).toHaveBeenCalledWith({
      getAllApis200ResponseInner: {
        apiName: 'payments',
        apiType: GetAllApis200ResponseInnerApiTypeEnum.Openapi,
        apiVersion: '1.2.3',
        content,
        prerelease: true,
        serviceName: 'billing-service',
        sourceUrl: 'https://api-index.example.com/api/rest/v1/apis/billing-service/payments/1.2.3/raw',
      },
    });

    expect(consoleLogSpy).toHaveBeenCalledTimes(1);
    expect(consoleLogSpy.mock.calls[0][0]).toContain('payments.yaml: Uploaded billing-service/payments@1.2.3');
  });
});
