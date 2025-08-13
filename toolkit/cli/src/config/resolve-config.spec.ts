/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { exit } from 'node:process';

import { beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';

import { CONFIG_FILE_NOT_FOUND, FAILED_LOADING_CONFIG_FILE } from '../common/exit-codes';
import type { ConfigExplorer, ConfigResolver } from './config';
import { resolveConfig } from './config';

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:process', () => ({
  exit: mock(),
}));

const createMockExplorer = (loadResult: any = null, shouldThrow = false): ConfigExplorer => ({
  load: (filepath: string) => {
    if (shouldThrow) {
      throw new Error(`Config file not found: ${filepath}`);
    }
    return loadResult;
  },
});

const createMockResolver = (explorer: ConfigExplorer): ConfigResolver => ({
  createExplorer: () => explorer,
});

const mockConsoleLog = spyOn(console, 'log').mockImplementation(() => {});
const mockConsoleError = spyOn(console, 'error').mockImplementation(() => {});

describe('config', () => {
  beforeEach(() => {
    mockConsoleLog.mockReset();
    mockConsoleError.mockReset();
  });

  describe('resolveConfig', () => {
    it('should create explorer with correct module name', () => {
      const mockExplorer = createMockExplorer({ config: {}, filepath: '/test/.snow-whiterc' });
      const mockResolver = createMockResolver(mockExplorer);
      const createExplorerSpy = spyOn(mockResolver, 'createExplorer');

      resolveConfig('/test/.snow-whiterc', mockResolver, 'test-module');

      expect(createExplorerSpy).toHaveBeenCalledWith('test-module');
    });

    it('should use default module name when not provided', () => {
      const mockExplorer = createMockExplorer({ config: {}, filepath: '/test/.snow-whiterc' });
      const mockResolver = createMockResolver(mockExplorer);
      const createExplorerSpy = spyOn(mockResolver, 'createExplorer');

      resolveConfig('/test/.snow-whiterc', mockResolver);

      expect(createExplorerSpy).toHaveBeenCalledWith('snow-white');
    });

    it('should load config from specified filepath', () => {
      const mockConfig = { config: { foo: 'bar' }, filepath: '/path/to/.snow-whiterc' };
      const mockExplorer = createMockExplorer(mockConfig);
      const mockResolver = createMockResolver(mockExplorer);
      const loadSpy = spyOn(mockExplorer, 'load');

      const result = resolveConfig('/path/to/.snow-whiterc', mockResolver);

      expect(result).toEqual(mockConfig);
      expect(mockConsoleLog).toHaveBeenCalledWith('⚙️ Loading configuration file: /path/to/.snow-whiterc');
      expect(loadSpy).toHaveBeenCalledWith('/path/to/.snow-whiterc');
    });

    it('should log error and exit when config is nonexistent', () => {
      const mockExplorer = createMockExplorer(null);
      const mockResolver = createMockResolver(mockExplorer);

      const result = resolveConfig('/nonexistent/.snow-whiterc', mockResolver);

      expect(result).toBeNull();
      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining(`⚙️ Configuration file not found at '/nonexistent/.snow-whiterc'`),
      );
      expect(exit).toHaveBeenCalledWith(CONFIG_FILE_NOT_FOUND);
    });

    it('should log error and exit when config loading fails', () => {
      const mockExplorer = createMockExplorer(null, true);
      const mockResolver = createMockResolver(mockExplorer);

      const result = resolveConfig('/nonexistent/.snow-whiterc', mockResolver);

      expect(result).toBeUndefined();
      expect(mockConsoleError).toHaveBeenCalledWith(
        expect.stringContaining(`⚙️ Failed to load configuration from '/nonexistent/.snow-whiterc'`),
        expect.any(Error),
      );
      expect(exit).toHaveBeenCalledWith(FAILED_LOADING_CONFIG_FILE);
    });

    it('should pass through the exact filepath to load method', () => {
      const testFilepath = '/custom/path/.snow-whiterc.json';
      const mockExplorer = createMockExplorer({ config: {}, filepath: testFilepath });
      const mockResolver = createMockResolver(mockExplorer);
      const loadSpy = spyOn(mockExplorer, 'load');

      resolveConfig(testFilepath, mockResolver);

      expect(loadSpy).toHaveBeenCalledWith(testFilepath);
    });
  });
});
