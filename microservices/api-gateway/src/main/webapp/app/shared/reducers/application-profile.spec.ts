/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { configureStore } from '@reduxjs/toolkit';
import axios from 'axios';
import sinon from 'sinon';

import profile, { getProfile, setPerformanceImpacted } from './application-profile';

describe('Profile reducer tests', () => {
  const initialState = {
    ribbonEnv: '',
    impactedPerformance: false,
    isBackendUnavailable: false,
    inProduction: true,
  };

  describe('Common tests', () => {
    it('should return the initial state', () => {
      const toTest = profile(undefined, { type: '' });
      expect(toTest).toEqual(initialState);
    });

    it('should return the right payload in prod', () => {
      const payload = {
        data: {
          'display-ribbon-on-profiles': 'awesome ribbon stuff',
          activeProfiles: ['prod'],
        },
      };

      expect(profile(undefined, { type: getProfile.fulfilled.type, payload })).toEqual({
        ribbonEnv: 'awesome ribbon stuff',
        impactedPerformance: false,
        inProduction: true,
        isBackendUnavailable: false,
      });
    });

    it('should return the right payload in dev with OpenAPI enabled', () => {
      const payload = {
        data: {
          'display-ribbon-on-profiles': 'awesome ribbon stuff',
          activeProfiles: ['api-docs', 'dev'],
        },
      };

      expect(profile(undefined, { type: getProfile.fulfilled.type, payload })).toEqual({
        ribbonEnv: 'awesome ribbon stuff',
        impactedPerformance: false,
        inProduction: false,
        isBackendUnavailable: false,
      });
    });
  });

  describe('Actions', () => {
    const resolvedObject = { value: 'whatever' };
    const getState = jest.fn();
    const dispatch = jest.fn();
    const extra = {};

    beforeEach(() => {
      configureStore({
        reducer: (state = [], action) => [...state, action],
      });

      axios.get = sinon.stub().returns(Promise.resolve(resolvedObject));
    });

    describe('setPerformanceImpacted', () => {
      it.each([true, false])('creates action: %s', (impacted: boolean) => {
        const result = setPerformanceImpacted(impacted);

        expect(result).toEqual({
          payload: impacted,
          type: 'applicationProfile/set_performance_impacted',
        });
      });
    });

    describe('getProfile', () => {
      it('dispatches GET_SESSION_PENDING and GET_SESSION_FULFILLED actions', async () => {
        const result = await getProfile()(dispatch, getState, extra);

        const pendingAction = dispatch.mock.calls[0][0];
        expect(pendingAction.meta.requestStatus).toBe('pending');
        expect(getProfile.fulfilled.match(result)).toBe(true);
      });
    });
  });

  describe('Reducer', () => {
    it.each([true, false])(
      'should set impactedPerformance to %s when setPerformanceImpacted is being dispatched',
      (impactedPerformance: boolean) => {
        expect(profile(undefined, { type: setPerformanceImpacted.type, payload: impactedPerformance })).toEqual({
          ...initialState,
          impactedPerformance,
        });
      },
    );

    it('should set isBackendUnavailable to true when getProfile is rejected with a 5xx status', () => {
      const payload = {
        name: 'AxiosError',
        message: 'Request failed with status code 500',
        status: 500,
      };

      expect(profile(undefined, { type: getProfile.rejected.type, error: payload })).toEqual({
        ...initialState,
        isBackendUnavailable: true,
      });
    });
  });
});
