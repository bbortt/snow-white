/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export const isFetchError = (error: unknown): boolean =>
  error instanceof TypeError || (error instanceof Error && error.name === 'FetchError');

export const isResponseError = (error: unknown): error is Error & { response: Response } =>
  error instanceof Error && error.name === 'ResponseError' && 'response' in error;
