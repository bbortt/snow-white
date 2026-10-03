/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { GetReportByCalculationId200Response } from 'app/clients/report-api';
import type { IQualityGate } from 'app/shared/model/quality-gate.model';
import type { EntityState, IQueryParams } from 'app/shared/reducers/reducer.utils';
import type { AxiosResponse } from 'axios';

import { createAsyncThunk, isFulfilled, isPending } from '@reduxjs/toolkit';
import { qualityGateApi } from 'app/entities/quality-gate-config/quality-gate-api';
import { reportApi } from 'app/entities/quality-gate/report-api';
import { FindingStatus } from 'app/shared/model/enumerations/finding-status.model';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import { defaultValue } from 'app/shared/model/quality-gate.model';
import { createEntitySlice, serializeAxiosError } from 'app/shared/reducers/reducer.utils';

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
  minCoveragePercentage,
  status,
  calculationRequest,
  interfaces,
  initiatedAt,
}: GetReportByCalculationId200Response): IQualityGate => ({
  calculationId,
  qualityGateConfig: { name: qualityGateConfigName },
  minCoveragePercentage,
  apiTests: interfaces?.map(apiTest => ({
    serviceName: apiTest.serviceName,
    apiName: apiTest.apiName,
    apiVersion: apiTest.apiVersion,
    apiType: apiTest.apiType,
    testResults: apiTest.testResults?.map(testResult => ({
      id: testResult.id,
      coverage: testResult.coverage,
      additionalInformation: testResult.additionalInformation,
      isIncludedInQualityGate: testResult.isIncludedInQualityGate ?? false,
      findings: testResult.findings?.map(finding => ({ ...finding, status: FindingStatus[finding.status] })),
    })),
    status: apiTest.status ? ReportStatus[apiTest.status] : ReportStatus.IN_PROGRESS,
    stackTrace: apiTest.stackTrace,
  })),
  status: ReportStatus[status],
  createdAt: initiatedAt,
  calculationRequest: {
    lookbackWindow: calculationRequest.lookbackWindow,
    attributeFilters: calculationRequest.attributeFilters,
  },
});

// Actions

interface QualityGateQueryParams extends IQueryParams {
  serviceName?: string;
  apiName?: string;
  apiVersion?: string;
}

export const getEntities = createAsyncThunk(
  'qualityGate/fetch_entity_list',
  async ({ page, size, sort, serviceName, apiName, apiVersion }: QualityGateQueryParams): Promise<AxiosResponse<IQualityGate[]>> => {
    return await reportApi.listQualityGateReports(page, size, sort, serviceName, apiName, apiVersion).then(response => ({
      ...response,
      data: response.data.map(qualityGateReport => fromDto(qualityGateReport)),
    }));
  },
  { serializeError: serializeAxiosError },
);

/**
 * The report is the authority on the threshold it was scored against; the gate is read only for its
 * current definition — name, description and criteria set — which is not pinned anywhere.
 *
 * That read is therefore allowed to fail. A gate deleted or renamed after a calculation leaves the
 * report intact, so losing the gate must cost the detail view its gate description, not the whole
 * page: the thunk keeps the report it already has rather than rejecting.
 */
export const getEntity = createAsyncThunk(
  'qualityGate/fetch_entity',
  async (calculationId: string): Promise<AxiosResponse<IQualityGate>> => {
    const reportResponse = await reportApi.getReportByCalculationId(calculationId);
    const qualityGate = fromDto(reportResponse.data);

    if (qualityGate.qualityGateConfig?.name) {
      try {
        const configResponse = await qualityGateApi.getQualityGateByName(qualityGate.qualityGateConfig.name);
        qualityGate.qualityGateConfig = {
          name: configResponse.data.name,
          description: configResponse.data.description,
          isPredefined: configResponse.data.isPredefined,
          minCoveragePercentage: configResponse.data.minCoveragePercentage,
          openApiCoverageCriteria: configResponse.data.openApiCoverageCriteria?.map(name => ({ name })),
        };
      } catch {
        // The gate is gone or unreachable. The report still carries its name and its pinned
        // threshold, which is everything the coverage bars need.
      }
    }

    return { ...reportResponse, data: qualityGate };
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
