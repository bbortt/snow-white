/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { QualityGateConfig } from 'app/clients/quality-gate-api';
import type { IOpenApiCriterion } from 'app/shared/model/open-api-criterion.model';
import type { IQualityGateConfig } from 'app/shared/model/quality-gate-config.model';
import type { IQueryParams, EntityState } from 'app/shared/reducers/reducer.utils';
import type { AxiosResponse } from 'axios';

import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { qualityGateApi } from 'app/entities/quality-gate-config/quality-gate-api';
import { defaultValue } from 'app/shared/model/quality-gate-config.model';
import { createEntitySlice, serializeAxiosError } from 'app/shared/reducers/reducer.utils';
import { cleanEntity } from 'app/shared/util/entity-utils';

const initialState: EntityState<IQualityGateConfig> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

const toDto = ({ name, description, openApiCriteria }: IQualityGateConfig): QualityGateConfig => {
  return {
    name,
    description,
    isPredefined: false,
    openApiCriteria: openApiCriteria?.map(openApiCriterion => openApiCriterion.name),
  };
};

const fromDto = ({ name, description, isPredefined, openApiCriteria }: QualityGateConfig): IQualityGateConfig => {
  return {
    name,
    description,
    isPredefined,
    openApiCriteria: openApiCriteria?.map(openApiCriterion => ({ name: openApiCriterion }) as IOpenApiCriterion),
  };
};

// Actions

export const getEntities = createAsyncThunk(
  'qualityGateConfig/fetch_entity_list',
  async ({ page, size, sort }: IQueryParams): Promise<AxiosResponse<IQualityGateConfig[]>> => {
    return qualityGateApi.getAllQualityGates(page, size, sort).then(response => {
      // eslint-disable-next-line no-console
      console.log('response from gateway:', response);
      return {
        ...response,
        data: response.data.map(qualityGateConfig => fromDto(qualityGateConfig)),
      };
    });
  },
  { serializeError: serializeAxiosError },
);

export const getEntity = createAsyncThunk(
  'qualityGateConfig/fetch_entity',
  async (name: string): Promise<AxiosResponse<IQualityGateConfig>> => {
    return qualityGateApi.getQualityGateByName(name).then(response => ({
      ...response,
      data: fromDto(response.data),
    }));
  },
  { serializeError: serializeAxiosError },
);

export const createEntity = createAsyncThunk(
  'qualityGateConfig/create_entity',
  async (entity: IQualityGateConfig, thunkAPI) => {
    const result = await qualityGateApi.createQualityGate(toDto(entity)).then(qualityGateConfig => {
      cleanEntity(entity);
      return qualityGateConfig;
    });
    thunkAPI.dispatch(getEntities({}));
    return result;
  },
  { serializeError: serializeAxiosError },
);

export const updateEntity = createAsyncThunk(
  'qualityGateConfig/update_entity',
  async (entity: IQualityGateConfig, thunkAPI) => {
    const result = await qualityGateApi.updateQualityGate(entity.name, toDto(entity)).then(qualityGateConfig => {
      cleanEntity(entity);
      return qualityGateConfig;
    });
    thunkAPI.dispatch(getEntities({}));
    return result;
  },
  { serializeError: serializeAxiosError },
);

export const deleteEntity = createAsyncThunk(
  'qualityGateConfig/delete_entity',
  async (name: string, thunkAPI) => {
    const result = await qualityGateApi.deleteQualityGate(name);
    thunkAPI.dispatch(getEntities({}));
    return result;
  },
  { serializeError: serializeAxiosError },
);

// slice

export const QualityGateConfigSlice = createEntitySlice({
  name: 'qualityGateConfig',
  initialState,
  extraReducers(builder) {
    builder
      .addCase(getEntity.fulfilled, (state, action) => {
        state.loading = false;
        state.entity = action.payload.data;
      })
      .addCase(deleteEntity.fulfilled, state => {
        state.updating = false;
        state.updateSuccess = true;
        state.entity = {};
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
      .addMatcher(isFulfilled(createEntity, updateEntity), (state, action) => {
        state.updating = false;
        state.loading = false;
        state.updateSuccess = true;
        state.entity = fromDto(action.payload.data);
      })
      .addMatcher(isPending(getEntities, getEntity), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.loading = true;
      })
      .addMatcher(isPending(createEntity, updateEntity, deleteEntity), state => {
        state.errorMessage = null;
        state.updateSuccess = false;
        state.updating = true;
      });
  },
});

export const { reset } = QualityGateConfigSlice.actions;

// Reducer
export default QualityGateConfigSlice.reducer;
