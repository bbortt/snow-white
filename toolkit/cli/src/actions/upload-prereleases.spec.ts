/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';
import { readFileSync } from 'node:fs';
import { exit } from 'node:process';

import type { ApiIndexApi } from '../clients/api-index-api';

import { PRERELEASE_UPLOAD_FAILED } from '../common/exit-codes';
import { scanGlob } from '../common/glob';
import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH } from '../common/openapi';
import { uploadPrereleases } from './upload-prereleases';

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:process', () => ({
  exit: mock().mockImplementation((code: number) => {
    throw new Error(`Process exited with code ${code}`);
  }),
}));

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('../common/glob', () => ({
  scanGlob: mock(),
}));

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:fs', () => ({
  readFileSync: mock(),
}));

const mockConsoleDebug = spyOn(console, 'debug');
const mockConsoleLog = spyOn(console, 'log');
const mockConsoleWarn = spyOn(console, 'warn');
const mockConsoleError = spyOn(console, 'error');

const BASE_URL = 'http://localhost:8080';

const VALID_YAML = `
openapi: 3.1.0
info:
  title: My Test API
  version: 1.2.3
  x-service-name: my-service
`.trim();

const makeResponseError = (status: number, statusText: string, jsonBody: unknown) =>
  Object.assign(new Error('Response returned an error code'), {
    name: 'ResponseError',
    response: {
      json: () => Promise.resolve(jsonBody),
      status,
      statusText,
    },
  });

const makeFetchError = (message: string) => Object.assign(new Error(message), { name: 'FetchError' });

const makeApiIndexApi = (ingestApi = mock()): ApiIndexApi => ({ ingestApi }) as unknown as ApiIndexApi;

describe('upload-prereleases action', () => {
  let mockIngestApi: ReturnType<typeof mock>;

  beforeEach(() => {
    mockConsoleDebug.mockReset();
    mockConsoleLog.mockReset();
    mockConsoleWarn.mockReset();
    mockConsoleError.mockReset();

    mockIngestApi = mock();

    // @ts-expect-error TS2339: Property mockClear does not exist on type
    exit.mockClear();
    (scanGlob as any).mockReset();
    (readFileSync as any).mockReset();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  describe('file scanning', () => {
    it('warns and returns early when no files match the pattern', async () => {
      (scanGlob as any).mockReturnValue([]);

      await uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: 'services/**/openapi.yaml', url: BASE_URL });

      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  No files matched the pattern: services/**/openapi.yaml'));
      expect(mockIngestApi).not.toHaveBeenCalled();
    });

    it('logs the number of matched files', async () => {
      (scanGlob as any).mockReturnValue(['svc-a/openapi.yaml', 'svc-b/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockIngestApi.mockResolvedValue(undefined);

      await uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: 'svc-*/openapi.yaml', url: BASE_URL });

      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('Found 2 file(s) to upload.'));
    });

    it('always logs the temporary nature warning', async () => {
      (scanGlob as any).mockReturnValue([]);

      await uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL });

      expect(mockConsoleLog).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  Prerelease uploads are temporary and will be cleaned up asynchronously after the pipeline completes.'),
      );
    });
  });

  describe('metadata extraction and upload', () => {
    it('uploads spec with the correct payload using default JSON paths', async () => {
      const file = 'services/my-api/openapi.yaml';
      (scanGlob as any).mockReturnValue([file]);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockIngestApi.mockResolvedValue(undefined);

      await uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: 'services/**/openapi.yaml', url: BASE_URL });

      expect(mockIngestApi).toHaveBeenCalledWith(
        expect.objectContaining({
          getAllApis200ResponseInner: expect.objectContaining({
            apiName: 'My Test API',
            apiType: 'OPENAPI',
            apiVersion: '1.2.3',
            content: VALID_YAML,
            prerelease: true,
            serviceName: 'my-service',
          }),
        }),
      );
      expect(mockConsoleLog).toHaveBeenCalledWith(
        expect.stringContaining('✅  services/my-api/openapi.yaml: Uploaded my-service/My Test API@1.2.3'),
      );
    });

    it('uses custom JSON paths when provided', async () => {
      const customYaml = `
metadata:
  name: Custom API
  release: 2.0.0
  owner: custom-service
`.trim();
      (scanGlob as any).mockReturnValue(['custom.yaml']);
      (readFileSync as any).mockReturnValue(customYaml);
      mockIngestApi.mockResolvedValue(undefined);

      await uploadPrereleases(makeApiIndexApi(mockIngestApi), {
        apiNamePath: 'metadata.name',
        apiVersionPath: 'metadata.release',
        globPattern: '*.yaml',
        serviceNamePath: 'metadata.owner',
        url: BASE_URL,
      });

      expect(mockIngestApi).toHaveBeenCalledWith(
        expect.objectContaining({
          getAllApis200ResponseInner: expect.objectContaining({
            apiName: 'Custom API',
            apiVersion: '2.0.0',
            serviceName: 'custom-service',
          }),
        }),
      );
    });

    it('reports missing api name and counts the file as failed', () => {
      const yamlMissingTitle = `
openapi: 3.1.0
info:
  version: 1.0.0
  x-service-name: my-service
`.trim();
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue(yamlMissingTitle);

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  openapi.yaml: Missing required metadata fields.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_API_NAME_PATH}' not found or empty.`));
      expect(mockIngestApi).not.toHaveBeenCalled();
    });

    it('reports missing api version and counts the file as failed', () => {
      const yamlMissingVersion = `
openapi: 3.1.0
info:
  title: My API
  x-service-name: my-service
`.trim();
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue(yamlMissingVersion);

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_API_VERSION_PATH}' not found or empty.`));
    });

    it('reports missing service name and counts the file as failed', () => {
      const yamlMissingServiceName = `
openapi: 3.1.0
info:
  title: My API
  version: 1.0.0
`.trim();
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue(yamlMissingServiceName);

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_SERVICE_NAME_PATH}' not found or empty.`));
    });

    it('reports all missing metadata fields when none are present', () => {
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue('openapi: 3.1.0');

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  openapi.yaml: Missing required metadata fields.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_API_NAME_PATH}' not found or empty.`));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_API_VERSION_PATH}' not found or empty.`));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_SERVICE_NAME_PATH}' not found or empty.`));
      expect(mockIngestApi).not.toHaveBeenCalled();
    });

    it('uses the server /raw endpoint as sourceUrl', async () => {
      (scanGlob as any).mockReturnValue(['services/my-api/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockIngestApi.mockResolvedValue(undefined);

      await uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: 'services/**/openapi.yaml', url: BASE_URL });

      expect(mockIngestApi).toHaveBeenCalledWith(
        expect.objectContaining({
          getAllApis200ResponseInner: expect.objectContaining({
            sourceUrl: `${BASE_URL}/api/rest/v1/apis/my-service/My Test API/1.2.3/raw`,
          }),
        }),
      );
    });

    it('counts a file read error as a failed upload', () => {
      (scanGlob as any).mockReturnValue(['unreadable.yaml']);
      (readFileSync as any).mockImplementation(() => {
        throw new Error('EACCES: permission denied');
      });

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  unreadable.yaml: Upload failed.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Error: EACCES: permission denied'));
      expect(mockIngestApi).not.toHaveBeenCalled();
    });
  });

  describe('HTTP upload error handling', () => {
    beforeEach(() => {
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
    });

    it('logs status and message body on HTTP error with message', () => {
      mockIngestApi.mockRejectedValue(makeResponseError(409, 'Conflict', { message: 'API already exists as stable' }));

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  openapi.yaml: Upload failed.'));
      expect(mockConsoleDebug).toHaveBeenCalledWith(expect.stringContaining('\t  Status: 409'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Details: API already exists as stable'));
    });

    it('ignores conflict HTTP status with error message when flag is set', async () => {
      mockIngestApi.mockRejectedValue(makeResponseError(409, 'Conflict', { message: 'API already exists as stable' }));

      await uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', ignoreExisting: true, url: BASE_URL });

      expect(exit).not.toHaveBeenCalled();
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('\t  ⚠️ Ignoring already existing my-service/My Test API@1.2.3'),
      );
      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('Upload complete: 0 succeeded, 1 failed.'));
    });

    it('logs status and statusText on HTTP error without message body', () => {
      mockIngestApi.mockRejectedValue(makeResponseError(500, 'Internal Server Error', null));

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  openapi.yaml: Upload failed.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Error: Internal Server Error'));
    });

    it('logs network error when no response received', () => {
      mockIngestApi.mockRejectedValue(makeFetchError('Network Error'));

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  No response received from server.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Check if the service is running and accessible.'));
    });

    it('logs message for generic Error instances', () => {
      mockIngestApi.mockRejectedValue(new Error('Unexpected failure'));

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Error: Unexpected failure'));
    });

    it('serialises non-Error thrown values', () => {
      mockIngestApi.mockRejectedValue({ custom: 'error object' });

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Error: {"custom":"error object"}'));
    });
  });

  describe('exit behaviour', () => {
    it('exits with PRERELEASE_UPLOAD_FAILED when at least one upload fails', () => {
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockIngestApi.mockRejectedValue(new Error('upload error'));

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
    });

    it('does not exit when all uploads succeed', async () => {
      (scanGlob as any).mockReturnValue(['a.yaml', 'b.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockIngestApi.mockResolvedValue(undefined);

      await uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL });

      expect(exit).not.toHaveBeenCalled();
    });

    it('processes all files before exiting on partial failure', () => {
      (scanGlob as any).mockReturnValue(['good.yaml', 'bad.yaml', 'also-good.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockIngestApi.mockResolvedValueOnce(undefined).mockRejectedValueOnce(new Error('upload error')).mockResolvedValueOnce(undefined);

      expect(uploadPrereleases(makeApiIndexApi(mockIngestApi), { globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow();

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
      expect(mockIngestApi).toHaveBeenCalledTimes(3);
      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('Upload complete: 2 succeeded, 1 failed.'));
    });
  });
});
