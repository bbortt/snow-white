/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it } from 'bun:test';

import { sanitizeConfiguration } from './sanitize-configuration';

describe('sanitize configuration', () => {
  const expectCombinationError = (options: CalculateOptions): void => {
    expect(sanitizeConfiguration(options)).resolves.toBeUndefined();

    expect(mockConsoleError).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('❌ You cannot use a config file in combination with these calculation parameters:'),
    );
    expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\t- qualityGate'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\t- serviceName'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(4, expect.stringContaining('\t- apiName'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(5, expect.stringContaining('\t- apiVersion'));

    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  };

  it('should throw error if config and qualityGate is configured', () => {
    const options: CalculateOptions = {
      config: 'config',
      qualityGate: 'quality-gate',
    };

    expectCombinationError(options);
  });

  it('should throw error if config and serviceName is configured', () => {
    const options: CalculateOptions = {
      config: 'config',
      serviceName: 'test-service',
    };

    expectCombinationError(options);
  });

  it('should throw error if config and apiName is configured', () => {
    const options: CalculateOptions = {
      config: 'config',
      apiName: 'test-api',
    };

    expectCombinationError(options);
  });

  it('should throw error if config and apiVersion is configured', () => {
    const options: CalculateOptions = {
      config: 'config',
      apiVersion: 'test-version',
    };

    expectCombinationError(options);
  });

  it('resolves config from path', () => {
    const options: CalculateOptions = {
      config: 'config',
    };

    expect(sanitizeConfiguration(options)).resolves.toBeUndefined();

    expect(resolveSnowWhiteConfig).toHaveBeenCalledWith('config');
  });

  const expectMissingConfigurationParameters = (options: CalculateOptions): void => {
    expect(sanitizeConfiguration(options)).resolves.toBeUndefined();

    expect(mockConsoleError).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining('❌ Either define a config file or all of these calculation parameters:'),
    );
    expect(mockConsoleError).toHaveBeenNthCalledWith(2, expect.stringContaining('\t- qualityGate'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(3, expect.stringContaining('\t- serviceName'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(4, expect.stringContaining('\t- apiName'));
    expect(mockConsoleError).toHaveBeenNthCalledWith(5, expect.stringContaining('\t- apiVersion'));

    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  };

  it('should throw error if qualityGate is undefined in direct configuration', () => {
    const options: CalculateOptions = {
      qualityGate: 'quality-gate',
    };

    expectMissingConfigurationParameters(options);
  });

  it('should throw error if serviceName is undefined in direct configuration', () => {
    const options: CalculateOptions = {
      serviceName: 'test-service',
    };

    expectMissingConfigurationParameters(options);
  });

  it('should throw error if apiName is undefined in direct configuration', () => {
    const options: CalculateOptions = {
      apiName: 'test-api',
    };

    expectMissingConfigurationParameters(options);
  });

  it('should throw error if apiVersion is undefined in direct configuration', () => {
    const options: CalculateOptions = {
      apiVersion: 'test-version',
    };

    expectMissingConfigurationParameters(options);
  });
});
