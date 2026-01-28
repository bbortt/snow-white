/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import {
  BodyType,
  IWireMockRequest,
  IWireMockResponse,
  MatchingAttributes,
  WireMock,
} from 'wiremock-captain';
import { execa } from 'execa';

const ITEMS_TO_INDEX = 500;

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

describe('API Sync Job', () => {
  const wiremockEndpoint =
    process.env.WIREMOCK_ENDPOINT ?? 'http://localhost:9000';
  const wiremock = new WireMock(wiremockEndpoint);

  beforeEach(async () => {
    await wiremock.clearAll();
  });

  describe('artifactory', () => {
    const EXPECTED_TIME = 60_000;

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
    ): Promise<void> => {
      const apiIndexExistsRequest: IWireMockRequest = {
        method: 'GET',
        endpoint: `/api/rest/v1/apis/${serviceName}/${apiName}/${index}.0.0/exists`,
      };

      const apiIndexExistsResponse: IWireMockResponse = {
        status: 404,
      };

      await wiremock.register(apiIndexExistsRequest, apiIndexExistsResponse);
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
      `should process ${ITEMS_TO_INDEX} OpenAPI specifications in ${EXPECTED_TIME}ms`,
      async () => {
        const indexItems = [];
        for (let i = 1; i <= ITEMS_TO_INDEX; i++) {
          indexItems.push(
            `{"repo": "snow-white-generic-local", "path": "/${i}/", "name": "petstore.json"}`,
          );

          await stubForArtefactGet(
            'petstore.json',
            i,
            JSON.parse(
              // language=json
              `{
              "openapi": "3.0.0",
              "info": {
              "title": "Petstore API",
                "version": "${i}.0.0",
                "extensions": {
                  "x-service-name": "example-application"
                }
              }
            }`,
            ),
          );

          await stubForFileInfoGet('petstore.json', i);

          await stubForApiExistsGet('example-application', 'Petstore%20API', i);

          await stubForAqlQueryPost('*.json', indexItems.join(','));
          await stubForAqlQueryPost('*.yml', '');
          await stubForAqlQueryPost('*.yaml', '');
        }

        await stubForAqlQueryPost('*.json', indexItems.join(','));
        await stubForAqlQueryPost('*.yml', '');
        await stubForAqlQueryPost('*.yaml', '');

        const ingestApiEndpoint = '/api/rest/v1/apis';
        await stubForIndexApiPost(ingestApiEndpoint);

        const apiSyncJobEnv = {
          ['SNOW_WHITE_API_SYNC_JOB_API-INDEX_BASE-URL']:
            'http://wiremock:8080',
          ['SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_BASE-URL']:
            'http://wiremock:8080/artifactory',
          ['SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_ACCESS-TOKEN']:
            ARTIFACTORY_BEARER_TOKEN,
          ['SNOW_WHITE_API_SYNC_JOB_ARTIFACTORY_REPOSITORY']:
            'snow-white-generic-local',
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
