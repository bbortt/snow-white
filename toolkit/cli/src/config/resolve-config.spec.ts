/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { existsSync } from 'node:fs';
import { exit } from 'node:process';

import { afterAll, beforeEach, describe, expect, it, jest, mock, spyOn } from 'bun:test';
import type { CosmiconfigResult } from 'cosmiconfig';

import { CONFIG_FILE_NOT_FOUND, FAILED_LOADING_CONFIG_FILE } from '../common/exit-codes';
import type { ConfigExplorer, ConfigResolver } from './resolve-config';
import { resolveConfigInternal } from './resolve-config';

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:fs', () => ({
  existsSync: mock(),
}));

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('node:process', () => ({
  exit: mock().mockImplementation((code: number) => {
    throw new Error(`Process exited with code ${code}`);
  }),
}));

const createMockExplorer = (loadResult: any = null, shouldThrow = false): ConfigExplorer => ({
  load: (filepath: string) => {
    if (shouldThrow) {
      throw new Error(`Config file not found: ${filepath}`);
    }
    return loadResult;
  },
  search: mock(),
});

const createMockResolver = (explorer: ConfigExplorer): ConfigResolver => ({
  createExplorer: () => explorer,
});

const mockConsoleLog = spyOn(console, 'log').mockImplementation(() => {});
const mockConsoleError = spyOn(console, 'error').mockImplementation(() => {});

describe('resolveConfig', () => {
  beforeEach(() => {
    mockConsoleLog.mockReset();
    mockConsoleError.mockReset();
  });

  afterAll(() => {
    jest.restoreAllMocks();
  });

  it('should create explorer with correct module name', () => {
    const mockExplorer = createMockExplorer({});
    const mockResolver = createMockResolver(mockExplorer);
    const createExplorerSpy = spyOn(mockResolver, 'createExplorer');

    const filepath = '/test/.snow-whiterc';
    expect(() => resolveConfigInternal(filepath, mockResolver, 'test-module')).toThrowError('Process exited with code 1');

    expect(createExplorerSpy).toHaveBeenCalledWith('test-module');

    expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`⚙️ Configuration file '${filepath}' does not exist`));
    expect(mockConsoleLog).not.toHaveBeenCalled();
  });

  it('should use default module name when not provided', () => {
    const mockExplorer = createMockExplorer({});
    const mockResolver = createMockResolver(mockExplorer);
    const createExplorerSpy = spyOn(mockResolver, 'createExplorer');

    const filepath = '/test/.snow-whiterc';
    expect(() => resolveConfigInternal(filepath, mockResolver)).toThrowError('Process exited with code 1');

    expect(createExplorerSpy).toHaveBeenCalledWith('snow-white');

    expect(mockConsoleError).toHaveBeenCalledWith(expect.stringContaining(`⚙️ Configuration file '${filepath}' does not exist`));
    expect(mockConsoleLog).not.toHaveBeenCalled();
  });

  const expectConfigLoadedSuccessfully = (
    mockResolver: ConfigResolver,
    mockConfig: {
      config: { foo: string };
      filepath: string;
    },
    mockExplorer: ConfigExplorer,
    filepath: string,
    loadSpy: jest.Mock<(filepath: string) => CosmiconfigResult>,
    filepathToResolve?: string,
  ): void => {
    const result = resolveConfigInternal(filepathToResolve, mockResolver);
    expect(result).toEqual(mockConfig.config);

    if (!filepathToResolve) {
      expect(mockExplorer.search).toHaveBeenCalled();
    }

    expect(existsSync).toHaveBeenCalledWith(filepath);

    expect(mockConsoleError).not.toHaveBeenCalled();
    expect(mockConsoleLog).toHaveBeenCalledWith(`⚙️ Loading configuration file: ${filepath}`);
    expect(loadSpy).toHaveBeenCalledWith(filepath);
  };

  it('should load config from specified filepath', () => {
    const filepath = '/path/to/.snow-whiterc';
    const mockConfig = { config: { foo: 'bar' }, filepath };
    const mockExplorer = createMockExplorer(mockConfig);

    // @ts-expect-error TS2339: Property mockReturnValue does not exist on type (path: PathLike) => boolean
    existsSync.mockReturnValue(true);

    const mockResolver = createMockResolver(mockExplorer);
    const loadSpy = spyOn(mockExplorer, 'load');

    expectConfigLoadedSuccessfully(mockResolver, mockConfig, mockExplorer, filepath, loadSpy, filepath);
  });

  it('should search for config config when no filepath specified', () => {
    const filepath = '/path/to/.snow-whiterc';
    const mockConfig = { config: { foo: 'bar' }, filepath };

    const mockExplorer = createMockExplorer(mockConfig);
    // @ts-expect-error TS2339: Property mockReturnValue does not exist on type () => CosmiconfigResult
    mockExplorer.search.mockReturnValue({ filepath });

    // @ts-expect-error TS2339: Property mockReturnValue does not exist on type (path: PathLike) => boolean
    existsSync.mockReturnValue(true);

    const mockResolver = createMockResolver(mockExplorer);
    const loadSpy = spyOn(mockExplorer, 'load');

    expectConfigLoadedSuccessfully(mockResolver, mockConfig, mockExplorer, filepath, loadSpy);
  });

  it('should log error and exit when no filepath found', () => {
    const mockExplorer = createMockExplorer(null);
    const mockResolver = createMockResolver(mockExplorer);

    expect(() => resolveConfigInternal(undefined, mockResolver)).toThrowError('Process exited with code 1');

    expect(mockExplorer.search).toHaveBeenCalled();

    expect(mockConsoleError).toHaveBeenCalledWith(
      expect.stringContaining(`⚙️ Failed to find configuration file - try with '--configFile <path-to-your-config-file>'`),
    );
    expect(exit).toHaveBeenCalledWith(CONFIG_FILE_NOT_FOUND);
  });

  it('should log error and exit when config loading fails', () => {
    const mockExplorer = createMockExplorer(null, true);
    const mockResolver = createMockResolver(mockExplorer);

    const filepath = '/nonexistent/.snow-whiterc';
    expect(() => resolveConfigInternal(filepath, mockResolver)).toThrowError('Process exited with code 2');

    expect(mockConsoleError).toHaveBeenCalledWith(
      expect.stringContaining(`⚙️ Failed to load configuration file: Config file not found: ${filepath}`),
    );
    expect(exit).toHaveBeenCalledWith(FAILED_LOADING_CONFIG_FILE);
  });
});
