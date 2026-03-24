/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import axios, { AxiosError } from 'axios';
import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';
import { readFileSync } from 'node:fs';
import { exit } from 'node:process';

import { INVALID_CONFIG_FORMAT, PRERELEASE_UPLOAD_FAILED } from '../common/exit-codes';
import { scanGlob } from '../common/glob';
import { resolveConfig } from '../config/resolve-config';
import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH, uploadPrereleases } from './upload-prereleases';

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:process', () => ({
  exit: mock().mockImplementation((code: number) => {
    throw new Error(`Process exited with code ${code}`);
  }),
}));

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('../config/resolve-config', () => ({
  resolveConfig: mock(),
}));

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('../common/glob', () => ({
  scanGlob: mock(),
}));

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:fs', () => ({
  readFileSync: mock(),
}));

const mockConsoleLog = spyOn(console, 'log');
const mockConsoleWarn = spyOn(console, 'warn');
const mockConsoleError = spyOn(console, 'error');
const mockAxiosPost = spyOn(axios, 'post');

const BASE_URL = 'http://localhost:8080';

const VALID_YAML = `
openapi: 3.1.0
info:
  title: My Test API
  version: 1.2.3
  x-service-name: my-service
`.trim();

describe('upload-prereleases action', () => {
  beforeEach(() => {
    mockConsoleLog.mockReset();
    mockConsoleWarn.mockReset();
    mockConsoleError.mockReset();
    mockAxiosPost.mockReset();

    // @ts-expect-error TS2339: Property mockClear does not exist on type
    exit.mockClear();
    (resolveConfig as any).mockReset();
    (scanGlob as any).mockReset();
    (readFileSync as any).mockReset();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  describe('URL resolution', () => {
    it('uses --url directly without consulting the config file', async () => {
      (scanGlob as any).mockReturnValue([]);

      await uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL });

      expect(resolveConfig).not.toHaveBeenCalled();
      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining(`Base URL: ${BASE_URL}`));
    });

    it('reads URL from config file when --url is not provided', async () => {
      (resolveConfig as any).mockReturnValue({ url: BASE_URL });
      (scanGlob as any).mockReturnValue([]);

      await uploadPrereleases({ globPattern: '*.yaml' });

      expect(resolveConfig).toHaveBeenCalledWith(undefined);
      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining(`Base URL: ${BASE_URL}`));
    });

    it('passes --config-file path to resolveConfig', async () => {
      (resolveConfig as any).mockReturnValue({ url: BASE_URL });
      (scanGlob as any).mockReturnValue([]);

      await uploadPrereleases({ configFile: '/path/to/config.json', globPattern: '*.yaml' });

      expect(resolveConfig).toHaveBeenCalledWith('/path/to/config.json');
    });

    it('exits when URL is absent from the config file', () => {
      (resolveConfig as any).mockReturnValue({});

      expect(() => uploadPrereleases({ globPattern: '*.yaml' })).toThrow(`Process exited with code ${INVALID_CONFIG_FORMAT}`);

      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌  Snow-White base URL must be defined via --url or in the configuration file.'),
      );
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });
  });

  describe('file scanning', () => {
    it('warns and returns early when no files match the pattern', async () => {
      (scanGlob as any).mockReturnValue([]);

      await uploadPrereleases({ globPattern: 'services/**/openapi.yaml', url: BASE_URL });

      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  No files matched the pattern: services/**/openapi.yaml'));
      expect(mockAxiosPost).not.toHaveBeenCalled();
    });

    it('logs the number of matched files', async () => {
      (scanGlob as any).mockReturnValue(['svc-a/openapi.yaml', 'svc-b/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockAxiosPost.mockResolvedValue({ status: 201 });

      await uploadPrereleases({ globPattern: 'svc-*/openapi.yaml', url: BASE_URL });

      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('Found 2 file(s) to upload.'));
    });

    it('always logs the temporary nature warning', async () => {
      (scanGlob as any).mockReturnValue([]);

      await uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL });

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
      mockAxiosPost.mockResolvedValue({ status: 201 });

      await uploadPrereleases({ globPattern: 'services/**/openapi.yaml', url: BASE_URL });

      expect(mockAxiosPost).toHaveBeenCalledWith(
        `${BASE_URL}/api/rest/v1/apis`,
        expect.objectContaining({
          apiName: 'My Test API',
          apiType: 'OPENAPI',
          apiVersion: '1.2.3',
          content: VALID_YAML,
          prerelease: true,
          serviceName: 'my-service',
        }),
        expect.objectContaining({ headers: { 'Content-Type': 'application/json' } }),
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
      mockAxiosPost.mockResolvedValue({ status: 201 });

      await uploadPrereleases({
        apiNamePath: 'metadata.name',
        apiVersionPath: 'metadata.release',
        globPattern: '*.yaml',
        serviceNamePath: 'metadata.owner',
        url: BASE_URL,
      });

      expect(mockAxiosPost).toHaveBeenCalledWith(
        `${BASE_URL}/api/rest/v1/apis`,
        expect.objectContaining({ apiName: 'Custom API', apiVersion: '2.0.0', serviceName: 'custom-service' }),
        expect.anything(),
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

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  openapi.yaml: Missing required metadata fields.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_API_NAME_PATH}' not found or empty.`));
      expect(mockAxiosPost).not.toHaveBeenCalled();
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

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

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

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_SERVICE_NAME_PATH}' not found or empty.`));
    });

    it('reports all missing metadata fields when none are present', () => {
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue('openapi: 3.1.0');

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  openapi.yaml: Missing required metadata fields.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_API_NAME_PATH}' not found or empty.`));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_API_VERSION_PATH}' not found or empty.`));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`'${DEFAULT_SERVICE_NAME_PATH}' not found or empty.`));
      expect(mockAxiosPost).not.toHaveBeenCalled();
    });

    it('uses file:// scheme in sourceUrl', async () => {
      (scanGlob as any).mockReturnValue(['services/my-api/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockAxiosPost.mockResolvedValue({ status: 201 });

      await uploadPrereleases({ globPattern: 'services/**/openapi.yaml', url: BASE_URL });

      expect(mockAxiosPost).toHaveBeenCalledWith(
        `${BASE_URL}/api/rest/v1/apis`,
        expect.objectContaining({ sourceUrl: expect.stringContaining('file://') }),
        expect.anything(),
      );
    });

    it('counts a file read error as a failed upload', () => {
      (scanGlob as any).mockReturnValue(['unreadable.yaml']);
      (readFileSync as any).mockImplementation(() => {
        throw new Error('EACCES: permission denied');
      });

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  unreadable.yaml: Upload failed.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Error: EACCES: permission denied'));
      expect(mockAxiosPost).not.toHaveBeenCalled();
    });
  });

  describe('HTTP upload error handling', () => {
    beforeEach(() => {
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
    });

    it('logs status and message body on HTTP error with message', () => {
      const axiosError = new AxiosError('Conflict', 'ERR_BAD_REQUEST', {} as any, {}, {
        data: { message: 'API already exists as stable' },
        status: 409,
        statusText: 'Conflict',
      } as any);
      mockAxiosPost.mockRejectedValue(axiosError);

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  openapi.yaml: Upload failed.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Status: 409'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Details: API already exists as stable'));
    });

    it('logs status and statusText on HTTP error without message body', () => {
      const axiosError = new AxiosError('Internal Server Error', 'ERR_BAD_RESPONSE', {} as any, {}, {
        data: null,
        status: 500,
        statusText: 'Internal Server Error',
      } as any);
      mockAxiosPost.mockRejectedValue(axiosError);

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Status: 500'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Error: Internal Server Error'));
    });

    it('logs network error when no response received', () => {
      const axiosError = new AxiosError('Network Error', 'ERR_NETWORK', {} as any, { timeout: 5000 });
      mockAxiosPost.mockRejectedValue(axiosError);

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  No response received from server.'));
      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Check if the service is running and accessible.'));
    });

    it('logs message for generic Error instances', () => {
      mockAxiosPost.mockRejectedValue(new Error('Unexpected failure'));

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Error: Unexpected failure'));
    });

    it('serialises non-Error thrown values', () => {
      mockAxiosPost.mockRejectedValue({ custom: 'error object' });

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('\t  Error: {"custom":"error object"}'));
    });
  });

  describe('exit behaviour', () => {
    it('exits with PRERELEASE_UPLOAD_FAILED when at least one upload fails', () => {
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockAxiosPost.mockRejectedValue(new Error('upload error'));

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(exit).toHaveBeenCalledWith(PRERELEASE_UPLOAD_FAILED);
    });

    it('does not exit when all uploads succeed', async () => {
      (scanGlob as any).mockReturnValue(['a.yaml', 'b.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockAxiosPost.mockResolvedValue({ status: 201 });

      await uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL });

      expect(exit).not.toHaveBeenCalled();
    });

    it('processes all files before exiting on partial failure', () => {
      (scanGlob as any).mockReturnValue(['good.yaml', 'bad.yaml', 'also-good.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);
      mockAxiosPost
        .mockResolvedValueOnce({ status: 201 })
        .mockRejectedValueOnce(new Error('upload error'))
        .mockResolvedValueOnce({ status: 201 });

      expect(uploadPrereleases({ globPattern: '*.yaml', url: BASE_URL })).rejects.toThrow(
        `Process exited with code ${PRERELEASE_UPLOAD_FAILED}`,
      );

      expect(mockAxiosPost).toHaveBeenCalledTimes(3);
      expect(mockConsoleLog).toHaveBeenCalledWith(expect.stringContaining('Upload complete: 2 succeeded, 1 failed.'));
    });
  });
});
