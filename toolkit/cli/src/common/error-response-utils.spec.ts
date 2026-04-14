/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect, it } from 'bun:test';

import { isFetchError, isResponseError } from './error-response-utils';

const makeResponseError = (): Error & { response: Response } => {
  const error = new Error('response error') as Error & { response: Response };
  error.name = 'ResponseError';
  error.response = new Response(null, { status: 500 });
  return error;
};

describe('isFetchError', () => {
  it('returns true for a TypeError', () => {
    expect(isFetchError(new TypeError('Failed to fetch'))).toBe(true);
  });

  it('returns true for an Error with name "FetchError"', () => {
    const error = new Error('fetch failed');
    error.name = 'FetchError';
    expect(isFetchError(error)).toBe(true);
  });

  it('returns false for a plain Error', () => {
    expect(isFetchError(new Error('generic'))).toBe(false);
  });

  it('returns false for an Error with an unrelated name', () => {
    const error = new Error('something');
    error.name = 'ResponseError';
    expect(isFetchError(error)).toBe(false);
  });

  it('returns false for a plain object', () => {
    expect(isFetchError({ message: 'nope' })).toBe(false);
  });

  it('returns false for null', () => {
    expect(isFetchError(null)).toBe(false);
  });

  it('returns false for undefined', () => {
    expect(isFetchError(undefined)).toBe(false);
  });

  it('returns false for a string', () => {
    expect(isFetchError('error')).toBe(false);
  });
});

describe('isResponseError', () => {
  it('returns true for a well-formed ResponseError', () => {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    expect(isResponseError(makeResponseError())).toBe(true);
  });

  it('returns false for an Error without name "ResponseError"', () => {
    const error = new Error('generic');
    expect(isResponseError(error)).toBe(false);
  });

  it('returns false for an Error named "ResponseError" but missing the response property', () => {
    const error = new Error('no response');
    error.name = 'ResponseError';
    expect(isResponseError(error)).toBe(false);
  });

  it('returns false for a TypeError (even though it is an Error subclass)', () => {
    expect(isResponseError(new TypeError('type error'))).toBe(false);
  });

  it('returns false for a plain object that looks like a ResponseError', () => {
    expect(isResponseError({ name: 'ResponseError', response: new Response() })).toBe(false);
  });

  it('returns false for null', () => {
    expect(isResponseError(null)).toBe(false);
  });

  it('returns false for undefined', () => {
    expect(isResponseError(undefined)).toBe(false);
  });

  it('returns false for a string', () => {
    expect(isResponseError('ResponseError')).toBe(false);
  });
});
