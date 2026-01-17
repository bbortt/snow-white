/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ListQualityGateReports200ResponseInner } from 'app/clients/report-api';
import type { IQualityGate } from 'app/shared/model/quality-gate.model';
import type { EntityState } from 'app/shared/reducers/reducer.utils';
import type { AxiosResponse } from 'axios';

import { reportApi } from 'app/entities/quality-gate/report-api';
import { defaultValue } from 'app/shared/model/quality-gate.model';
import configureStore from 'redux-mock-store';
import { thunk } from 'redux-thunk';

import reducer, { getEntities, getEntity, reset } from './quality-gate.reducer';

jest.mock('app/entities/quality-gate/report-api', () => ({
  reportApi: {
    listQualityGateReports: jest.fn(),
    getReportByCalculationId: jest.fn(),
  },
}));

describe('Quality-Gate reducer tests', () => {
  function isEmpty(element): boolean {
    if (element instanceof Array) {
      return element.length === 0;
    }
    return Object.keys(element).length === 0;
  }

  const initialState: EntityState<IQualityGate> = {
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
    it('should set a message in errorMessage', () => {
      testMultipleTypes(
        [getEntities.rejected.type, getEntity.rejected.type],
        'some message',
        state => {
          expect(state).toMatchObject({
            errorMessage: null,
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
  });

  describe('Actions', () => {
    let mockStore;
    let store;

    const resolvedObject: AxiosResponse<ListQualityGateReports200ResponseInner> = {
      data: {
        calculationId: '7769ae2f-cc7e-448d-ab07-0b4dc075744d',
        qualityGateConfigName: 'unit test',
        status: 'IN_PROGRESS',
        calculationRequest: {
          includeApis: [
            {
              serviceName: 'test service',
              apiName: 'test api',
              apiVersion: 'test api version',
            },
          ],
          lookbackWindow: '1234',
          attributeFilters: { foo: 'bar', traceparent: '00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01' },
        },
        interfaces: [
          {
            serviceName: 'test service',
            apiName: 'test api',
            apiVersion: 'test api version',
            apiType: 'OPENAPI',
            testResults: [
              {
                id: 'test_openapi_criterion',
                coverage: 0.5,
                additionalInformation: 'additional information',
                isIncludedInQualityGate: false,
              },
            ],
          },
        ],
        initiatedAt: '2025-05-07T18:00:00.00Z',
      },
    } as AxiosResponse;

    const expectedObject: IQualityGate = {
      calculationId: '7769ae2f-cc7e-448d-ab07-0b4dc075744d',
      qualityGateConfig: { name: 'unit test' },
      apiTests: [
        {
          serviceName: 'test service',
          apiName: 'test api',
          apiVersion: 'test api version',
          apiType: 'OPENAPI',
          testResults: [
            {
              id: 'test_openapi_criterion',
              coverage: 0.5,
              additionalInformation: 'additional information',
              isIncludedInQualityGate: false,
            },
          ],
        },
      ],
      status: 'IN_PROGRESS',
      createdAt: '2025-05-07T18:00:00.00Z',
      calculationRequest: {
        lookbackWindow: '1234',
        attributeFilters: 'foo=bar, traceparent=00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01',
      },
    };

    beforeEach(() => {
      mockStore = configureStore([thunk]);
      store = mockStore();

      (reportApi.listQualityGateReports as jest.MockedFn<any>).mockResolvedValueOnce({ data: [resolvedObject.data] });
      (reportApi.getReportByCalculationId as jest.MockedFn<any>).mockResolvedValueOnce(resolvedObject);
    });

    it('dispatches FETCH_QUALITYGATE_LIST actions', async () => {
      const expectedActions = [
        {
          type: getEntities.pending.type,
        },
        {
          type: getEntities.fulfilled.type,
          payload: { data: [expectedObject] },
        },
      ];

      await store.dispatch(getEntities({}));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);
    });

    it('dispatches FETCH_QUALITYGATE actions', async () => {
      const expectedActions = [
        {
          type: getEntity.pending.type,
        },
        {
          type: getEntity.fulfilled.type,
          payload: { data: expectedObject },
        },
      ];

      const calculationId = '0a32c534-8333-4b96-8e14-34bb5b4095d2';
      await store.dispatch(getEntity(calculationId));

      expect(store.getActions()[0]).toMatchObject(expectedActions[0]);
      expect(store.getActions()[1]).toMatchObject(expectedActions[1]);

      expect(reportApi.getReportByCalculationId).toHaveBeenCalledWith(calculationId);
    });

    it('dispatches RESET actions', async () => {
      const expectedActions = [reset()];

      await store.dispatch(reset());

      expect(store.getActions()).toEqual(expectedActions);
    });
  });
});
