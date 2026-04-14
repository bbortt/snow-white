/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';

import * as logResponseErrorModule from './log-response-error.ts';

const mockIsFetchError = mock();
const mockIsResponseError = mock();

await mock.module('../../common/error-response-utils.ts', () => ({
  isFetchError: mockIsFetchError,
  isResponseError: mockIsResponseError,
}));

const { handleUploadError } = await import('./handle-upload-error.ts');

const makeMetadata = () => ({
  apiName: 'my-api',
  apiVersion: '1.0.0',
  serviceName: 'my-service',
});

const makeResponseError = (status: number) => ({
  response: { status },
});

describe('handleUploadError', () => {
  let logResponseErrorSpy: ReturnType<typeof spyOn>;

  let consoleWarnSpy: ReturnType<typeof spyOn>;
  let consoleErrorSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    logResponseErrorSpy = spyOn(logResponseErrorModule, 'logResponseError').mockImplementation(() => {});

    consoleErrorSpy = spyOn(console, 'error').mockImplementation(() => {});
    consoleWarnSpy = spyOn(console, 'warn').mockImplementation(() => {});

    mockIsFetchError.mockReset();
    mockIsResponseError.mockReset();
  });

  afterEach(() => {
    logResponseErrorSpy.mockRestore();

    consoleErrorSpy.mockRestore();
    consoleWarnSpy.mockRestore();
  });

  describe('always', () => {
    it('logs a red upload-failed message for the file', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(false);

      await handleUploadError(new Error('oops'), 'spec.yaml', makeMetadata(), false);

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('spec.yaml: Upload failed.'));
    });
  });

  describe('when the error is a response error', () => {
    describe('and status is 409 and ignoreExisting is true', () => {
      it('returns { ignored: true }', async () => {
        mockIsResponseError.mockReturnValue(true);
        const error = makeResponseError(409);

        const result = await handleUploadError(error, 'spec.yaml', makeMetadata(), true);

        expect(result).toEqual({ ignored: true });
      });

      it('logs a warning with the service/api@version string', async () => {
        mockIsResponseError.mockReturnValue(true);
        const error = makeResponseError(409);

        await handleUploadError(error, 'spec.yaml', makeMetadata(), true);

        expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining('my-service/my-api@1.0.0'));
      });

      it('does not call logResponseError', async () => {
        mockIsResponseError.mockReturnValue(true);
        const error = makeResponseError(409);

        await handleUploadError(error, 'spec.yaml', makeMetadata(), true);

        expect(logResponseErrorSpy).not.toHaveBeenCalled();
      });
    });

    describe('and status is 409 but ignoreExisting is false', () => {
      it('returns { ignored: false }', async () => {
        mockIsResponseError.mockReturnValue(true);
        const error = makeResponseError(409);

        const result = await handleUploadError(error, 'spec.yaml', makeMetadata(), false);

        expect(result).toEqual({ ignored: false });
      });

      it('calls logResponseError with the error', async () => {
        mockIsResponseError.mockReturnValue(true);
        const error = makeResponseError(409);

        await handleUploadError(error, 'spec.yaml', makeMetadata(), false);

        expect(logResponseErrorSpy).toHaveBeenCalledWith(error);
      });
    });

    describe('and status is not 409', () => {
      it('returns { ignored: false }', async () => {
        mockIsResponseError.mockReturnValue(true);
        const error = makeResponseError(500);

        const result = await handleUploadError(error, 'spec.yaml', makeMetadata(), true);

        expect(result).toEqual({ ignored: false });
      });

      it('calls logResponseError with the error', async () => {
        mockIsResponseError.mockReturnValue(true);
        const error = makeResponseError(422);

        await handleUploadError(error, 'spec.yaml', makeMetadata(), false);

        expect(logResponseErrorSpy).toHaveBeenCalledWith(error);
      });
    });
  });

  describe('when the error is a fetch error', () => {
    it('returns { ignored: false }', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(true);

      const result = await handleUploadError(new Error('fetch failed'), 'spec.yaml', makeMetadata(), false);

      expect(result).toEqual({ ignored: false });
    });

    it('logs "No response received from server."', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(true);

      await handleUploadError(new Error('fetch failed'), 'spec.yaml', makeMetadata(), false);

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('No response received from server.'));
    });

    it('logs a hint to check if the service is running', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(true);

      await handleUploadError(new Error('fetch failed'), 'spec.yaml', makeMetadata(), false);

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('Check if the service is running'));
    });

    it('does not call logResponseError', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(true);

      await handleUploadError(new Error('fetch failed'), 'spec.yaml', makeMetadata(), false);

      expect(logResponseErrorSpy).not.toHaveBeenCalled();
    });
  });

  describe('when the error is an unknown Error instance', () => {
    it('returns { ignored: false }', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(false);

      const result = await handleUploadError(new Error('something broke'), 'spec.yaml', makeMetadata(), false);

      expect(result).toEqual({ ignored: false });
    });

    it('logs the error message', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(false);

      await handleUploadError(new Error('something broke'), 'spec.yaml', makeMetadata(), false);

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('something broke'));
    });
  });

  describe('when the error is a non-Error object', () => {
    it('returns { ignored: false }', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(false);

      const result = await handleUploadError({ code: 42 }, 'spec.yaml', makeMetadata(), false);

      expect(result).toEqual({ ignored: false });
    });

    it('logs the JSON-serialised error', async () => {
      mockIsResponseError.mockReturnValue(false);
      mockIsFetchError.mockReturnValue(false);

      await handleUploadError({ code: 42 }, 'spec.yaml', makeMetadata(), false);

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('{"code":42}'));
    });
  });
});
