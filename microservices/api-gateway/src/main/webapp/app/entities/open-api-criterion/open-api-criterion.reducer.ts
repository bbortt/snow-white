/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { createEntitySlice, EntityState, serializeAxiosError } from 'app/shared/reducers/reducer.utils';
import { IOpenApiCriterion, defaultValue } from 'app/shared/model/open-api-criterion.model';
import { CriteriaApi } from 'app/clients/quality-gate-api';
import { AxiosResponse } from 'axios';
import { SnowWhiteState } from 'app/entities/reducers';

const initialState: EntityState<IOpenApiCriterion> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

const criteriaApi = new CriteriaApi(null, SERVER_API_URL);

// Actions

export const getEntities = createAsyncThunk('openApiCriterion/fetch_entity_list', async (): Promise<AxiosResponse<IOpenApiCriterion[]>> => {
  return criteriaApi.listOpenApiCriteria().then(response => ({
    ...response,
    data: response.data.map(openApiCriterion => {
      const { id, name, description } = openApiCriterion;
      return { name: id, label: name, description } as IOpenApiCriterion;
    }),
  }));
});

export const getEntity = createAsyncThunk(
  'openApiCriterion/fetch_entity',
  async (name: string, { dispatch, getState }) => {
    const state = (getState() as { snowwhite: SnowWhiteState }).snowwhite;

    if (state.openApiCriterion.entities.length === 0) {
      await dispatch(getEntities());
    }

    const updatedState = (getState() as { snowwhite: SnowWhiteState }).snowwhite;
    const entity = updatedState.openApiCriterion.entities.find(openApiCriterion => openApiCriterion.name === name);

    if (!entity) {
      throw new Error(`Entity with name "${name}" not found`);
    }

    return entity;
  },
  { serializeError: serializeAxiosError },
);

// slice

export const OpenApiCriterionSlice = createEntitySlice({
  name: 'openApiCriterion',
  initialState,
  extraReducers(builder) {
    builder
      .addCase(getEntity.fulfilled, (state, action) => {
        state.loading = false;
        state.entity = action.payload;
      })
      .addMatcher(isFulfilled(getEntities), (state, action) => {
        const { data, headers } = action.payload;

        return {
          ...state,
          loading: false,
          entities: data,
          totalItems: parseInt(headers['x-total-count'], 10),
        };
      })
      .addMatcher(isPending(getEntities, getEntity), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.loading = true;
      });
  },
});

export const { reset } = OpenApiCriterionSlice.actions;

// Reducer
export default OpenApiCriterionSlice.reducer;
