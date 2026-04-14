/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, spyOn } from 'bun:test';

import { logResponseError } from './log-response-error';

describe('logResponseError', () => {
  let consoleErrorSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleErrorSpy = spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  it('logs status and message from response body', async () => {
    const error = {
      response: new Response(JSON.stringify({ message: 'Something went wrong' }), {
        headers: { 'Content-Type': 'application/json' },
        status: 400,
        statusText: 'Bad Request',
      }),
    } as Error & { response: Response };

    await logResponseError(error);

    expect(consoleErrorSpy).toHaveBeenCalledTimes(1);

    expect(consoleErrorSpy.mock.calls[0][0]).toContain('Details: Something went wrong');
  });

  it('falls back to statusText when body has no message', async () => {
    const error = {
      response: new Response(JSON.stringify({ foo: 'bar' }), {
        headers: { 'Content-Type': 'application/json' },
        status: 404,
        statusText: 'Not Found',
      }),
    } as Error & { response: Response };

    await logResponseError(error);

    expect(consoleErrorSpy).toHaveBeenCalledTimes(1);

    expect(consoleErrorSpy.mock.calls[0][0]).toContain('Error: Not Found');
  });

  it('falls back to statusText when body is invalid JSON', async () => {
    const error = {
      response: new Response('not-json', {
        headers: { 'Content-Type': 'application/json' },
        status: 500,
        statusText: 'Internal Server Error',
      }),
    } as Error & { response: Response };

    await logResponseError(error);

    expect(consoleErrorSpy).toHaveBeenCalledTimes(1);

    expect(consoleErrorSpy.mock.calls[0][0]).toContain('Error: Internal Server Error');
  });
});
