/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { expectFailsWithMessageContaining, isSubset } from './helpers';
import { renderHelmChart } from './render-helm-chart';

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

  describe('snow-white.replicas', () => {
    it('should throw when snow-white.mode contains unexpected value', async () => {
      await expect(() =>
        renderHelmChart({
          chartPath: 'charts/snow-white',
          values: {
            snowWhite: {
              mode: 'custom',
            },
          },
        }),
      ).rejects.toThrow(
        "⚠ ERROR: You must set 'snowWhite.mode' to a valid value: 'minimal', 'high-available' or 'autoscale'!",
      );
    });
  });
});
