/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { ListQualityGateReports200ResponseInner } from 'app/clients/report-api';
import type { IQualityGate } from 'app/shared/model/quality-gate.model';
import type { EntityState, IQueryParams } from 'app/shared/reducers/reducer.utils';
import type { AxiosResponse } from 'axios';

import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { reportApi } from 'app/entities/quality-gate/report-api';
import { defaultValue } from 'app/shared/model/quality-gate.model';
import { createEntitySlice, serializeAxiosError } from 'app/shared/reducers/reducer.utils';
import dayjs from 'dayjs';

const initialState: EntityState<IQualityGate> = {
  loading: false,
  errorMessage: null,
  entities: [],
  entity: defaultValue,
  updating: false,
  totalItems: 0,
  updateSuccess: false,
};

const fromDto = ({
  calculationId,
  qualityGateConfigName,
  status,
  calculationRequest,
  initiatedAt,
  openApiTestResults,
}: ListQualityGateReports200ResponseInner): IQualityGate => ({
  calculationId,
  qualityGateConfig: { name: qualityGateConfigName },
  status,
  calculationRequest: { ...calculationRequest, attributeFilters: joinAttributeFilters(calculationRequest.attributeFilters) },
  createdAt: dayjs(initiatedAt),
  openApiTestResults: openApiTestResults?.map(openApiTestResult => ({
    ...openApiTestResult,
    openApiCriterionName: openApiTestResult.id,
  })),
});

export const joinAttributeFilters = (attributeFilters?: Record<string, string>): string => {
  if (!attributeFilters || Object.keys(attributeFilters).length === 0) {
    return '';
  }

  return Object.entries(attributeFilters)
    .map(([key, value]) => `${key}=${value}`)
    .join(', ');
};

// Actions

export const getEntities = createAsyncThunk(
  'qualityGate/fetch_entity_list',
  async ({ page, size, sort }: IQueryParams): Promise<AxiosResponse<IQualityGate[]>> => {
    return reportApi.listQualityGateReports(page, size, sort).then(response => ({
      ...response,
      data: response.data.map(qualityGateReport => fromDto(qualityGateReport)),
    }));
  },
  { serializeError: serializeAxiosError },
);

export const getEntity = createAsyncThunk(
  'qualityGate/fetch_entity',
  async (calculationId: string): Promise<AxiosResponse<IQualityGate>> => {
    return reportApi.getReportByCalculationId(calculationId).then(response => ({
      ...response,
      data: fromDto(response.data),
    }));
  },
  { serializeError: serializeAxiosError },
);

// slice

export const QualityGateSlice = createEntitySlice({
  name: 'qualityGate',
  initialState,
  extraReducers(builder) {
    builder
      .addCase(getEntity.fulfilled, (state, action) => {
        state.loading = false;
        state.entity = action.payload.data;
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

export const { reset } = QualityGateSlice.actions;

// Reducer
export default QualityGateSlice.reducer;
