/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { QualityGateConfig } from 'app/clients/quality-gate-api';
import type { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';
import type { EntityState } from 'app/shared/reducers/reducer.utils';
import type { AxiosResponse } from 'axios';

import { qualityGateApi } from 'app/entities/quality-gate-config/quality-gate-api';
import { defaultValue } from 'app/shared/model/quality-gate-config.model';
import configureStore from 'redux-mock-store';
import { thunk } from 'redux-thunk';

import reducer, { createEntity, deleteEntity, getEntities, getEntity, updateEntity, reset } from './quality-gate-config.reducer';

jest.mock('app/entities/quality-gate-config/quality-gate-api', () => ({
  qualityGateApi: {
    getAllQualityGates: jest.fn(),
    getQualityGateByName: jest.fn(),
    createQualityGate: jest.fn(),
    updateQualityGate: jest.fn(),
    deleteQualityGate: jest.fn(),
  },
}));

describe('Quality-Gate Config reducer tests', () => {
  function isEmpty(element): boolean {
    if (element instanceof Array) {
      return element.length === 0;
    }
    return Object.keys(element).length === 0;
  }

  const initialState: EntityState<IQualityGateConfig> = {
    loading: false,
    errorMessage: null,
    entities: [],
    entity: defaultValue,
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

  function testMultipleTypes(types, payload, testFunction, error?) {
    types.forEach(e => {
      testFunction(reducer(undefined, { type: e, payload, error }));
    });
  }

  describe('Common', () => {
    it('should return the initial state', () => {
      testInitialState(reducer(undefined, { type: '' }));
    });
  });

  describe('Requests', () => {
    it('should set state to loading', () => {
      testMultipleTypes([getEntities.pending.type, getEntity.pending.type], {}, state => {
        expect(state).toMatchObject({
          errorMessage: null,
          updateSuccess: false,
          loading: true,
        });
      });
    });

    it('should set state to updating', () => {
      testMultipleTypes([createEntity.pending.type, updateEntity.pending.type, deleteEntity.pending.type], {}, state => {
        expect(state).toMatchObject({
          errorMessage: null,
          updateSuccess: false,
          updating: true,
        });
      });
    });

    it('should reset the state', () => {
      expect(reducer({ ...initialState, loading: true }, reset())).toEqual({
        ...initialState,
      });
    });
  });

  describe('Failures', () => {
    it.failing('should set a message in errorMessage', () => {
      testMultipleTypes(
        [
          getEntities.rejected.type,
          getEntity.rejected.type,
          createEntity.rejected.type,
          updateEntity.rejected.type,
          deleteEntity.rejected.type,
        ],
        'some message',
        state => {
          expect(state).toMatchObject({
            errorMessage: 'error message',
            updateSuccess: false,
            updating: false,
          });
        },
        {
          message: 'error message',
        },
      );
    });
  });

  describe('Successes', () => {
    it('should fetch all entities', () => {
      const payload = { data: [{ 1: 'fake1' }, { 2: 'fake2' }], headers: { 'x-total-count': 123 } };
      expect(
        reducer(undefined, {
          type: getEntities.fulfilled.type,
          payload,
        }),
      ).toEqual({
        ...initialState,
        loading: false,
        totalItems: payload.headers['x-total-count'],
        entities: payload.data,
      });
    });

    it('should fetch a single entity', () => {
      const payload = { data: { 1: 'fake1' } };
      expect(
        reducer(undefined, {
          type: getEntity.fulfilled.type,
          payload,
        }),
      ).toEqual({
        ...initialState,
        loading: false,
        entity: payload.data,
      });
    });

    it('should create/update entity', () => {
      const payload = { data: 'fake payload' };
      expect(
        reducer(undefined, {
          type: createEntity.fulfilled.type,
          payload,
        }),
      ).toEqual({
        ...initialState,
        updating: false,
        updateSuccess: true,
        entity: { description: undefined, isPredefined: undefined, name: undefined, openApiCriteria: undefined },
      });
    });

    it('should delete entity', () => {
      const payload = 'fake payload';
      const toTest = reducer(undefined, {
        type: deleteEntity.fulfilled.type,
        payload,
      });
      expect(toTest).toMatchObject({
        updating: false,
        updateSuccess: true,
      });
    });
  });

  describe('Actions', () => {
    let store;

    const resolvedObject: AxiosResponse<QualityGateConfig> = {
      data: { name: 'name', isPredefined: true, openApiCriteria: ['test_openapi_criterion'] },
    } as AxiosResponse;

    const epxectedObject: IQualityGateConfig = {
      name: 'name',
      isPredefined: true,
      openApiCriteria: [{ name: 'test_openapi_criterion' }],
    };

    beforeEach(() => {
      const mockStore = configureStore([thunk]);
      store = mockStore({});

      (qualityGateApi.getAllQualityGates as jest.MockedFn<any>).mockResolvedValueOnce({ data: [resolvedObject.data] });
      (qualityGateApi.getQualityGateByName as jest.MockedFn<any>).mockResolvedValueOnce(resolvedObject);
      (qualityGateApi.createQualityGate as jest.MockedFn<any>).mockResolvedValueOnce(resolvedObject);
      (qualityGateApi.updateQualityGate as jest.MockedFn<any>).mockResolvedValueOnce(resolvedObject);
      (qualityGateApi.deleteQualityGate as jest.MockedFn<any>).mockResolvedValueOnce(resolvedObject);
    });

    it('dispatches FETCH_QUALITYGATECONFIG_LIST actions', async () => {
      const expectedActions = [
        {
          type: getEntities.pending.type,
        },
        {
          type: getEntities.fulfilled.type,
          payload: { data: [epxectedObject] },
        },
      ];

      await store.dispatch(getEntities({}));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);
    });

    it('dispatches FETCH_QUALITYGATECONFIG actions', async () => {
      const expectedActions = [
        {
          type: getEntity.pending.type,
        },
        {
          type: getEntity.fulfilled.type,
          payload: { data: epxectedObject },
        },
      ];

      const name = 'name';
      await store.dispatch(getEntity(name));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);

      expect(qualityGateApi.getQualityGateByName).toHaveBeenCalledWith(name);
    });

    it('dispatches CREATE_QUALITYGATECONFIG actions', async () => {
      const expectedActions = [
        {
          type: createEntity.pending.type,
        },
        {
          type: getEntities.pending.type,
        },
        {
          type: createEntity.fulfilled.type,
          payload: resolvedObject,
        },
      ];

      const qualityGateConfig: IQualityGateConfig = { name: 'name', openApiCriteria: [{ name: 'test_openapi_criterion' }] };
      await store.dispatch(createEntity(qualityGateConfig));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);
      expect(store.getActions()[2]).toMatchObject(expectedActions[2]);

      expect(qualityGateApi.createQualityGate).toHaveBeenCalledWith(
        expect.objectContaining({
          ...qualityGateConfig,
          isPredefined: false,
          openApiCriteria: ['test_openapi_criterion'],
        }),
      );
    });

    it('dispatches UPDATE_QUALITYGATECONFIG actions', async () => {
      const expectedActions = [
        {
          type: updateEntity.pending.type,
        },
        {
          type: getEntities.pending.type,
        },
        {
          type: updateEntity.fulfilled.type,
          payload: resolvedObject,
        },
      ];

      const qualityGateConfig: IQualityGateConfig = { name: 'name', openApiCriteria: [{ name: 'test_openapi_criterion' }] };
      await store.dispatch(updateEntity(qualityGateConfig));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);
      expect(store.getActions()[2]).toMatchObject(expectedActions[2]);

      expect(qualityGateApi.updateQualityGate).toHaveBeenCalledWith(
        qualityGateConfig.name,
        expect.objectContaining({
          ...qualityGateConfig,
          isPredefined: false,
          openApiCriteria: ['test_openapi_criterion'],
        }),
      );
    });

    it('dispatches DELETE_QUALITYGATECONFIG actions', async () => {
      const expectedActions = [
        {
          type: deleteEntity.pending.type,
        },
        {
          type: getEntities.pending.type,
        },
        {
          type: deleteEntity.fulfilled.type,
          payload: resolvedObject,
        },
      ];

      const name = 'name';
      await store.dispatch(deleteEntity(name));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);
      expect(store.getActions()[2]).toMatchObject(expectedActions[2]);

      expect(qualityGateApi.deleteQualityGate).toHaveBeenCalledWith(name);
    });

    it('dispatches RESET actions', async () => {
      const expectedActions = [reset()];

      await store.dispatch(reset());

      expect(store.getActions()).toEqual(expectedActions);
    });
  });
});
