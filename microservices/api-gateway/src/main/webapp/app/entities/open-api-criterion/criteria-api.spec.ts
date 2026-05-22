/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { CriteriaApi } from 'app/clients/criteria-api';
import { criteriaApi } from 'app/entities/open-api-criterion/criteria-api';

jest.mock('app/clients/criteria-api', () => ({
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
