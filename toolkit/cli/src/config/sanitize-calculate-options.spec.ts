/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';
import { readFileSync } from 'node:fs';
import { exit } from 'node:process';

import type { CliOptions } from './cli-options';
import type { CalculateOptions } from './sanitized-options';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { scanGlob } from '../common/glob';
import { resolveConfig } from './resolve-config';
import { sanitizeCalculateOptions } from './sanitize-calculate-options';

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

describe('sanitizeCalculateOptions', () => {
  let consoleWarnSpy: ReturnType<typeof spyOn>;
  let consoleErrorSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleErrorSpy = spyOn(console, 'error').mockImplementation(() => {});
    consoleWarnSpy = spyOn(console, 'warn').mockImplementation(() => {});

    // @ts-expect-error TS2339: Property mockClear does not exist on type
    exit.mockClear();
    (resolveConfig as any).mockReset();
    (scanGlob as any).mockReset();
    (readFileSync as any).mockReset();
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
    consoleWarnSpy.mockRestore();
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
    expect(() => sanitizeCalculateOptions(options)).toThrowError('Process exited with code 3');

    expect(consoleErrorSpy).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('❌ You cannot use options from multiple configuration groups together.'),
    );
    expect(consoleErrorSpy).toHaveBeenNthCalledWith(2, expect.stringContaining('\tGroup 1: serviceName, apiName, apiVersion'));
    expect(consoleErrorSpy).toHaveBeenNthCalledWith(3, expect.stringContaining('\tGroup 2: configFile'));

    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  });

  it.each([
    // apiSpecs combined with any exact config parameter
    {
      apiSpecs: 'some-glob-pattern',
      serviceName: 'test-service',
    },
    {
      apiName: 'test-api',
      apiSpecs: 'some-glob-pattern',
    },
    {
      apiSpecs: 'some-glob-pattern',
      apiVersion: 'test-version',
    },
  ])('should exit with code 3 when apiSpecs is combined with exact config params: %s', (options: Partial<CliOptions>) => {
    expect(() => sanitizeCalculateOptions(options)).toThrowError('Process exited with code 3');

    expect(consoleErrorSpy).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('❌ You cannot use options from multiple configuration groups together.'),
    );
    expect(consoleErrorSpy).toHaveBeenNthCalledWith(2, expect.stringContaining('\tGroup 1: serviceName, apiName, apiVersion'));
    expect(consoleErrorSpy).toHaveBeenNthCalledWith(3, expect.stringContaining('\tGroup 2: apiSpecs'));

    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  });

  describe('file-based configuration', () => {
    it('resolves config from exact path if specified', () => {
      (resolveConfig as any).mockReturnValueOnce(sanitizedOptions);

      expect(sanitizeCalculateOptions({})).toEqual(sanitizedOptions);

      expect(resolveConfig).toHaveBeenCalled();
    });

    const expectRecursiveOrEmptyConfiguration = () => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        expect.stringContaining('❌ Configuration file may not contain recursive references or be empty.'),
      );
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    };

    it('should exit with code 3 if file is empty', () => {
      (resolveConfig as any).mockReturnValueOnce({});

      expect(() => sanitizeCalculateOptions({})).toThrowError('Process exited with code 3');

      expect(resolveConfig).toHaveBeenCalled();

      expectRecursiveOrEmptyConfiguration();
    });

    it('should exit with code 3 if file contains recursive configuration', () => {
      const configFile = 'configFile';

      (resolveConfig as any).mockReturnValueOnce({ configFile });

      expect(() => sanitizeCalculateOptions({ configFile })).toThrowError('Process exited with code 3');

      expect(resolveConfig).toHaveBeenCalledWith(configFile);

      expectRecursiveOrEmptyConfiguration();
    });

    it.each(incompleteApiInformation)('should exit with code 3 if any property is missing: %s', (options: Partial<CliOptions>) => {
      (resolveConfig as any).mockReturnValueOnce({ apiInformation: [options], qualityGate: 'quality-gate', url: 'url' });

      expect(() => sanitizeCalculateOptions({ configFile: 'configFile' })).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenCalledWith(
        expect.stringContaining('❌ Each API information must contain serviceName, apiName, and apiVersion.'),
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

      expect(() => sanitizeCalculateOptions({ configFile: 'configFile' })).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ Snow-White base URL must be defined in the configuration.'));

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no Quality-Gate is provided', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiName: 'test-api',
        apiVersion: 'test-version',
        serviceName: 'test-service',
        url: 'url',
      });

      expect(() => sanitizeCalculateOptions({ configFile: 'configFile' })).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ Quality-Gate name must be defined in the configuration.'));

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

      const result = sanitizeCalculateOptions({ url: 'http://cli-url.com' });

      expect(result.url).toBe('http://cli-url.com');
      expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining('⚠️ CLI parameter --url overrides config file value'));
    });

    it('should override qualityGate from config file with CLI parameter', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        qualityGate: 'file-gate',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ qualityGate: 'cli-gate' });

      expect(result.qualityGate).toBe('cli-gate');
      expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining('⚠️ CLI parameter --quality-gate overrides config file value'));
    });

    it('should override lookbackWindow from config file with CLI parameter', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        lookbackWindow: '1h',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ lookbackWindow: '24h' });

      expect(result.lookbackWindow).toBe('24h');
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ CLI parameter --lookback-window overrides config file value'),
      );
    });

    it('should override attributeFilters from config file with CLI filters', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        attributeFilters: { environment: 'production' },
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ filter: ['region=us-west-1'] });

      expect(result.attributeFilters).toEqual({ region: 'us-west-1' });
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ CLI parameter --filter overrides config file attributeFilters'),
      );
    });

    it('should not warn when CLI parameter matches config file value', () => {
      const fileConfig: CalculateOptions = {
        ...sanitizedOptions,
        url: 'http://same-url.com',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeCalculateOptions({ url: 'http://same-url.com' });

      expect(result.url).toBe('http://same-url.com');
      expect(consoleWarnSpy).not.toHaveBeenCalled();
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
      expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining('⚠️ Ignoring invalid filter format: "invalid-filter"'));
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
      expect(consoleWarnSpy).not.toHaveBeenCalled();
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
      expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining('⚠️ Lookback window "invalid" may not be in expected format'));
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

      expect(consoleErrorSpy).not.toHaveBeenCalled();

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
      expect(() => sanitizeCalculateOptions(options)).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenNthCalledWith(
        1,
        expect.stringContaining('❌ Either define a config file or all of these calculation parameters:'),
      );
      expect(consoleErrorSpy).toHaveBeenNthCalledWith(2, expect.stringContaining('\t- --service-name'));
      expect(consoleErrorSpy).toHaveBeenNthCalledWith(3, expect.stringContaining('\t- --api-name'));
      expect(consoleErrorSpy).toHaveBeenNthCalledWith(4, expect.stringContaining('\t- --api-version'));

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
        }),
      ).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ Snow-White base URL must be defined in the configuration.'));

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
        }),
      ).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ Quality-Gate name must be defined in the configuration.'));

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
        apiSpecs: 'services/**/openapi.yaml',
        qualityGate: 'basic-coverage',
        url: 'http://localhost:9000',
      });

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
        apiSpecs: '*.yaml',
        apiVersionPath: 'metadata.release',
        qualityGate: 'basic-coverage',
        serviceNamePath: 'metadata.owner',
        url: 'http://localhost:9000',
      });

      expect(result.apiInformation).toEqual([{ apiName: 'Custom API', apiVersion: '2.0.0', serviceName: 'custom-service' }]);
    });

    it('uses custom JSON paths from file when provided', () => {
      const customYaml = `
metadata:
  name: Custom API
  release: 2.0.0
  owner: custom-service
`.trim();
      (scanGlob as any).mockReturnValue(['custom.yaml']);
      (readFileSync as any).mockReturnValue(customYaml);

      (resolveConfig as any).mockReturnValueOnce({
        apiNamePath: 'metadata.name',
        apiSpecs: '*.yaml',
        apiVersionPath: 'metadata.release',
        qualityGate: 'basic-coverage',
        serviceNamePath: 'metadata.owner',
        url: 'http://localhost:9000',
      });

      const result = sanitizeCalculateOptions({});

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
        apiSpecs: 'svc-*/openapi.yaml',
        qualityGate: 'basic-coverage',
        url: 'http://localhost:9000',
      });

      expect(result.apiInformation).toHaveLength(2);
      expect(result.apiInformation).toContainEqual({ apiName: 'My Test API', apiVersion: '1.2.3', serviceName: 'my-service' });
      expect(result.apiInformation).toContainEqual({ apiName: 'Second API', apiVersion: '2.0.0', serviceName: 'second-service' });
    });

    it('warns and returns empty array when no files match the pattern', () => {
      (scanGlob as any).mockReturnValue([]);

      expect(() =>
        sanitizeCalculateOptions({
          apiSpecs: 'services/**/openapi.yaml',
          qualityGate: 'basic-coverage',
          url: 'http://localhost:9000',
        }),
      ).toThrowError('Process exited with code 3');

      expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining('⚠️ No files matched the pattern: services/**/openapi.yaml'));
    });

    it('exits with code 3 when a file is missing required metadata fields', () => {
      (scanGlob as any).mockReturnValue(['openapi.yaml']);
      (readFileSync as any).mockReturnValue('openapi: 3.1.0');

      expect(() =>
        sanitizeCalculateOptions({
          apiSpecs: '*.yaml',
          qualityGate: 'basic-coverage',
          url: 'http://localhost:9000',
        }),
      ).toThrowError('Process exited with code 3');

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ openapi.yaml: Missing required metadata fields.'));
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('ignores --api-specs and warns when config file already has apiInformation', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiInformation: [{ apiName: 'existing-api', apiVersion: '1.0.0', serviceName: 'existing-service' }],
        qualityGate: 'basic-coverage',
        url: 'http://localhost:9000',
      });

      const result = sanitizeCalculateOptions({
        apiSpecs: 'services/**/openapi.yaml',
        configFile: 'snow-white.json',
      });

      expect(scanGlob).not.toHaveBeenCalled();
      expect(result.apiInformation).toEqual([{ apiName: 'existing-api', apiVersion: '1.0.0', serviceName: 'existing-service' }]);
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ --api-specs is ignored because apiInformation is already defined in the configuration.'),
      );
    });

    it('uses glob when config file is present but has no apiInformation', () => {
      (resolveConfig as any).mockReturnValueOnce({ qualityGate: 'basic-coverage', url: 'http://localhost:9000' });
      (scanGlob as any).mockReturnValue(['services/my-api/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);

      const result = sanitizeCalculateOptions({
        apiSpecs: 'services/**/openapi.yaml',
        configFile: 'snow-white.json',
      });

      expect(result.apiInformation).toEqual([{ apiName: 'My Test API', apiVersion: '1.2.3', serviceName: 'my-service' }]);
      expect(consoleWarnSpy).not.toHaveBeenCalledWith(expect.stringContaining('--api-specs is ignored'));
    });

    it('reads apiSpecs from config file when not provided via CLI', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiSpecs: 'services/**/openapi.yaml',
        qualityGate: 'basic-coverage',
        url: 'http://localhost:9000',
      });
      (scanGlob as any).mockReturnValue(['services/my-api/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);

      const result = sanitizeCalculateOptions({ configFile: 'snow-white.json' });

      expect(result.apiInformation).toEqual([{ apiName: 'My Test API', apiVersion: '1.2.3', serviceName: 'my-service' }]);
    });

    it('warns when CLI --api-specs overrides config file apiSpecs', () => {
      (resolveConfig as any).mockReturnValueOnce({
        apiSpecs: 'old/**/openapi.yaml',
        qualityGate: 'basic-coverage',
        url: 'http://localhost:9000',
      });
      (scanGlob as any).mockReturnValue(['services/my-api/openapi.yaml']);
      (readFileSync as any).mockReturnValue(VALID_YAML);

      sanitizeCalculateOptions({
        apiSpecs: 'services/**/openapi.yaml',
        configFile: 'snow-white.json',
      });

      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining(
          '⚠️ CLI parameter --api-specs overrides config file value: "old/**/openapi.yaml" → "services/**/openapi.yaml"',
        ),
      );
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
