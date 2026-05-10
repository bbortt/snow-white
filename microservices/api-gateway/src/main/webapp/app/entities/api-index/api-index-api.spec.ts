/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { ApiIndexApi } from 'app/clients/api-index-api';
import { apiIndexApi } from 'app/entities/api-index/api-index-api';

jest.mock('app/clients/api-index-api', () => ({
  ApiIndexApi: jest.fn(),
}));

describe('Report API', () => {
  it('should be defined', () => {
    expect(apiIndexApi).toBeDefined();
  });

  it('should be constructed', () => {
    expect(ApiIndexApi).toHaveBeenCalledWith(undefined, SERVER_API_URL);
  });
});
