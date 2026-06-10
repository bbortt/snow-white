/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { GetAllApis200ResponseInner } from 'app/clients/api-index-api';
import type { EntityState } from 'app/shared/reducers/reducer.utils';
import type { AxiosResponse } from 'axios';

import { apiIndexApi } from 'app/entities/api-index/api-index-api';
import configureStore from 'redux-mock-store';
import { thunk } from 'redux-thunk';

import reducer, { getEntities, reset } from './api-index.reducer';

jest.mock('app/entities/api-index/api-index-api', () => ({
  apiIndexApi: {
    getAllApis: jest.fn(),
  },
}));

describe('API Index reducer tests', () => {
  function isEmpty(element): boolean {
    if (element instanceof Array) {
      return element.length === 0;
    }
    return Object.keys(element).length === 0;
  }

  const initialState: EntityState<GetAllApis200ResponseInner> = {
    loading: false,
    errorMessage: null,
    entities: [],
    entity: {} as GetAllApis200ResponseInner,
    totalItems: 0,
    updating: false,
    updateSuccess: false,
  };

  function testInitialState(state) {
    expect(state).toMatchObject({
      loading: false,
      errorMessage: null,
      updating: false,
      updateSuccess: false,
    });
    expect(isEmpty(state.entities));
    expect(isEmpty(state.entity));
  }

  describe('Common', () => {
    it('should return the initial state', () => {
      testInitialState(reducer(undefined, { type: '' }));
    });
  });

  describe('Requests', () => {
    it('should set state to loading', () => {
      expect(reducer(undefined, { type: getEntities.pending.type, payload: {} })).toMatchObject({
        errorMessage: null,
        updateSuccess: false,
        loading: true,
      });
    });

    it('should reset the state', () => {
      expect(reducer({ ...initialState, loading: true }, reset())).toEqual(initialState);
    });
  });

  describe('Failures', () => {
    it('should clear loading on rejection', () => {
      expect(
        reducer(undefined, {
          type: getEntities.rejected.type,
          payload: 'some message',
          error: { message: 'error message' },
        }),
      ).toMatchObject({
        errorMessage: null,
        updateSuccess: false,
        updating: false,
        loading: false,
      });
    });
  });

  describe('Successes', () => {
    it('should fetch all entities and store total count from header', () => {
      const payload: AxiosResponse<GetAllApis200ResponseInner[]> = {
        data: [
          {
            serviceName: 'service-a',
            apiName: 'api-a',
            apiVersion: '1.0.0',
            sourceUrl: 'http://example.com/api-a.yml',
            apiType: 'OPENAPI',
          },
          {
            serviceName: 'service-b',
            apiName: 'api-b',
            apiVersion: '2.0.0',
            sourceUrl: 'http://example.com/api-b.yml',
            apiType: 'ASYNCAPI',
            prerelease: true,
          },
        ],
        headers: { 'x-total-count': '42' },
      } as unknown as AxiosResponse<GetAllApis200ResponseInner[]>;

      expect(reducer(undefined, { type: getEntities.fulfilled.type, payload })).toEqual({
        ...initialState,
        loading: false,
        entities: payload.data,
        totalItems: 42,
      });
    });

    it('should parse x-total-count as an integer', () => {
      const payload = {
        data: [],
        headers: { 'x-total-count': '7' },
      };

      const state = reducer(undefined, { type: getEntities.fulfilled.type, payload });

      expect(state.totalItems).toBe(7);
    });
  });

  describe('Actions', () => {
    let mockStore;
    let store;

    const apiEntries: GetAllApis200ResponseInner[] = [
      {
        serviceName: 'my-service',
        apiName: 'my-api',
        apiVersion: '1.0.0',
        sourceUrl: 'http://artifactory.example.com/my-api-1.0.0.yml',
        apiType: 'OPENAPI',
        prerelease: false,
      },
    ];

    const resolvedResponse: AxiosResponse<GetAllApis200ResponseInner[]> = {
      data: apiEntries,
      headers: { 'x-total-count': '1' },
      status: 200,
      statusText: 'OK',
      config: {} as any,
    };

    beforeEach(() => {
      mockStore = configureStore([thunk]);
      store = mockStore();

      (apiIndexApi.getAllApis as jest.MockedFn<any>).mockResolvedValueOnce(resolvedResponse);
    });

    it('dispatches FETCH_API_INDEX_LIST pending then fulfilled', async () => {
      await store.dispatch(getEntities({}));

      const actions = store.getActions();
      expect(actions[0]).toMatchObject({ type: getEntities.pending.type });
      expect(actions[1]).toMatchObject({
        type: getEntities.fulfilled.type,
        payload: resolvedResponse,
      });
      expect(apiIndexApi.getAllApis).toHaveBeenCalledWith(undefined, undefined, undefined);
    });

    it('passes pagination and sorting params to API client', async () => {
      await store.dispatch(getEntities({ page: 2, size: 25, sort: 'apiName,asc' }));

      expect(apiIndexApi.getAllApis).toHaveBeenCalledWith(2, 25, 'apiName,asc');
    });

    it('dispatches RESET action', async () => {
      await store.dispatch(reset());

      expect(store.getActions()).toEqual([reset()]);
    });
  });
});
