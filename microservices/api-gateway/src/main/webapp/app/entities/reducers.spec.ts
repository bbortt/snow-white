/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { getSnowWhiteState, SnowWhiteState } from 'app/entities/reducers';

describe('reducers', () => {
  describe('getSnowWhiteState', () => {
    it('should extract the root application state', () => {
      const snowWhiteState = {} as SnowWhiteState;
      const getState = () => ({ snowwhite: snowWhiteState });

      expect(getSnowWhiteState(getState)).toBe(snowWhiteState);
    });
  });
});
