/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
import { beforeEach, describe, expect, it, mock } from 'bun:test';
import { Agent, fetch as undiciFetch, RetryAgent } from 'undici';

import { createFetchWithRetry } from './fetch-with-retry';

// eslint-disable-next-line @typescript-eslint/no-floating-promises
mock.module('undici', () => {
  const mockFetch = mock();
  const MockAgent = mock().mockImplementation(() => ({ type: 'Agent' }));
  const MockRetryAgent = mock().mockImplementation(agent => ({ type: 'RetryAgent', wraps: agent }));

  return {
    Agent: MockAgent,
    fetch: mockFetch,
    RetryAgent: MockRetryAgent,
  };
});

const mockUndiciFetch = undiciFetch;
const MockAgent = Agent;
const MockRetryAgent = RetryAgent;

describe(createFetchWithRetry, () => {
  beforeEach(() => {
    mock.clearAllMocks();
  });

  describe('default agent behaviour', () => {
    it('creates a RetryAgent wrapping an Agent when no agent is provided', () => {
      createFetchWithRetry();

      expect(MockAgent).toHaveBeenCalledTimes(1);
      expect(MockRetryAgent).toHaveBeenCalledTimes(1);
      expect(MockRetryAgent).toHaveBeenCalledWith(expect.any(Object)); // the Agent instance
    });

    it('returns a function', () => {
      const fetchFn = createFetchWithRetry();
      expect(typeof fetchFn).toBe('function');
    });
  });

  describe('custom agent', () => {
    it('uses the provided agent instead of creating a new one', () => {
      const customAgent = { type: 'CustomAgent' } as unknown as RetryAgent;
      createFetchWithRetry(customAgent);

      expect(MockAgent).not.toHaveBeenCalled();
      expect(MockRetryAgent).not.toHaveBeenCalled();
    });

    it('passes the custom agent as dispatcher', async () => {
      const customAgent = { type: 'CustomAgent' } as unknown as RetryAgent;
      const fetchFn = createFetchWithRetry(customAgent);

      mockUndiciFetch.mockResolvedValueOnce(new Response('ok') as any);

      await fetchFn('https://example.com');

      expect(mockUndiciFetch).toHaveBeenCalledWith('https://example.com', {
        dispatcher: customAgent,
      });
    });
  });

  describe('returned fetch function', () => {
    it('calls undiciFetch with the given input and merges init options', async () => {
      const fetchFn = createFetchWithRetry();
      const mockResponse = new Response('hello') as any;
      mockUndiciFetch.mockResolvedValueOnce(mockResponse);

      const result = await fetchFn('https://example.com', {
        headers: { 'Content-Type': 'application/json' },
        method: 'POST',
      });

      expect(mockUndiciFetch).toHaveBeenCalledWith(
        'https://example.com',
        expect.objectContaining({
          dispatcher: expect.anything(),
          headers: { 'Content-Type': 'application/json' },
          method: 'POST',
        }),
      );
      expect(result).toBe(mockResponse);
    });

    it('always injects the dispatcher, overriding any dispatcher in init', async () => {
      const customAgent = { type: 'CustomAgent' } as unknown as RetryAgent;
      const fetchFn = createFetchWithRetry(customAgent);
      mockUndiciFetch.mockResolvedValueOnce(new Response() as any);

      const differentDispatcher = { type: 'Other' } as any;
      await fetchFn('https://example.com', { dispatcher: differentDispatcher });

      const [, passedInit] = mockUndiciFetch.mock.calls[0];
      expect((passedInit as any).dispatcher).toBe(customAgent);
    });

    it('works without an init argument', async () => {
      const fetchFn = createFetchWithRetry();
      mockUndiciFetch.mockResolvedValueOnce(new Response() as any);

      await fetchFn('https://example.com');

      expect(mockUndiciFetch).toHaveBeenCalledWith('https://example.com', expect.objectContaining({ dispatcher: expect.anything() }));
    });

    it('propagates rejections from undiciFetch', () => {
      const fetchFn = createFetchWithRetry();
      mockUndiciFetch.mockRejectedValueOnce(new Error('network error'));

      expect(fetchFn('https://example.com')).rejects.toThrow('network error');
    });

    it('accepts a Request object as input', async () => {
      const fetchFn = createFetchWithRetry();
      mockUndiciFetch.mockResolvedValueOnce(new Response() as any);

      const request = new Request('https://example.com', { method: 'GET' });
      await fetchFn(request);

      expect(mockUndiciFetch).toHaveBeenCalledWith(request, expect.objectContaining({ dispatcher: expect.anything() }));
    });
  });
});
