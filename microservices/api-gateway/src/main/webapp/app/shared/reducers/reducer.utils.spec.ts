/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { AxiosError } from 'axios';

import { createEntitySlice, isFulfilledAction, isPendingAction, isRejectedAction, serializeAxiosError } from './reducer.utils';

describe('reducer.utils', () => {
  describe('isRejectedAction', () => {
    it('should return true for a rejected action type', () => {
      expect(isRejectedAction({ type: 'entity/rejected' })).toBe(true);
    });

    it('should return false for a non-rejected action type', () => {
      expect(isRejectedAction({ type: 'entity/fulfilled' })).toBe(false);
    });
  });

  describe('isPendingAction', () => {
    it('should return true for a pending action type', () => {
      expect(isPendingAction({ type: 'entity/pending' })).toBe(true);
    });

    it('should return false for a non-pending action type', () => {
      expect(isPendingAction({ type: 'entity/fulfilled' })).toBe(false);
    });
  });

  describe('isFulfilledAction', () => {
    it('should return true for a fulfilled action type', () => {
      expect(isFulfilledAction({ type: 'entity/fulfilled' })).toBe(true);
    });

    it('should return false for a non-fulfilled action type', () => {
      expect(isFulfilledAction({ type: 'entity/pending' })).toBe(false);
    });
  });

  describe('serializeAxiosError', () => {
    it('should return the value untouched when it is an axios error', () => {
      const axiosError = {
        isAxiosError: true,
        name: 'AxiosError',
        message: 'boom',
        toJSON: () => ({}),
      } as unknown as AxiosError;

      expect(serializeAxiosError(axiosError)).toBe(axiosError);
    });

    it('should build a simplified error from a plain object', () => {
      const result = serializeAxiosError({ name: 'Error', message: 'boom', stack: 'trace', code: 'ERR', irrelevant: 42 });

      expect(result).toEqual({ name: 'Error', message: 'boom', stack: 'trace', code: 'ERR' });
    });

    it('should wrap a non-object value as a message', () => {
      expect(serializeAxiosError('boom')).toEqual({ message: 'boom' });
    });

    it('should wrap null as a message', () => {
      expect(serializeAxiosError(null)).toEqual({ message: 'null' });
    });
  });

  describe('createEntitySlice', () => {
    const initialState = {
      loading: false,
      errorMessage: null,
      entities: [],
      entity: {},
      updating: false,
      updateSuccess: false,
    };

    it('should reset to initial state', () => {
      const slice = createEntitySlice({ name: 'test', initialState });

      const nextState = slice.reducer({ ...initialState, loading: true }, slice.actions.reset());

      expect(nextState).toEqual(initialState);
    });

    it('should handle rejection by default', () => {
      const slice = createEntitySlice({ name: 'test', initialState });

      const nextState = slice.reducer(
        { ...initialState, loading: true, updating: true, updateSuccess: true },
        {
          type: 'test/action/rejected',
        },
      );

      expect(nextState.loading).toBe(false);
      expect(nextState.updating).toBe(false);
      expect(nextState.updateSuccess).toBe(false);
    });

    it('should skip default rejection handling when skipRejectionHandling is true', () => {
      const slice = createEntitySlice({ name: 'test', initialState, skipRejectionHandling: true });

      const nextState = slice.reducer({ ...initialState, loading: true }, { type: 'test/action/rejected' });

      expect(nextState.loading).toBe(true);
    });

    it('should apply custom extraReducers in addition to the default rejection handling', () => {
      const slice = createEntitySlice({
        name: 'test',
        initialState,
        extraReducers(builder) {
          builder.addCase('test/custom', state => {
            state.updateSuccess = true;
          });
        },
      });

      const nextState = slice.reducer({ ...initialState }, { type: 'test/custom' });

      expect(nextState.updateSuccess).toBe(true);
    });
  });
});
