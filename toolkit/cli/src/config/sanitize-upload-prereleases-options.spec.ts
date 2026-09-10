/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';
import { exit } from 'node:process';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH } from '../common/openapi';
import { resolveConfig } from './resolve-config';
import { sanitizeUploadPrereleasesOptions } from './sanitize-upload-prereleases-options';

await mock.module('node:process', () => ({
  exit: mock().mockImplementation((code: number) => {
    throw new Error(`Process exited with code ${code}`);
  }),
}));

await mock.module('./resolve-config', () => ({
  resolveConfig: mock(),
}));

await mock.module('../common/glob', () => ({
  scanGlob: mock(),
}));

await mock.module('node:fs', () => ({
  readFileSync: mock(),
}));

const BASE_URL = 'http://localhost:8080';

describe('sanitizeUploadPrereleasesOptions', () => {
  let consoleWarnSpy: ReturnType<typeof spyOn>;
  let consoleErrorSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleErrorSpy = spyOn(console, 'error').mockImplementation(() => {});
    consoleWarnSpy = spyOn(console, 'warn').mockImplementation(() => {});

    // @ts-expect-error TS2339: Property mockClear does not exist on type
    exit.mockClear();
    // @ts-expect-error TS2339: Property mockReset does not exist on type
    (resolveConfig as any).mockReset();
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
    consoleWarnSpy.mockRestore();
  });

  describe('URL resolution', () => {
    it('should use CLI url directly without consulting the config file', () => {
      const result = sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml', url: BASE_URL });

      expect(resolveConfig).not.toHaveBeenCalled();
      expect(result.url).toBe(BASE_URL);
    });

    it('should read URL from config file when --url is not provided', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      const result = sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml' });

      expect(resolveConfig).toHaveBeenCalledWith(undefined);
      expect(result.url).toBe(BASE_URL);
    });

    it('should pass --config-file path to resolveConfig', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml', configFile: '/path/to/config.json' });

      expect(resolveConfig).toHaveBeenCalledWith('/path/to/config.json');
    });

    it('should exit with code 3 when URL is absent from both CLI and config file', () => {
      (resolveConfig as any).mockReturnValueOnce({});

      expect(() => sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml' })).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        expect.stringContaining('❌ Snow-White base URL must be defined via --url or in the configuration file.'),
      );
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should warn when CLI url overrides config file url', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: 'http://config-url.com' });

      const result = sanitizeUploadPrereleasesOptions({
        apiSpecs: '*.yaml',
        configFile: 'config.json',
        url: 'http://cli-url.com',
      });

      expect(result.url).toBe('http://cli-url.com');
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ CLI parameter --url overrides config file value: "http://config-url.com" → "http://cli-url.com"'),
      );
    });

    it('should not warn when CLI url matches config file url', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      const result = sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml', configFile: 'config.json', url: BASE_URL });

      expect(result.url).toBe(BASE_URL);
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });
  });

  describe('path parameter resolution', () => {
    it('should use hardcoded defaults when no CLI or config file values are provided', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      const result = sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml' });

      expect(result.apiNamePath).toBe(DEFAULT_API_NAME_PATH);
      expect(result.apiVersionPath).toBe(DEFAULT_API_VERSION_PATH);
      expect(result.serviceNamePath).toBe(DEFAULT_SERVICE_NAME_PATH);
    });

    it('should read path params from config file as fallback', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiNamePath: 'custom.name',
        apiVersionPath: 'custom.version',
        serviceNamePath: 'custom.service',
        url: BASE_URL,
      });

      const result = sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml' });

      expect(result.apiNamePath).toBe('custom.name');
      expect(result.apiVersionPath).toBe('custom.version');
      expect(result.serviceNamePath).toBe('custom.service');
    });

    it('should use CLI path params over config file values', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiNamePath: 'config.name',
        apiVersionPath: 'config.version',
        serviceNamePath: 'config.service',
        url: BASE_URL,
      });

      const result = sanitizeUploadPrereleasesOptions({
        apiNamePath: 'cli.name',
        apiSpecs: '*.yaml',
        apiVersionPath: 'cli.version',
        configFile: 'config.json',
        serviceNamePath: 'cli.service',
      });

      expect(result.apiNamePath).toBe('cli.name');
      expect(result.apiVersionPath).toBe('cli.version');
      expect(result.serviceNamePath).toBe('cli.service');
    });

    it('should warn when CLI api-name-path overrides config file value', () => {
      (resolveConfig as any).mockReturnValueOnce({ apiNamePath: 'config.name', url: BASE_URL });

      sanitizeUploadPrereleasesOptions({ apiNamePath: 'cli.name', apiSpecs: '*.yaml', configFile: 'config.json' });

      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ CLI parameter --api-name-path overrides config file value: "config.name" → "cli.name"'),
      );
    });

    it('should warn when CLI api-version-path overrides config file value', () => {
      (resolveConfig as any).mockReturnValueOnce({ apiVersionPath: 'config.version', url: BASE_URL });

      sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml', apiVersionPath: 'cli.version', configFile: 'config.json' });

      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ CLI parameter --api-version-path overrides config file value: "config.version" → "cli.version"'),
      );
    });

    it('should warn when CLI service-name-path overrides config file value', () => {
      (resolveConfig as any).mockReturnValueOnce({ serviceNamePath: 'config.service', url: BASE_URL });

      sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml', configFile: 'config.json', serviceNamePath: 'cli.service' });

      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ CLI parameter --service-name-path overrides config file value: "config.service" → "cli.service"'),
      );
    });
  });

  describe('apiSpecs resolution', () => {
    it('should use CLI --api-specs directly when provided', () => {
      const result = sanitizeUploadPrereleasesOptions({ apiSpecs: 'services/**/openapi.yaml', url: BASE_URL });

      expect(result.globPattern).toBe('services/**/openapi.yaml');
      expect(resolveConfig).not.toHaveBeenCalled();
    });

    it('should read apiSpecs from config file when not provided via CLI', () => {
      (resolveConfig as any).mockReturnValueOnce({ apiSpecs: 'services/**/openapi.yaml', url: BASE_URL });

      const result = sanitizeUploadPrereleasesOptions({});

      expect(result.globPattern).toBe('services/**/openapi.yaml');
    });

    it('should warn when CLI --api-specs overrides config file value', () => {
      (resolveConfig as any).mockReturnValueOnce({ apiSpecs: 'old/**/openapi.yaml', url: BASE_URL });

      const result = sanitizeUploadPrereleasesOptions({ apiSpecs: 'new/**/openapi.yaml', configFile: 'config.json' });

      expect(result.globPattern).toBe('new/**/openapi.yaml');
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ CLI parameter --api-specs overrides config file value: "old/**/openapi.yaml" → "new/**/openapi.yaml"'),
      );
    });

    it('should exit with code 3 when apiSpecs is absent from both CLI and config file', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      expect(() => sanitizeUploadPrereleasesOptions({})).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        expect.stringContaining('❌ API specs glob pattern must be defined via --api-specs or in the configuration file.'),
      );
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });
  });

  describe('option passthrough', () => {
    it('should pass through globPattern and ignoreExisting', () => {
      const result = sanitizeUploadPrereleasesOptions({
        apiSpecs: 'services/**/openapi.yaml',
        ignoreExisting: true,
        url: BASE_URL,
      });

      expect(result.globPattern).toBe('services/**/openapi.yaml');
      expect(result.ignoreExisting).toBe(true);
    });

    it('should default ignoreExisting to false when not provided', () => {
      const result = sanitizeUploadPrereleasesOptions({ apiSpecs: '*.yaml', url: BASE_URL });

      expect(result.ignoreExisting).toBe(false);
    });
  });
});
