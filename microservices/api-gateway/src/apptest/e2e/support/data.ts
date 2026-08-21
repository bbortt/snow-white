/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

/*
 * Wire-format DTOs mirroring the OpenAPI-generated clients under
 * `src/main/webapp/app/clients/*`. Kept as plain local types instead of importing the generated
 * code so this black-box suite never depends on webapp build internals, only on the public
 * REST contract (which the generated clients also target).
 */

export type ApiType = 'ASYNCAPI' | 'GRAPHQL' | 'OPENAPI' | 'UNSPECIFIED';
export type CalculationStatus = 'FAILED' | 'FINISHED_EXCEPTIONALLY' | 'IN_PROGRESS' | 'PASSED' | 'TIMED_OUT';

export interface ApiTestResultDto {
  additionalInformation?: string;
  coverage: number;
  id: string;
  isIncludedInQualityGate?: boolean;
}

export interface ApiTestDto {
  apiName: string;
  apiType?: ApiType;
  apiVersion?: string;
  serviceName: string;
  stackTrace?: string;
  status?: CalculationStatus;
  testResults?: ApiTestResultDto[];
}

export interface QualityGateReportDto {
  calculationId: string;
  calculationRequest: {
    attributeFilters?: Record<string, string>;
    includeApis: Array<{ apiName: string; apiVersion: string; serviceName: string }>;
    lookbackWindow?: string;
  };
  initiatedAt: string;
  interfaces?: ApiTestDto[];
  qualityGateConfigName: string;
  status: CalculationStatus;
}

export function qualityGateReport(overrides: Partial<QualityGateReportDto> = {}): QualityGateReportDto {
  return {
    calculationId: 'b6e1e6d0-1c1a-4c1a-9b0a-000000000001',
    calculationRequest: {
      includeApis: [{ serviceName: 'order-service', apiName: 'orders-api', apiVersion: '1.0.0' }],
      lookbackWindow: '24h',
    },
    initiatedAt: '2026-08-01T10:00:00Z',
    interfaces: [],
    qualityGateConfigName: 'default',
    status: 'PASSED',
    ...overrides,
  };
}

export interface QualityGateConfigDto {
  description?: string;
  isPredefined: boolean;
  minCoveragePercentage: number;
  name: string;
  openApiCoverageCriteria: string[];
}

export function qualityGateConfig(overrides: Partial<QualityGateConfigDto> = {}): QualityGateConfigDto {
  return {
    description: 'Default Quality-Gate',
    isPredefined: false,
    minCoveragePercentage: 100,
    name: 'default',
    openApiCoverageCriteria: ['PATH_COVERAGE'],
    ...overrides,
  };
}

export interface OpenApiCriterionDto {
  description?: string;
  id: string;
  name: string;
}

// PATH_COVERAGE/HTTP_METHOD_COVERAGE are real criterion IDs with translations in
// i18n/en/openApiCriterion.json — using them exercises the translated-label path, not the
// "unknown criterion" fallback.
export function openApiCriterion(overrides: Partial<OpenApiCriterionDto> = {}): OpenApiCriterionDto {
  return {
    description: 'Every path defined in the OpenAPI specification has been called.',
    id: 'PATH_COVERAGE',
    name: 'Path Coverage',
    ...overrides,
  };
}

export interface ApiIndexEntryDto {
  apiName: string;
  apiType: ApiType;
  apiVersion: string;
  content?: string;
  prerelease?: boolean;
  serviceName: string;
  sourceUrl: string;
}

export function apiIndexEntry(overrides: Partial<ApiIndexEntryDto> = {}): ApiIndexEntryDto {
  return {
    apiName: 'orders-api',
    apiType: 'OPENAPI',
    apiVersion: '1.0.0',
    serviceName: 'order-service',
    sourceUrl: 'https://artifactory.example.com/orders-api-1.0.0.yaml',
    ...overrides,
  };
}
