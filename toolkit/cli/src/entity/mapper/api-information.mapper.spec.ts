/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { describe, expect } from 'bun:test';

import { toDtos } from './api-information.mapper';

describe('API Information Mapper', () => {
  describe('toDtos', () => {
    const genericApiInformation = { apiName: 'API1', apiVersion: 'v1', serviceName: 'Service1' };
    expect(toDtos([genericApiInformation])).toEqual([genericApiInformation]);
  });
});
