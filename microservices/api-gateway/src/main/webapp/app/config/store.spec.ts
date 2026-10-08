/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { configureInjectableStore } from './store';

describe('store', () => {
  describe('configureInjectableStore', () => {
    it('should inject an async reducer and replace the store reducer', () => {
      const replaceReducer = jest.fn();
      const fakeStore = { replaceReducer } as any;

      const injectableStore = configureInjectableStore(fakeStore);
      const asyncReducer = (state = {}) => state;

      injectableStore.injectReducer('foo', asyncReducer);

      expect(injectableStore.asyncReducers.foo).toBe(asyncReducer);
      expect(replaceReducer).toHaveBeenCalledTimes(1);
    });
  });
});
