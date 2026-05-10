/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { GetAllApis200ResponseInner } from 'app/clients/api-index-api';
import type { EntityState } from 'app/shared/reducers/reducer.utils';
import type { AxiosResponse } from 'axios';

import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { apiIndexApi } from 'app/entities/api-index/api-index-api';
import { createEntitySlice, serializeAxiosError } from 'app/shared/reducers/reducer.utils';

const defaultValue: GetAllApis200ResponseInner = {} as GetAllApis200ResponseInner;

const initialState: EntityState<GetAllApis200ResponseInner> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

export const getEntities = createAsyncThunk(
  'apiIndex/fetch_entity_list',
  async (): Promise<AxiosResponse<GetAllApis200ResponseInner[]>> => {
    return apiIndexApi.getAllApis();
  },
  { serializeError: serializeAxiosError },
);

export const ApiIndexSlice = createEntitySlice({
  name: 'apiIndex',
  initialState,
  extraReducers(builder) {
    builder
      .addMatcher(isFulfilled(getEntities), (state, action) => {
        const { data, headers } = action.payload;

        return {
          ...state,
          loading: false,
          entities: data,
          totalItems: parseInt(headers['x-total-count'], 10),
        };
      })
      .addMatcher(isPending(getEntities), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.loading = true;
      });
  },
});

export const { reset } = ApiIndexSlice.actions;

export default ApiIndexSlice.reducer;
