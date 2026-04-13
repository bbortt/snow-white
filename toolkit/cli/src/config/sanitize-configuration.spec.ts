/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';
import { readFileSync } from 'node:fs';
import { exit } from 'node:process';

import type { CliOptions } from './cli-options';
import type { CalculateOptions } from './sanitized-options';

import { DEFAULT_API_NAME_PATH, DEFAULT_API_VERSION_PATH, DEFAULT_SERVICE_NAME_PATH } from '../actions/upload-prereleases';
import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { scanGlob } from '../common/glob';
import { resolveConfig } from './resolve-config';
import { sanitizeCalculateOptions, sanitizeUploadPrereleasesOptions } from './sanitize-configuration';

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:process', () => ({
  exit: mock().mockImplementation((code: number) => {
    throw new Error(`Process exited with code ${code}`);
  }),
}));

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('./resolve-config', () => ({
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

const mockConsoleError = spyOn(console, 'error');
const mockConsoleWarn = spyOn(console, 'warn');

const incompleteApiInformation = [
  {
    apiName: 'test-api',
    apiVersion: 'test-version',
  },
  {
    apiVersion: 'test-version',
    serviceName: 'test-service',
  },
  {
    apiName: 'test-api',
    serviceName: 'test-service',
  },
];

const sanitizedOptions: CalculateOptions = {
  apiInformation: [
    {
      apiName: 'test-api',
      apiVersion: 'api-version',
      serviceName: 'test-service',
    },
  ],
  async: false,
  qualityGate: 'quality-gate',
  url: 'url',
};

describe('sanitizeConfiguration', () => {
  beforeEach(() => {
    mockConsoleError.mockReset();
    mockConsoleWarn.mockReset();

    // @ts-expect-error TS2339: Property mockClear does not exist on type
    exit.mockClear();
    // @ts-expect-error TS2339: Property mockReset does not exist on type
    (resolveConfig as any).mockReset();
    (scanGlob as any).mockReset();
    (readFileSync as any).mockReset();
  });

  it.each([
    // configFile combined with any exact config parameter
    {
      configFile: 'config',
      serviceName: 'test-service',
    },
    {
      apiName: 'test-api',
      configFile: 'config',
    },
    {
      apiVersion: 'test-version',
      configFile: 'config',
    },
  ])('should exit with code 3 when configFile is combined with exact config params: %s', (options: Partial<CliOptions>) => {
    // @ts-expect-error TS2345: Argument of type Partial<CliOptions> is not assignable to parameter of type CliOptions
    expect(() => sanitizeCalculateOptions(options)).toThrowError('Process exited with code 3');

    expect(mockConsoleError).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('❌  You cannot use options from multiple configuration groups together.'),
    );
    expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\tGroup 1: serviceName, apiName, apiVersion'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\tGroup 2: configFile'));

    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  });

  it.each([
    // openApiSpecs combined with any exact config parameter
    {
      openApiSpecs: 'some-glob-pattern',
      serviceName: 'test-service',
    },
    {
      apiName: 'test-api',
      openApiSpecs: 'some-glob-pattern',
    },
    {
      apiVersion: 'test-version',
      openApiSpecs: 'some-glob-pattern',
    },
  ])('should exit with code 3 when openApiSpecs is combined with exact config params: %s', (options: Partial<CliOptions>) => {
    // @ts-expect-error TS2345: Argument of type Partial<CliOptions> is not assignable to parameter of type CliOptions
    expect(() => sanitizeCalculateOptions(options)).toThrowError('Process exited with code 3');

    expect(mockConsoleError).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('❌  You cannot use options from multiple configuration groups together.'),
    );
    expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\tGroup 1: serviceName, apiName, apiVersion'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\tGroup 2: openApiSpecs'));

    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  });

  describe('file-based configuration', () => {
    it('resolves config from exact path if specified', () => {
      (resolveConfig as any).mockReturnValueOnce(sanitizedOptions);

      expect(sanitizeCalculateOptions({} as CliOptions)).toEqual(sanitizedOptions);

      expect(resolveConfig).toHaveBeenCalled();
    });

    const expectRecursiveOrEmptyConfiguration = () => {
      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌  Configuration file may not contain recursive references or be empty.'),
      );
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    };

    it('should exit with code 3 if file is empty', () => {
      (resolveConfig as any).mockReturnValueOnce({});

      expect(() => sanitizeCalculateOptions({} as CliOptions)).toThrowError('Process exited with code 3');

      expect(resolveConfig).toHaveBeenCalled();

      expectRecursiveOrEmptyConfiguration();
    });

    it('should exit with code 3 if file contains recursive configuration', () => {
      const configFile = 'configFile';

      (resolveConfig as any).mockReturnValueOnce({ configFile });

      expect(() => sanitizeCalculateOptions({ configFile } as CliOptions)).toThrowError('Process exited with code 3');

      expect(resolveConfig).toHaveBeenCalledWith(configFile);

      expectRecursiveOrEmptyConfiguration();
    });

    it.each(incompleteApiInformation)('should exit with code 3 if any property is missing: %s', (options: Partial<CliOptions>) => {
      (resolveConfig as any).mockReturnValueOnce({ apiInformation: [options], qualityGate: 'quality-gate', url: 'url' });

      expect(() => sanitizeCalculateOptions({ configFile: 'configFile' } as CliOptions)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌  Each API information must contain serviceName, apiName, and apiVersion.'),
      );

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no URL is provided', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiName: 'test-api',
        apiVersion: 'test-version',
        qualityGate: 'quality-gate',
        serviceName: 'test-service',
      });

      expect(() => sanitizeCalculateOptions({ configFile: 'configFile' } as CliOptions)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌  Snow-White base URL must be defined in the configuration.'),
      );

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no Quality-Gate is provided', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiName: 'test-api',
        apiVersion: 'test-version',
        serviceName: 'test-service',
        url: 'url',
      });

      expect(() => sanitizeCalculateOptions({ configFile: 'configFile' } as CliOptions)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  Quality-Gate name must be defined in the configuration.'));

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });
  });

  describe('CLI parameter precedence over config file', () => {
    it('should override URL from config file with CLI parameter', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        url: 'http://config-file-url.com',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ url: 'http://cli-url.com' } as CliOptions);

      expect(result.url).toBe('http://cli-url.com');
      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  CLI parameter --url overrides config file value'));
    });

    it('should override qualityGate from config file with CLI parameter', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        qualityGate: 'file-gate',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ qualityGate: 'cli-gate' } as CliOptions);

      expect(result.qualityGate).toBe('cli-gate');
      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  CLI parameter --quality-gate overrides config file value'));
    });

    it('should override lookbackWindow from config file with CLI parameter', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        lookbackWindow: '1h',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ lookbackWindow: '24h' } as CliOptions);

      expect(result.lookbackWindow).toBe('24h');
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  CLI parameter --lookback-window overrides config file value'),
      );
    });

    it('should override attributeFilters from config file with CLI filters', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        attributeFilters: { environment: 'production' },
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ filter: ['region=us-west-1'] } as CliOptions);

      expect(result.attributeFilters).toEqual({ region: 'us-west-1' });
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  CLI parameter --filter overrides config file attributeFilters'),
      );
    });

    it('should not warn when CLI parameter matches config file value', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        url: 'http://same-url.com',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ url: 'http://same-url.com' } as CliOptions);

      expect(result.url).toBe('http://same-url.com');
      expect(mockConsoleWarn).not.toHaveBeenCalled();
    });
  });

  describe('filter parsing', () => {
    it('should parse multiple filters correctly', () => {
      const result = sanitizeCalculateOptions({
        apiName: 'test-api',
        apiVersion: 'api-version',
        filter: ['environment=production', 'region=us-west-1'],
        qualityGate: 'quality-gate',
        serviceName: 'test-service',
        url: 'url',
      });

      expect(result.attributeFilters).toEqual({
        environment: 'production',
        region: 'us-west-1',
      });
    });

    it('should warn and skip invalid filter format', () => {
      const result = sanitizeCalculateOptions({
        apiName: 'test-api',
        apiVersion: 'api-version',
        filter: ['invalid-filter', 'valid=filter'],
        qualityGate: 'quality-gate',
        serviceName: 'test-service',
        url: 'url',
      });

      expect(result.attributeFilters).toEqual({ valid: 'filter' });
      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  Ignoring invalid filter format: "invalid-filter"'));
    });

    it('should handle filter with equals sign in value', () => {
      const result = sanitizeCalculateOptions({
        apiName: 'test-api',
        apiVersion: 'api-version',
        filter: ['query=a=b'],
        qualityGate: 'quality-gate',
        serviceName: 'test-service',
        url: 'url',
      });

      expect(result.attributeFilters).toEqual({ query: 'a=b' });
    });
  });

  describe('lookbackWindow validation', () => {
    it('should accept valid lookback window formats', () => {
      const result = sanitizeCalculateOptions({
        apiName: 'test-api',
        apiVersion: 'api-version',
        lookbackWindow: '24h',
        qualityGate: 'quality-gate',
        serviceName: 'test-service',
        url: 'url',
      });

      expect(result.lookbackWindow).toBe('24h');
      expect(mockConsoleWarn).not.toHaveBeenCalled();
    });

    it('should warn for potentially invalid lookback window format', () => {
      const result = sanitizeCalculateOptions({
        apiName: 'test-api',
        apiVersion: 'api-version',
        lookbackWindow: 'invalid',
        qualityGate: 'quality-gate',
        serviceName: 'test-service',
        url: 'url',
      });

      expect(result.lookbackWindow).toBe('invalid');
      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  Lookback window "invalid" may not be in expected format'));
    });
  });

  describe('explicit configuration', () => {
    it('should return sanitized options when all required properties are provided', () => {
      expect(
        sanitizeCalculateOptions({
          apiName: 'test-api',
          apiVersion: 'api-version',
          qualityGate: 'quality-gate',
          serviceName: 'test-service',
          url: 'url',
        }),
      ).toEqual(sanitizedOptions);

      expect(mockConsoleError).not.toHaveBeenCalled();

      expect(exit).not.toHaveBeenCalled();
    });

    it('should include optional lookbackWindow and attributeFilters', () => {
      const result = sanitizeCalculateOptions({
        apiName: 'test-api',
        apiVersion: 'api-version',
        filter: ['env=prod'],
        lookbackWindow: '1h',
        qualityGate: 'quality-gate',
        serviceName: 'test-service',
        url: 'url',
      });

      expect(result).toEqual({
        ...sanitizedOptions,
        attributeFilters: { env: 'prod' },
        lookbackWindow: '1h',
      });
    });

    it.each(incompleteApiInformation)('should exit with code 3 if any property is missing: %s', (options: Partial<CliOptions>) => {
      // @ts-expect-error TS2345: Argument of type Partial<CliOptions> is not assignable to parameter of type CliOptions
      expect(() => sanitizeCalculateOptions(options)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenNthCalledWith(
        1,
        expect.stringContaining('❌  Either define a config file or all of these calculation parameters:'),
      );
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\t- --service-name'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\t- --api-name'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(4, expect.stringContaining('\t- --api-version'));

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no URL is provided', () => {
      // test is being done with explicit configuration, but validation applies to any configuration
      expect(() =>
        sanitizeCalculateOptions({
          apiName: 'test-api',
          apiVersion: 'test-version',
          qualityGate: 'quality-gate',
          serviceName: 'test-service',
        } as CliOptions),
      ).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌  Snow-White base URL must be defined in the configuration.'),
      );

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no Quality-Gate is provided', () => {
      // test is being done with explicit configuration, but validation applies to any configuration
      expect(() =>
        sanitizeCalculateOptions({
          apiName: 'test-api',
          apiVersion: 'test-version',
          serviceName: 'test-service',
          url: 'url',
        } as CliOptions),
      ).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  Quality-Gate name must be defined in the configuration.'));

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });
  });

  describe('OpenAPI glob pattern configuration', () => {
    const VALID_YAML = `
openapi: 3.1.0
info:
  title: My Test API
  version: 1.2.3
  x-service-name: my-service
`.trim();

    it('scans files and builds apiInformation when only --open-api-specs is provided', () => {
      (scanGlob as any).mockReturnValue(['services/my-api/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);

      const result = sanitizeCalculateOptions({
        openApiSpecs: 'services/**/openapi.yaml',
        qualityGate: 'basic-coverage',
        url: 'http://localhost:9000',
      } as CliOptions);

      expect(result.apiInformation).toEqual([{ apiName: 'My Test API', apiVersion: '1.2.3', serviceName: 'my-service' }]);
    });

    it('uses custom JSON paths when provided', () => {
      const customYaml = `
metadata:
  name: Custom API
  release: 2.0.0
  owner: custom-service
`.trim();
      (scanGlob as any).mockReturnValue(['custom.yaml']);
      (readFileSync as any).mockReturnValue(customYaml);

      const result = sanitizeCalculateOptions({
        apiNamePath: 'metadata.name',
        apiVersionPath: 'metadata.release',
        openApiSpecs: '*.yaml',
        qualityGate: 'basic-coverage',
        serviceNamePath: 'metadata.owner',
        url: 'http://localhost:9000',
      } as CliOptions);

      expect(result.apiInformation).toEqual([{ apiName: 'Custom API', apiVersion: '2.0.0', serviceName: 'custom-service' }]);
    });

    it('builds apiInformation from multiple matched files', () => {
      const yaml2 = `
openapi: 3.1.0
info:
  title: Second API
  version: 2.0.0
  x-service-name: second-service
`.trim();
      (scanGlob as any).mockReturnValue(['svc-a/openapi.yaml', 'svc-b/openapi.yaml']);
      (readFileSync as any).mockReturnValueOnce(VALID_YAML).mockReturnValueOnce(yaml2);

      const result = sanitizeCalculateOptions({
        openApiSpecs: 'svc-*/openapi.yaml',
        qualityGate: 'basic-coverage',
        url: 'http://localhost:9000',
      } as CliOptions);

      expect(result.apiInformation).toHaveLength(2);
      expect(result.apiInformation).toContainEqual({ apiName: 'My Test API', apiVersion: '1.2.3', serviceName: 'my-service' });
      expect(result.apiInformation).toContainEqual({ apiName: 'Second API', apiVersion: '2.0.0', serviceName: 'second-service' });
    });

    it('warns and returns empty array when no files match the pattern', () => {
      (scanGlob as any).mockReturnValue([]);

      expect(() =>
        sanitizeCalculateOptions({
          openApiSpecs: 'services/**/openapi.yaml',
          qualityGate: 'basic-coverage',
          url: 'http://localhost:9000',
        } as CliOptions),
      ).toThrowError('Process exited with code 3');

      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  No files matched the pattern: services/**/openapi.yaml'));
    });

    it('exits with code 3 when a file is missing required metadata fields', () => {
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue('openapi: 3.1.0');

      expect(() =>
        sanitizeCalculateOptions({
          openApiSpecs: '*.yaml',
          qualityGate: 'basic-coverage',
          url: 'http://localhost:9000',
        } as CliOptions),
      ).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  openapi.yaml: Missing required metadata fields.'));
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('ignores --open-api-specs and warns when config file already has apiInformation', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiInformation: [{ apiName: 'existing-api', apiVersion: '1.0.0', serviceName: 'existing-service' }],
        qualityGate: 'basic-coverage',
        url: 'http://localhost:9000',
      });

      const result = sanitizeCalculateOptions({
        configFile: 'snow-white.json',
        openApiSpecs: 'services/**/openapi.yaml',
      } as CliOptions);

      expect(scanGlob).not.toHaveBeenCalled();
      expect(result.apiInformation).toEqual([{ apiName: 'existing-api', apiVersion: '1.0.0', serviceName: 'existing-service' }]);
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  --open-api-specs is ignored because apiInformation is already defined in the configuration.'),
      );
    });

    it('uses glob when config file is present but has no apiInformation', () => {
      (resolveConfig as any).mockReturnValueOnce({ qualityGate: 'basic-coverage', url: 'http://localhost:9000' });
      (scanGlob as any).mockReturnValue(['services/my-api/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);

      const result = sanitizeCalculateOptions({
        configFile: 'snow-white.json',
        openApiSpecs: 'services/**/openapi.yaml',
      } as CliOptions);

      expect(result.apiInformation).toEqual([{ apiName: 'My Test API', apiVersion: '1.2.3', serviceName: 'my-service' }]);
      expect(mockConsoleWarn).not.toHaveBeenCalledWith(expect.stringContaining('--open-api-specs is ignored'));
    });
  });

  describe('async flag', () => {
    it('should respect custom async flag value', () => {
      const async = true;

      expect(
        sanitizeCalculateOptions({
          apiName: 'test-api',
          apiVersion: 'api-version',
          async,
          qualityGate: 'quality-gate',
          serviceName: 'test-service',
          url: 'url',
        }),
      ).toEqual({
        ...sanitizedOptions,
        async,
      });
    });
  });
});

describe('sanitizeUploadPrereleasesOptions', () => {
  const BASE_URL = 'http://localhost:8080';

  afterAll(() => {
    jest.restoreAllMocks();
  });

  beforeEach(() => {
    mockConsoleError.mockReset();
    mockConsoleWarn.mockReset();

    // @ts-expect-error TS2339: Property mockClear does not exist on type
    exit.mockClear();
    // @ts-expect-error TS2339: Property mockReset does not exist on type
    (resolveConfig as any).mockReset();
  });

  describe('URL resolution', () => {
    it('should use CLI url directly without consulting the config file', () => {
      const result = sanitizeUploadPrereleasesOptions({ prereleaseSpecs: '*.yaml', url: BASE_URL });

      expect(resolveConfig).not.toHaveBeenCalled();
      expect(result.url).toBe(BASE_URL);
    });

    it('should read URL from config file when --url is not provided', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      const result = sanitizeUploadPrereleasesOptions({ prereleaseSpecs: '*.yaml' });

      expect(resolveConfig).toHaveBeenCalledWith(undefined);
      expect(result.url).toBe(BASE_URL);
    });

    it('should pass --config-file path to resolveConfig', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      sanitizeUploadPrereleasesOptions({ configFile: '/path/to/config.json', prereleaseSpecs: '*.yaml' });

      expect(resolveConfig).toHaveBeenCalledWith('/path/to/config.json');
    });

    it('should exit with code 3 when URL is absent from both CLI and config file', () => {
      (resolveConfig as any).mockReturnValueOnce({});

      expect(() => sanitizeUploadPrereleasesOptions({ prereleaseSpecs: '*.yaml' })).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌  Snow-White base URL must be defined via --url or in the configuration file.'),
      );
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should warn when CLI url overrides config file url', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: 'http://config-url.com' });

      const result = sanitizeUploadPrereleasesOptions({
        configFile: 'config.json',
        prereleaseSpecs: '*.yaml',
        url: 'http://cli-url.com',
      });

      expect(result.url).toBe('http://cli-url.com');
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  CLI parameter --url overrides config file value: "http://config-url.com" → "http://cli-url.com"'),
      );
    });

    it('should not warn when CLI url matches config file url', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      const result = sanitizeUploadPrereleasesOptions({ configFile: 'config.json', prereleaseSpecs: '*.yaml', url: BASE_URL });

      expect(result.url).toBe(BASE_URL);
      expect(mockConsoleWarn).not.toHaveBeenCalled();
    });
  });

  describe('path parameter resolution', () => {
    it('should use hardcoded defaults when no CLI or config file values are provided', () => {
      (resolveConfig as any).mockReturnValueOnce({ url: BASE_URL });

      const result = sanitizeUploadPrereleasesOptions({ prereleaseSpecs: '*.yaml' });

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

      const result = sanitizeUploadPrereleasesOptions({ prereleaseSpecs: '*.yaml' });

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
        apiVersionPath: 'cli.version',
        configFile: 'config.json',
        prereleaseSpecs: '*.yaml',
        serviceNamePath: 'cli.service',
      });

      expect(result.apiNamePath).toBe('cli.name');
      expect(result.apiVersionPath).toBe('cli.version');
      expect(result.serviceNamePath).toBe('cli.service');
    });

    it('should warn when CLI api-name-path overrides config file value', () => {
      (resolveConfig as any).mockReturnValueOnce({ apiNamePath: 'config.name', url: BASE_URL });

      sanitizeUploadPrereleasesOptions({ apiNamePath: 'cli.name', configFile: 'config.json', prereleaseSpecs: '*.yaml' });

      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  CLI parameter --api-name-path overrides config file value: "config.name" → "cli.name"'),
      );
    });

    it('should warn when CLI api-version-path overrides config file value', () => {
      (resolveConfig as any).mockReturnValueOnce({ apiVersionPath: 'config.version', url: BASE_URL });

      sanitizeUploadPrereleasesOptions({ apiVersionPath: 'cli.version', configFile: 'config.json', prereleaseSpecs: '*.yaml' });

      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  CLI parameter --api-version-path overrides config file value: "config.version" → "cli.version"'),
      );
    });

    it('should warn when CLI service-name-path overrides config file value', () => {
      (resolveConfig as any).mockReturnValueOnce({ serviceNamePath: 'config.service', url: BASE_URL });

      sanitizeUploadPrereleasesOptions({ configFile: 'config.json', prereleaseSpecs: '*.yaml', serviceNamePath: 'cli.service' });

      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  CLI parameter --service-name-path overrides config file value: "config.service" → "cli.service"'),
      );
    });
  });

  describe('option passthrough', () => {
    it('should pass through globPattern and ignoreExisting', () => {
      const result = sanitizeUploadPrereleasesOptions({
        ignoreExisting: true,
        prereleaseSpecs: 'services/**/openapi.yaml',
        url: BASE_URL,
      });

      expect(result.globPattern).toBe('services/**/openapi.yaml');
      expect(result.ignoreExisting).toBe(true);
    });

    it('should default ignoreExisting to false when not provided', () => {
      const result = sanitizeUploadPrereleasesOptions({ prereleaseSpecs: '*.yaml', url: BASE_URL });

      expect(result.ignoreExisting).toBe(false);
    });
  });
});
