/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';
import { exit } from 'node:process';

import type { CliOptions } from './cli-options';
import type { SanitizedOptions } from './sanitized-options';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { resolveConfig } from './resolve-config';
import { sanitizeConfiguration } from './sanitize-configuration';

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

const sanitizedOptions: SanitizedOptions = {
  apiInformation: [
    {
      apiName: 'test-api',
      apiVersion: 'api-version',
      serviceName: 'test-service',
    },
  ],
  qualityGate: 'quality-gate',
  url: 'url',
};

describe('sanitizeConfiguration', () => {
  beforeEach(() => {
    mockConsoleError.mockReset();
    mockConsoleWarn.mockReset();

    // @ts-expect-error TS2339: Property mockClear does not exist on type
    exit.mockClear();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  it.each([
    // property configFile with any other property
    {
      configFile: 'config',
      openApiSpecs: 'some-glob-pattern',
    },
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
    // property openApiSpecs with any other property
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
  ])('should exit with code 3 when combination of parameters is invalid: %s', (options: Partial<CliOptions>) => {
    // @ts-expect-error TS2345: Argument of type Partial<CliOptions> is not assignable to parameter of type CliOptions
    expect(() => sanitizeConfiguration(options)).toThrowError('Process exited with code 3');

    expect(mockConsoleError).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('❌  You cannot use options from multiple configuration groups together.'),
    );
    expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\tGroup 1: serviceName, apiName, apiVersion'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\tGroup 2: configFile'));

    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  });

  describe('file-based configuration', () => {
    it('resolves config from exact path if specified', () => {
      (resolveConfig as any).mockReturnValueOnce(sanitizedOptions);

      expect(sanitizeConfiguration({} as CliOptions)).toEqual(sanitizedOptions);

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

      expect(() => sanitizeConfiguration({} as CliOptions)).toThrowError('Process exited with code 3');

      expect(resolveConfig).toHaveBeenCalled();

      expectRecursiveOrEmptyConfiguration();
    });

    it('should exit with code 3 if file contains recursive configuration', () => {
      const configFile = 'configFile';

      (resolveConfig as any).mockReturnValueOnce({ configFile });

      expect(() => sanitizeConfiguration({ configFile } as CliOptions)).toThrowError('Process exited with code 3');

      expect(resolveConfig).toHaveBeenCalledWith(configFile);

      expectRecursiveOrEmptyConfiguration();
    });

    it.each(incompleteApiInformation)('should exit with code 3 if any property is missing: %s', (options: Partial<CliOptions>) => {
      (resolveConfig as any).mockReturnValueOnce({ apiInformation: [options], qualityGate: 'quality-gate', url: 'url' });

      expect(() => sanitizeConfiguration({ configFile: 'configFile' } as CliOptions)).toThrowError('Process exited with code 3');

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

      expect(() => sanitizeConfiguration({ configFile: 'configFile' } as CliOptions)).toThrowError('Process exited with code 3');

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

      expect(() => sanitizeConfiguration({ configFile: 'configFile' } as CliOptions)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌  Quality-Gate name must be defined in the configuration.'));

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });
  });

  describe('CLI parameter precedence over config file', () => {
    it('should override URL from config file with CLI parameter', () => {
      const fileConfig: SanitizedOptions = {
        ...sanitizedOptions,
        url: 'http://config-file-url.com',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeConfiguration({ url: 'http://cli-url.com' } as CliOptions);

      expect(result.url).toBe('http://cli-url.com');
      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  CLI parameter --url overrides config file value'));
    });

    it('should override qualityGate from config file with CLI parameter', () => {
      const fileConfig: SanitizedOptions = {
        ...sanitizedOptions,
        qualityGate: 'file-gate',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeConfiguration({ qualityGate: 'cli-gate' } as CliOptions);

      expect(result.qualityGate).toBe('cli-gate');
      expect(mockConsoleWarn).toHaveBeenCalledWith(expect.stringContaining('⚠️  CLI parameter --qualityGate overrides config file value'));
    });

    it('should override lookbackWindow from config file with CLI parameter', () => {
      const fileConfig: SanitizedOptions = {
        ...sanitizedOptions,
        lookbackWindow: '1h',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeConfiguration({ lookbackWindow: '24h' } as CliOptions);

      expect(result.lookbackWindow).toBe('24h');
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  CLI parameter --lookbackWindow overrides config file value'),
      );
    });

    it('should override attributeFilters from config file with CLI filters', () => {
      const fileConfig: SanitizedOptions = {
        ...sanitizedOptions,
        attributeFilters: { environment: 'production' },
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeConfiguration({ filter: ['region=us-west-1'] } as CliOptions);

      expect(result.attributeFilters).toEqual({ region: 'us-west-1' });
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  CLI parameter --filter overrides config file attributeFilters'),
      );
    });

    it('should not warn when CLI parameter matches config file value', () => {
      const fileConfig: SanitizedOptions = {
        ...sanitizedOptions,
        url: 'http://same-url.com',
      };
      (resolveConfig as any).mockReturnValueOnce(fileConfig);

      const result = sanitizeConfiguration({ url: 'http://same-url.com' } as CliOptions);

      expect(result.url).toBe('http://same-url.com');
      expect(mockConsoleWarn).not.toHaveBeenCalled();
    });
  });

  describe('filter parsing', () => {
    it('should parse multiple filters correctly', () => {
      const result = sanitizeConfiguration({
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
      const result = sanitizeConfiguration({
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
      const result = sanitizeConfiguration({
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
      const result = sanitizeConfiguration({
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
      const result = sanitizeConfiguration({
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
        sanitizeConfiguration({
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
      const result = sanitizeConfiguration({
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
      expect(() => sanitizeConfiguration(options)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenNthCalledWith(
        1,
        expect.stringContaining('❌  Either define a config file or all of these calculation parameters:'),
      );
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\t- serviceName'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\t- apiName'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(4, expect.stringContaining('\t- apiVersion'));

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no URL is provided', () => {
      // test is being done with explicit configuration, but validation applies to any configuration
      expect(() =>
        sanitizeConfiguration({
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
        sanitizeConfiguration({
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
    it('is not implemented yet', () => {
      const options: CliOptions = {
        openApiSpecs: 'some-glob-pattern',
      };

      expect(() => sanitizeConfiguration(options)).toThrowError('Process exited with code 0');

      expect(mockConsoleError).not.toHaveBeenCalled();
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️  OpenAPI specs are not yet implemented. Using provided options as is.'),
      );

      expect(exit).toHaveBeenCalledWith(0);
    });
  });
});
