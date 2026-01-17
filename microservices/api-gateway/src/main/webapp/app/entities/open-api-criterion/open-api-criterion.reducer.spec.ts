/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { OpenApiCriterion } from 'app/clients/quality-gate-api';
import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import type { EntityState } from 'app/shared/reducers/reducer.utils';
import type { AxiosResponse } from 'axios';

import { criteriaApi } from 'app/entities/open-api-criterion/criteria-api';
import { defaultValue } from 'app/shared/model/open-api-criterion.model';
import configureStore from 'redux-mock-store';
import { thunk } from 'redux-thunk';

import reducer, { getEntities, getEntity, reset } from './open-api-criterion.reducer';

jest.mock('app/entities/open-api-criterion/criteria-api', () => ({
  criteriaApi: {
    listOpenApiCriteria: jest.fn(),
  },
}));

describe('OpenAPI Criterion reducer tests', () => {
  function isEmpty(element): boolean {
    if (element instanceof Array) {
      return element.length === 0;
    }
    return Object.keys(element).length === 0;
  }

  const initialState: EntityState<IOpenApiCriterion> = {
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

    it('should reset the state', () => {
      expect(reducer({ ...initialState, loading: true }, reset())).toEqual({
        ...initialState,
      });
    });
  });

  describe('Failures', () => {
    it.failing('should set a message in errorMessage', () => {
      testMultipleTypes(
        [getEntities.rejected.type, getEntity.rejected.type],
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
        entity: payload,
      });
    });
  });

  describe('Actions', () => {
    let mockStore;
    let store;

    const resolvedObject: AxiosResponse<OpenApiCriterion[]> = { data: [{ id: 'id', name: 'name' }] } as AxiosResponse;
    const expectedObject: IOpenApiCriterion = { name: 'id', label: 'name' };

    beforeEach(() => {
      mockStore = configureStore([thunk]);

      (criteriaApi.listOpenApiCriteria as jest.MockedFn<any>).mockResolvedValueOnce(resolvedObject);
    });

    afterEach(() => {
      store = null;
    });

    it('dispatches FETCH_OPENAPICRITERION_LIST actions', async () => {
      store = mockStore();

      const expectedActions = [
        {
          type: getEntities.pending.type,
        },
        {
          type: getEntities.fulfilled.type,
          payload: { data: [expectedObject] },
        },
      ];

      await store.dispatch(getEntities());

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);
    });

    it('dispatches FETCH_OPENAPICRITERION actions with predefined state', async () => {
      const defaultOpenApiCriterion: IOpenApiCriterion = { name: 'name', label: 'label' };
      store = mockStore({ snowwhite: { openApiCriterion: { entities: [defaultOpenApiCriterion] } } });

      const expectedActions = [
        {
          type: getEntity.pending.type,
        },
        {
          type: getEntity.fulfilled.type,
          payload: defaultOpenApiCriterion,
        },
      ];

      await store.dispatch(getEntity('name'));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);
    });

    it('dispatches FETCH_OPENAPICRITERION actions without predefined state', async () => {
      store = mockStore({ snowwhite: { openApiCriterion: { entities: [] } } });

      const expectedActions = [
        {
          type: getEntity.pending.type,
        },
        {
          type: getEntities.pending.type,
        },
        {
          type: getEntities.fulfilled.type,
          payload: { data: [expectedObject] },
        },
      ];

      await store.dispatch(getEntity('id'));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);
      expect(store.getActions()[2]).toMatchObject(expectedActions[2]);
      expect(store.getActions()[3]).not.toBeUndefined();
    });

    it('dispatches FETCH_OPENAPICRITERION actions with invalid name', async () => {
      store = mockStore({ snowwhite: { openApiCriterion: { entities: [{ name: 'something else', label: 'label' }] } } });

      const expectedActions = [
        {
          type: getEntity.pending.type,
        },
        {
          type: getEntity.rejected.type,
        },
      ];

      await store.dispatch(getEntity('not found'));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1].type).toBe(expectedActions[1].type);
      expect(store.getActions()[1].error.message).toBe('Entity with name "not found" not found');
    });

    it('dispatches RESET actions', async () => {
      store = mockStore();

      const expectedActions = [reset()];

      await store.dispatch(reset());

      expect(store.getActions()).toEqual(expectedActions);
    });
  });
});
