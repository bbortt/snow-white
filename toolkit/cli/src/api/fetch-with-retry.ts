/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { Agent, fetch as undiciFetch, RetryAgent } from 'undici';

export const createFetchWithRetry = (agent = new RetryAgent(new Agent())): unknown => {
  return (input: Parameters<typeof undiciFetch>[0], init?: Parameters<typeof undiciFetch>[1]) =>
    undiciFetch(input, { ...init, dispatcher: agent }) as unknown as Promise<Response>;
};
