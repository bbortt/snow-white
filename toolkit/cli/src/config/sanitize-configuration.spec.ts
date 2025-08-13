/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { exit } from 'node:process';

import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import type { CliOptions } from './cli-options';
import { resolveConfig } from './resolve-config';
import { sanitizeConfiguration } from './sanitize-configuration';
import type { SanitizedOptions } from './sanitized-options';

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
    serviceName: 'test-service',
    apiVersion: 'test-version',
  },
  {
    serviceName: 'test-service',
    apiName: 'test-api',
  },
];

const sanitizedOptions: SanitizedOptions = {
  apiInformation: [
    {
      serviceName: 'test-service',
      apiVersion: 'api-version',
      apiName: 'test-api',
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
      configFile: 'config',
      apiName: 'test-api',
    },
    {
      configFile: 'config',
      apiVersion: 'test-version',
    },
    // property openApiSpecs with any other property
    {
      openApiSpecs: 'some-glob-pattern',
      serviceName: 'test-service',
    },
    {
      openApiSpecs: 'some-glob-pattern',
      apiName: 'test-api',
    },
    {
      openApiSpecs: 'some-glob-pattern',
      apiVersion: 'test-version',
    },
  ])('should exit with code 3 when combination of parameters is invalid: %s', (options: Partial<CliOptions>) => {
    // @ts-expect-error TS2345: Argument of type Partial<CliOptions> is not assignable to parameter of type CliOptions
    expect(() => sanitizeConfiguration(options)).toThrowError('Process exited with code 3');

    expect(mockConsoleError).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('❌ You cannot use options from multiple configuration groups together.'),
    );
    expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\tGroup 1: serviceName, apiName, apiVersion'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\tGroup 2: configFile'));

    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  });

  describe('file-based configuration', () => {
    it('resolves config from exact path if specified', () => {
      (resolveConfig as any).mockReturnValueOnce(sanitizedOptions);

      expect(sanitizeConfiguration({} as CliOptions)).toBe(sanitizedOptions);

      expect(resolveConfig).toHaveBeenCalled();
    });

    const expectRecursiveOrEmptyConfiguration = () => {
      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌ Configuration file may not contain recursive references or be empty.'),
      );
      expect(mockConsoleWarn).not.toHaveBeenCalled();

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
        expect.stringContaining('❌ Each API information must contain serviceName, apiName, and apiVersion.'),
      );
      expect(mockConsoleWarn).not.toHaveBeenCalled();

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no URL is provided', () => {
      (resolveConfig as any).mockReturnValueOnce({
        serviceName: 'test-service',
        apiName: 'test-api',
        apiVersion: 'test-version',
        qualityGate: 'quality-gate',
      });

      expect(() => sanitizeConfiguration({ configFile: 'configFile' } as CliOptions)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌ Snow-White base URL must be defined in the configuration.'),
      );
      expect(mockConsoleWarn).not.toHaveBeenCalled();

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no Quality-Gate is provided', () => {
      (resolveConfig as any).mockReturnValueOnce({
        serviceName: 'test-service',
        apiName: 'test-api',
        apiVersion: 'test-version',
        url: 'url',
      });

      expect(() => sanitizeConfiguration({ configFile: 'configFile' } as CliOptions)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌ Quality-Gate name must be defined in the configuration.'));
      expect(mockConsoleWarn).not.toHaveBeenCalled();

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });
  });

  describe('explicit configuration', () => {
    it('should return sanitized options when all required properties are provided', () => {
      expect(
        sanitizeConfiguration({
          qualityGate: 'quality-gate',
          serviceName: 'test-service',
          apiVersion: 'api-version',
          apiName: 'test-api',
          url: 'url',
        }),
      ).toEqual(sanitizedOptions);

      expect(mockConsoleError).not.toHaveBeenCalled();
      expect(mockConsoleWarn).not.toHaveBeenCalled();

      expect(exit).not.toHaveBeenCalled();
    });

    it.each(incompleteApiInformation)('should exit with code 3 if any property is missing: %s', (options: Partial<CliOptions>) => {
      // @ts-expect-error TS2345: Argument of type Partial<CliOptions> is not assignable to parameter of type CliOptions
      expect(() => sanitizeConfiguration(options)).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenNthCalledWith(
        1,
        expect.stringContaining('❌ Either define a config file or all of these calculation parameters:'),
      );
      expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\t- serviceName'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\t- apiName'));
      expect(mockConsoleError).toHaveBeenNthCalledWith(4, expect.stringContaining('\t- apiVersion'));

      expect(mockConsoleWarn).not.toHaveBeenCalled();

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no URL is provided', () => {
      // test is being done with explicit configuration, but validation applies to any configuration
      expect(() =>
        sanitizeConfiguration({
          serviceName: 'test-service',
          apiName: 'test-api',
          apiVersion: 'test-version',
          qualityGate: 'quality-gate',
        } as CliOptions),
      ).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining('❌ Snow-White base URL must be defined in the configuration.'),
      );
      expect(mockConsoleWarn).not.toHaveBeenCalled();

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it('should exit with code 3 if no Quality-Gate is provided', () => {
      // test is being done with explicit configuration, but validation applies to any configuration
      expect(() =>
        sanitizeConfiguration({
          serviceName: 'test-service',
          apiName: 'test-api',
          apiVersion: 'test-version',
          url: 'url',
        } as CliOptions),
      ).toThrowError('Process exited with code 3');

      expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining('❌ Quality-Gate name must be defined in the configuration.'));
      expect(mockConsoleWarn).not.toHaveBeenCalled();

      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });
  });

  describe('OpenAPI glob pattern configuration', () => {
    it('is not implemented yet', () => {
      const options: CliOptions = {
        openApiSpecs: 'some-glob-pattern',
        url: 'url',
      };

      expect(() => sanitizeConfiguration(options)).toThrowError('Process exited with code 0');

      expect(mockConsoleError).not.toHaveBeenCalled();
      expect(mockConsoleWarn).toHaveBeenCalledWith(
        expect.stringContaining('⚠️ OpenAPI specs are not yet implemented. Using provided options as is.'),
      );

      expect(exit).toHaveBeenCalledWith(0);
    });
  });
});
