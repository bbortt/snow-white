/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { CriteriaApi } from 'app/clients/quality-gate-api';
import { criteriaApi } from 'app/entities/open-api-criterion/criteria-api';

jest.mock('app/clients/quality-gate-api', () => ({
  CriteriaApi: jest.fn(),
}));

describe('Criteria API', () => {
  it('should be defined', () => {
    expect(criteriaApi).toBeDefined();
  });

  it('should be constructed', () => {
    expect(CriteriaApi).toHaveBeenCalledWith(undefined, SERVER_API_URL);
  });
});
