/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IWireMockRequest, IWireMockResponse } from 'wiremock-captain';
import { WireMock } from 'wiremock-captain';
import { execa } from 'execa';

const ITEMS_TO_INDEX = 500;
const DEFAULT_PAGE_SIZE = 10;

const containerRegistry = process.env.CONTAINER_REGISTRY ?? 'ghcr.io';
const imageTag = process.env.IMAGE_TAG ?? '1.0.0-SNAPSHOT';

const createSirApiIndex = (): unknown[] => {
  const apiIndex = [];
  for (let i = 0; i < ITEMS_TO_INDEX; i++) {
    apiIndex.push({
      name: 'petstore-1.0.0.tgz',
      properties: [
        {
          key: 'oas.info.title',
          value: 'Swagger Petstore',
        },
        {
          key: 'oas.info.version',
          value: '1.0.0',
        },
        {
          key: 'oas.info.x-api-name',
          value: 'swagger-petstore',
        },
        {
          key: 'oas.info.x-service-name',
          value: 'example-application',
        },
        {
          key: 'oas.type',
          value: 'openapi',
        },
      ],
      source: 'https://www.petstore.dev/',
    });
  }
  return apiIndex;
};

const registerBackstageApiIndexPages = async (
  wiremock: WireMock,
): Promise<void> => {
  const items = [];
  for (let i = 0; i < DEFAULT_PAGE_SIZE; i++) {
    items.push({
      metadata: {
        annotations: {},
      },
      spec: {
        lifecycle: 'production',
        type: 'openapi',
        definition:
          'openapi: 3.1.0\ninfo:\n  title: Swagger Petstore\n  version: 1.0.0\n  x-service-name: example-application\npaths: {}',
      },
    });
  }

  for (let i = 0; i < ITEMS_TO_INDEX; i = i + DEFAULT_PAGE_SIZE) {
    const totalEntitiesRequest: IWireMockRequest = {
      method: 'GET',
      endpoint: `/entities/by-query?fields=metadata.annotations%2Cspec.definition&limit=${DEFAULT_PAGE_SIZE}&offset=${i}`,
    };
    const totalEntitiesResponse: IWireMockResponse = {
      status: 200,
      body: {
        items,
      },
    };
    await wiremock.register(totalEntitiesRequest, totalEntitiesResponse);
  }
};

const mapObjectToDockerEnvironmentArguments = (envObject: {
  [key: string]: string;
}): Array<string> => {
  let dockerJobArguments = [];
  Object.keys(envObject).forEach((key: string) => {
    dockerJobArguments.push('--env', `${key}=${envObject[key]}`);
  });
  return dockerJobArguments;
};

const getDockerImageName = (): string => {
  return `${containerRegistry}/bbortt/snow-white/api-sync-job:${imageTag}`;
};

const dockerDefaultArguments = [
  'run',
  '--rm',
  '--network',
  process.env.DOCKER_NETWORK ?? 'github_actions',
  '--cpus=0.5',
  '--memory=512m',
  '--env',
  'OTEL_LOGS_EXPORTER=none',
  '--env',
  'OTEL_METRICS_EXPORTER=none',
  '--env',
  'OTEL_TRACES_EXPORTER=none',
];

const invokeApiSyncJobWithDocker = async (apiSyncJobEnv: {
  [key: string]: string;
}): Promise<void> => {
  const dockerJobArguments =
    mapObjectToDockerEnvironmentArguments(apiSyncJobEnv);
  const commandLineArgs = [
    ...dockerDefaultArguments,
    ...dockerJobArguments,
    getDockerImageName(),
  ];

  console.info(`Executing docker command: ${commandLineArgs.join(' ')}`);

  await execa('docker', commandLineArgs);
};

describe('API Sync Job', () => {
  const indexUri = '/wiremock.json';

  const wiremockEndpoint =
    process.env.WIREMOCK_ENDPOINT ?? 'http://localhost:9000';
  const wiremock = new WireMock(wiremockEndpoint);

  beforeEach(async () => {
    await wiremock.clearAll();
  });

  describe('backstage', () => {
    const EXPECTED_TIME = 120_000;

    it(
      `should process ${ITEMS_TO_INDEX} OpenAPI specifications in ${EXPECTED_TIME}ms`,
      async () => {
        const totalEntitiesRequest: IWireMockRequest = {
          method: 'GET',
          endpoint: '/entities/by-query?limit=0',
        };
        const totalEntitiesResponse: IWireMockResponse = {
          status: 200,
          body: {
            items: [],
            totalItems: ITEMS_TO_INDEX,
            pageInfo: {},
          },
        };
        await wiremock.register(totalEntitiesRequest, totalEntitiesResponse);

        await registerBackstageApiIndexPages(wiremock);

        const apiIndexExistsRequest: IWireMockRequest = {
          method: 'GET',
          endpoint:
            '/api/rest/v1/apis/example-application/Swagger%20Petstore/1.0.0/exists',
        };
        const apiIndexExistsResponse: IWireMockResponse = {
          status: 404,
        };
        await wiremock.register(apiIndexExistsRequest, apiIndexExistsResponse);

        const ingestApiEndpoint = '/api/rest/v1/apis';
        const apiIndexRequest: IWireMockRequest = {
          method: 'POST',
          endpoint: ingestApiEndpoint,
        };
        const apiIndexResponse: IWireMockResponse = {
          status: 201,
        };
        await wiremock.register(apiIndexRequest, apiIndexResponse);

        const apiSyncJobEnv = {
          ['SNOW_WHITE_API_SYNC_JOB_API-INDEX_BASE-URL']:
            'http://wiremock:8080',
          ['SNOW_WHITE_API_SYNC_JOB_BACKSTAGE_BASE-URL']:
            'http://wiremock:8080',
          ['SNOW_WHITE_API_SYNC_JOB_MINIO_ENDPOINT']: 'http://minio:9000',
          ['SNOW_WHITE_API_SYNC_JOB_MINIO_BUCKET-NAME']: 'api-sync-job-apptest',
          ['SNOW_WHITE_API_SYNC_JOB_MINIO_INIT-BUCKET']: true,
          ['SNOW_WHITE_API_SYNC_JOB_MINIO_ACCESS-KEY']: 'minioadmin',
          ['SNOW_WHITE_API_SYNC_JOB_MINIO_SECRET-KEY']: 'minioadmin',
        };

        await invokeApiSyncJobWithDocker(apiSyncJobEnv);

        expect(
          await wiremock.getRequestsForAPI('POST', ingestApiEndpoint),
        ).toHaveLength(ITEMS_TO_INDEX);
      },
      EXPECTED_TIME,
    );
  });

  describe('service interface repository', () => {
    const EXPECTED_TIME = 60_000;

    it(
      `should process ${ITEMS_TO_INDEX} OpenAPI specifications in ${EXPECTED_TIME}ms`,
      async () => {
        const sirRequest: IWireMockRequest = {
          method: 'GET',
          endpoint: indexUri,
        };
        const sirResponse: IWireMockResponse = {
          status: 200,
          body: createSirApiIndex(),
        };
        await wiremock.register(sirRequest, sirResponse);

        const apiIndexExistsRequest: IWireMockRequest = {
          method: 'GET',
          endpoint:
            '/api/rest/v1/apis/example-application/Swagger%20Petstore/1.0.0/exists',
        };
        const apiIndexExistsResponse: IWireMockResponse = {
          status: 404,
        };
        await wiremock.register(apiIndexExistsRequest, apiIndexExistsResponse);

        const ingestApiEndpoint = '/api/rest/v1/apis';
        const apiIndexRequest: IWireMockRequest = {
          method: 'POST',
          endpoint: ingestApiEndpoint,
        };
        const apiIndexResponse: IWireMockResponse = {
          status: 201,
        };
        await wiremock.register(apiIndexRequest, apiIndexResponse);

        const apiSyncJobEnv = {
          ['SNOW_WHITE_API_SYNC_JOB_API-INDEX_BASE-URL']:
            'http://wiremock:8080',
          ['SNOW_WHITE_API_SYNC_JOB_SERVICE-INTERFACE_BASE-URL']:
            'http://wiremock:8080',
          ['SNOW_WHITE_API_SYNC_JOB_SERVICE-INTERFACE_INDEX-URI']: indexUri,
        };

        await invokeApiSyncJobWithDocker(apiSyncJobEnv);

        expect(
          await wiremock.getRequestsForAPI('POST', ingestApiEndpoint),
        ).toHaveLength(ITEMS_TO_INDEX);
      },
      EXPECTED_TIME,
    );
  });
});
