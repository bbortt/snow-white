/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { cleanEntity, mapIdList, overridePaginationStateWithQueryParams, overrideSortStateWithQueryParams } from './entity-utils';

describe('Entity utils', () => {
  describe('cleanEntity', () => {
    it('should not remove fields with an id', () => {
      const entityA = {
        a: {
          id: 5,
        },
      };
      const entityB = {
        a: {
          id: '5',
        },
      };

      expect(cleanEntity({ ...entityA })).toEqual(entityA);
      expect(cleanEntity({ ...entityB })).toEqual(entityB);
    });

    it('should remove fields with an empty id', () => {
      const entity = {
        a: {
          id: '',
        },
      };

      expect(cleanEntity({ ...entity })).toEqual({});
    });

    it('should not remove fields that are not objects', () => {
      const entity = {
        a: '',
        b: 5,
        c: [],
        d: '5',
      };

      expect(cleanEntity({ ...entity })).toEqual(entity);
    });
  });

  describe('mapIdList', () => {
    it("should map ids no matter the element's type", () => {
      const ids = ['jhipster', '', 1, { key: 'value' }];

      expect(mapIdList(ids)).toEqual([{ id: 'jhipster' }, { id: 1 }, { id: { key: 'value' } }]);
    });

    it('should return an empty array', () => {
      const ids = [];

      expect(mapIdList(ids)).toEqual([]);
    });
  });

  describe('overrideSortStateWithQueryParams', () => {
    it('should override sort and order when the sort query param is present', () => {
      const paginationBaseState = { sort: 'id', order: 'asc' };

      const result = overrideSortStateWithQueryParams(paginationBaseState, '?sort=name,desc');

      expect(result).toEqual({ sort: 'name', order: 'desc' });
    });

    it('should leave the state untouched when the sort query param is absent', () => {
      const paginationBaseState = { sort: 'id', order: 'asc' };

      const result = overrideSortStateWithQueryParams(paginationBaseState, '');

      expect(result).toEqual(paginationBaseState);
    });
  });

  describe('overridePaginationStateWithQueryParams', () => {
    it('should override activePage when the page query param is present', () => {
      const paginationBaseState = { sort: 'id', order: 'asc', activePage: 1, itemsPerPage: 20 };

      const result = overridePaginationStateWithQueryParams(paginationBaseState, '?page=3');

      expect(result.activePage).toEqual(3);
    });

    it('should leave activePage untouched when the page query param is absent', () => {
      const paginationBaseState = { sort: 'id', order: 'asc', activePage: 1, itemsPerPage: 20 };

      const result = overridePaginationStateWithQueryParams(paginationBaseState, '');

      expect(result.activePage).toEqual(1);
    });

    it('should also override sort and order when both query params are present', () => {
      const paginationBaseState = { sort: 'id', order: 'asc', activePage: 1, itemsPerPage: 20 };

      const result = overridePaginationStateWithQueryParams(paginationBaseState, '?sort=name,desc&page=2');

      expect(result).toEqual({ sort: 'name', order: 'desc', activePage: 2, itemsPerPage: 20 });
    });
  });
});
