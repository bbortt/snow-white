/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { isSubset } from './helpers';

describe('helpers', () => {
  describe('isSubset', () => {
    it('should return true when it is subset', () => {
      expect(isSubset({ foo: 'bar' }, { foo: 'bar', baz: 'buzz' })).toBe(true);
    });

    it('should return true when records are equal', () => {
      expect(isSubset({ foo: 'bar' }, { foo: 'bar' })).toBe(true);
    });

    it('should return false when it is not a subset', () => {
      expect(isSubset({ foo: 'bar' }, {})).toBe(false);
    });
  });
});
