/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export interface HeaderMessage {
  /** Success message */
  alert?: string;
  /** Error message */
  error?: string;
  /** Entity id for success messages. Entity name for error messages. */
  param?: string;
}

const headerToString = (headerValue: any): string => {
  if (Array.isArray(headerValue)) {
    if (headerValue.length > 1) {
      throw new Error('Multiple header values found');
    }
    headerValue = headerValue[0];
  }
  if (typeof headerValue !== 'string') {
    throw new Error('Header value is not a string');
  }
  return headerValue;
};

const decodeHeaderValue = (headerValue: string): string => decodeURIComponent(headerValue.replaceAll('+', ' '));

export const getMessageFromHeaders = (headers: Record<string, any>): HeaderMessage => {
  let alert: string | undefined = undefined;
  let param: string | undefined = undefined;
  let error: string | undefined = undefined;
  for (const [key, value] of Object.entries(headers)) {
    if (key.toLowerCase().endsWith('-alert')) {
      alert = headerToString(value);
    } else if (key.toLowerCase().endsWith('-error')) {
      error = headerToString(value);
    } else if (key.toLowerCase().endsWith('-params')) {
      param = decodeHeaderValue(headerToString(value));
    }
  }
  return { alert, error, param };
};
