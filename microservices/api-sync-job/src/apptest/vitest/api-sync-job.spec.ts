/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import {
  BodyType,
  EndpointFeature,
  IWireMockRequest,
  IWireMockResponse,
  MatchingAttributes,
  WireMock,
} from 'wiremock-captain';
import { execa } from 'execa';

const containerRegistry = process.env.CONTAINER_REGISTRY ?? 'ghcr.io';
const imageTag = process.env.IMAGE_TAG ?? '1.0.0-SNAPSHOT';

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

const createDefaultStubsAndInvokeApiSyncJob = async (
  stubForAqlQueryPost: (
    fileExtensionPattern: string,
    resultsTemplate: string,
  ) => Promise<void>,
  indexItems: string[],
  stubForIndexApiPost: (ingestApiEndpoint: string) => Promise<void>,
  ARTIFACTORY_BEARER_TOKEN: string,
): Promise<string> => {
  await stubForAqlQueryPost('*.json', indexItems.join(','));
  await stubForAqlQueryPost('*.yml', '');
  await stubForAqlQueryPost('*.yaml', '');

  const ingestApiEndpoint = '/api/rest/v1/apis';
  await stubForIndexApiPost(ingestApiEndpoint);

  const apiSyncJobEnv = {
    ['SNOW_WHITE_API_SYNC_JOB_API-INDEX_BASE-URL']: 'http://wiremock:8080',
    ['SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_BASE-URL']:
      'http://wiremock:8080/artifactory',
    ['SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_ACCESS-TOKEN']:
      ARTIFACTORY_BEARER_TOKEN,
    ['SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_REPOSITORY']:
      'snow-white-generic-local',
  };

  await invokeApiSyncJobWithDocker(apiSyncJobEnv);

  return ingestApiEndpoint;
};

describe('API Sync Job', () => {
  const wiremockEndpoint =
    process.env.WIREMOCK_ENDPOINT ?? 'http://localhost:9000';
  const wiremock = new WireMock(wiremockEndpoint);

  beforeEach(async () => {
    await wiremock.clearAll();
  });

  describe('artifactory', () => {
    // localhost is way slower (usually) compared to GitHub actions!
    const LOCALHOST_OVERHEAD = process.env.CI ? 0 : 60_000;

    // JVM + OTel-agent cold start alone takes ~20-45s in the resource-constrained (--cpus=0.5 --memory=512m) container,
    // before any actual sync work happens - so even a tiny batch needs a generous budget.
    const SMALL_BATCH_TIME = LOCALHOST_OVERHEAD + 60_000;
    // The 500-item batch additionally has to download, parse and publish every item (maxParallelSyncTasks: 3),
    // well beyond the fixed JVM startup cost above.
    const LARGE_BATCH_TIME = LOCALHOST_OVERHEAD + 90_000;

    const ARTIFACTORY_BEARER_TOKEN = 'random-bearer-token';

    const stubForAqlQueryPost = async (
      fileExtensionPattern: string,
      resultsTemplate: string,
    ): Promise<void> => {
      const aqlQueryPost: IWireMockRequest = {
        body: `items.find({"$or":[{"$and":[{"repo":"snow-white-generic-local","path":{"$match":"*"},"name":{"$match":"${fileExtensionPattern}"}}]}]}).include("name","repo","path","actual_md5","actual_sha1","size","type","modified","created","property")`,
        endpoint: '/artifactory/api/search/aql',
        headers: {
          Authorization: 'Bearer ' + ARTIFACTORY_BEARER_TOKEN,
        },
        method: 'POST',
      };

      const aqlQueryPostResponse: IWireMockResponse = {
        body: JSON.parse(
          // language=json
          `{
            "results": [
              ${resultsTemplate}
            ]
          }`,
        ),
        headers: {
          'Content-Type': 'application/json',
        },
        status: 200,
      };

      await wiremock.register(aqlQueryPost, aqlQueryPostResponse, {
        requestHeaderFeatures: {
          Authorization: MatchingAttributes.EqualTo,
        },
        requestBodyFeature: MatchingAttributes.EqualTo,
      });
    };

    const stubForArtefactGet = async (
      fileName: string,
      index: number,
      responseBody: object,
    ): Promise<void> => {
      const artefactGet: IWireMockRequest = {
        endpoint: `/artifactory/snow-white-generic-local/${index}/${fileName}`,
        headers: {
          Authorization: 'Bearer ' + ARTIFACTORY_BEARER_TOKEN,
        },
        method: 'GET',
      };

      const artefactGetResponse: IWireMockResponse = {
        body: responseBody,
        headers: {
          'Content-Type': 'application/json',
        },
        status: 200,
      };

      await wiremock.register(artefactGet, artefactGetResponse, {
        requestHeaderFeatures: {
          Authorization: MatchingAttributes.EqualTo,
        },
      });
    };

    const stubForFileInfoGet = async (
      fileName: string,
      index: number,
    ): Promise<void> => {
      const artefactGet: IWireMockRequest = {
        endpoint: `/artifactory/api/storage/snow-white-generic-local/${index}/${fileName}`,
        headers: {
          Authorization: 'Bearer ' + ARTIFACTORY_BEARER_TOKEN,
        },
        method: 'GET',
      };

      const artefactGetResponse: IWireMockResponse = {
        body: {
          downloadUri: `http://localhost:3000/artifactory/snow-white-generic-local/${index}/${fileName}`,
        },
        headers: {
          'Content-Type': 'application/json',
        },
        status: 200,
      };

      await wiremock.register(artefactGet, artefactGetResponse, {
        requestHeaderFeatures: {
          Authorization: MatchingAttributes.EqualTo,
        },
      });
    };

    const stubForApiExistsGet = async (
      serviceName: string,
      apiName: string,
      index: number,
      status: number = 404,
    ): Promise<void> => {
      const apiIndexExistsRequest: IWireMockRequest = {
        method: 'GET',
        endpoint: `/api/rest/v1/apis/${serviceName}/${apiName}/${index}.0.0/exists`,
      };

      const apiIndexExistsResponse: IWireMockResponse = {
        status,
      };

      // The default "url" match requires an exact match of the full URL including the query
      // string, but the client always appends "?includePrereleases=false" - match on the path
      // alone so the query string doesn't cause a silent match failure (falling back to
      // WireMock's default 404, which happens to look correct for the "not indexed" case).
      await wiremock.register(apiIndexExistsRequest, apiIndexExistsResponse, {
        requestEndpointFeature: EndpointFeature.UrlPath,
      });
    };

    const stubForRawArtefactGet = async (
      fileName: string,
      index: number,
      rawBody: string,
    ): Promise<void> => {
      const artefactGet: IWireMockRequest = {
        endpoint: `/artifactory/snow-white-generic-local/${index}/${fileName}`,
        headers: {
          Authorization: 'Bearer ' + ARTIFACTORY_BEARER_TOKEN,
        },
        method: 'GET',
      };

      const artefactGetResponse: IWireMockResponse = {
        body: rawBody,
        status: 200,
      };

      await wiremock.register(artefactGet, artefactGetResponse, {
        requestHeaderFeatures: {
          Authorization: MatchingAttributes.EqualTo,
        },
        responseBodyType: BodyType.Body,
      });
    };

    const stubForIndexApiPost = async (
      ingestApiEndpoint: string,
    ): Promise<void> => {
      const apiIndexRequest: IWireMockRequest = {
        method: 'POST',
        endpoint: ingestApiEndpoint,
      };

      const apiIndexResponse: IWireMockResponse = {
        status: 201,
      };

      await wiremock.register(apiIndexRequest, apiIndexResponse);
    };

    it(
      'should not re-publish an API that is already indexed',
      async () => {
        const totalItems = 3;
        const alreadyIndexedIndex = 2;

        const indexItems = [];
        for (let i = 1; i <= totalItems; i++) {
          indexItems.push(
            `{"repo": "snow-white-generic-local", "path": "/${i}/", "name": "petstore.json"}`,
          );

          await stubForArtefactGet(
            'petstore.json',
            i,
            JSON.parse(
              // language=json
              `{
                "openapi": "3.1.2",
                "info": {
                  "title": "Petstore API",
                  "version": "${i}.0.0",
                  "extensions": {
                    "x-service-name": "example-application"
                  }
                },
                "paths": {}
              }`,
            ),
          );

          await stubForFileInfoGet('petstore.json', i);

          await stubForApiExistsGet(
            'example-application',
            'Petstore%20API',
            i,
            i === alreadyIndexedIndex ? 200 : 404,
          );
        }

        const ingestApiEndpoint = await createDefaultStubsAndInvokeApiSyncJob(
          stubForAqlQueryPost,
          indexItems,
          stubForIndexApiPost,
          ARTIFACTORY_BEARER_TOKEN,
        );

        // Only the 2 not-yet-indexed APIs should have been published - the already-indexed one must be skipped without a redundant ingest call.
        expect(
          await wiremock.getRequestsForAPI('POST', ingestApiEndpoint),
        ).toHaveLength(totalItems - 1);
      },
      SMALL_BATCH_TIME,
    );

    it(
      'should skip an unparseable spec but still publish its valid siblings',
      async () => {
        await stubForArtefactGet(
          'valid.json',
          1,
          JSON.parse(
            // language=json
            `{
              "openapi": "3.1.2",
              "info": {
                "title": "Valid API",
                "version": "1.0.0",
                "extensions": {
                  "x-service-name": "example-application"
                }
              },
              "paths": {}
            }`,
          ),
        );
        await stubForFileInfoGet('valid.json', 1);
        await stubForApiExistsGet('example-application', 'Valid%20API', 1);

        // Not a valid OpenAPI document - the API sync job runs in its default
        // graceful parsing mode, so this must be skipped rather than aborting
        // the whole batch.
        await stubForRawArtefactGet(
          'invalid.json',
          2,
          'this is not a valid OpenAPI document',
        );
        await stubForFileInfoGet('invalid.json', 2);

        const indexItems = [
          '{"repo": "snow-white-generic-local", "path": "/1/", "name": "valid.json"}',
          '{"repo": "snow-white-generic-local", "path": "/2/", "name": "invalid.json"}',
        ];

        const ingestApiEndpoint = await createDefaultStubsAndInvokeApiSyncJob(
          stubForAqlQueryPost,
          indexItems,
          stubForIndexApiPost,
          ARTIFACTORY_BEARER_TOKEN,
        );

        // The job must complete successfully (docker exits 0 - execa would
        // otherwise reject above) and still publish the valid sibling spec.
        expect(
          await wiremock.getRequestsForAPI('POST', ingestApiEndpoint),
        ).toHaveLength(1);
      },
      SMALL_BATCH_TIME,
    );

    for (const { items, batchTime } of [
      { items: 500, batchTime: SMALL_BATCH_TIME },
      { items: 1000, batchTime: LARGE_BATCH_TIME },
    ]) {
      it(
        `should process ${items} OpenAPI specifications in ${batchTime}ms`,
        async () => {
          const indexItems = [];
          for (let i = 1; i <= items; i++) {
            indexItems.push(
              `{"repo": "snow-white-generic-local", "path": "/${i}/", "name": "petstore.json"}`,
            );

            await stubForArtefactGet(
              'petstore.json',
              i,
              JSON.parse(
                // language=json
                `{
                "openapi": "3.1.2",
                "info": {
                  "title": "Petstore API",
                  "version": "${i}.0.0",
                  "extensions": {
                    "x-service-name": "example-application"
                  }
                },
                  "paths": {
                    
                  }
                }`,
              ),
            );

            await stubForFileInfoGet('petstore.json', i);

            await stubForApiExistsGet(
              'example-application',
              'Petstore%20API',
              i,
            );

            await stubForAqlQueryPost('*.json', indexItems.join(','));
            await stubForAqlQueryPost('*.yml', '');
            await stubForAqlQueryPost('*.yaml', '');
          }

          const ingestApiEndpoint = await createDefaultStubsAndInvokeApiSyncJob(
            stubForAqlQueryPost,
            indexItems,
            stubForIndexApiPost,
            ARTIFACTORY_BEARER_TOKEN,
          );

          expect(
            await wiremock.getRequestsForAPI('POST', ingestApiEndpoint),
          ).toHaveLength(items);
        },
        batchTime,
      );
    }
  });
});
