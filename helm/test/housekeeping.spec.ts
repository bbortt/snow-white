/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { renderHelmChart } from './render-helm-chart';
import { describe, expect, it } from 'vitest';
import { expectToHaveDefaultLabelsForMicroservice } from './helpers';

const getTemplateSpec = (cronJob: any) => {
  const { spec } = cronJob;
  expect(spec).toBeDefined();

  const { jobTemplate } = spec;
  expect(jobTemplate).toBeDefined();

  const jobSpec = jobTemplate.spec;
  expect(jobSpec).toBeDefined();

  const { template } = jobSpec;
  expect(template).toBeDefined();

  const templateSpec = template.spec;
  expect(templateSpec).toBeDefined();

  return templateSpec;
};

describe('Housekeeping Job', () => {
  const renderAndGetCronJob = async (manifests?: any[]) => {
    if (!manifests) {
      manifests = await renderHelmChart({
        chartPath: 'charts/snow-white',
      });
    }

    const housekeepingJob = manifests.find(
      (m) =>
        m.kind === 'CronJob' &&
        m.metadata.name === 'snow-white-housekeeping-job-test-release',
    );
    expect(housekeepingJob).toBeDefined();

    return housekeepingJob;
  };

  it('should be enabled by default', async () => {
    expect(renderAndGetCronJob()).toBeDefined();
  });

  it('can be disabled with values', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      values: {
        snowWhite: {
          housekeeping: {
            enabled: false,
          },
        },
      },
    });

    const housekeepingJob = manifests.find(
      (m) =>
        m.kind === 'CronJob' &&
        m.metadata.name.startsWith('test-release-housekeeping-job'),
    );
    expect(housekeepingJob).toBeUndefined();
  });

  it('should be kubernetes CronJob', async () => {
    const cronJob = await renderAndGetCronJob();

    expect(cronJob.apiVersion).toMatch('v1');
    expect(cronJob.kind).toMatch('CronJob');

    const { metadata } = cronJob;
    expect(metadata).toBeDefined();

    expect(metadata.name).toMatch('snow-white-housekeeping-job-test-release');
  });

  it('should have default labels', async () => {
    const cronJob = await renderAndGetCronJob();

    const { metadata } = cronJob;
    expect(metadata).toBeDefined();

    expectToHaveDefaultLabelsForMicroservice(
      metadata.labels,
      'housekeeping-job',
    );
  });

  it('should truncate long release name', async () => {
    const manifests = await renderHelmChart({
      chartPath: 'charts/snow-white',
      // 53 chars is the max length for Helm release names
      releaseName: 'very-long-test-release-name-that-exceeds-the-limit',
    });

    const cronJob = manifests.find(
      (m) =>
        m.kind === 'CronJob' &&
        m.metadata.name ===
          'snow-white-housekeeping-job-very-long-test-release-name-that-ex',
    );

    expect(cronJob).toBeDefined();
    expect(cronJob.metadata.name).toHaveLength(63);
  });

  const getJobTemplateSpec = (cronJob: any): any => {
    const { spec } = cronJob;
    expect(spec).toBeDefined();

    const { jobTemplate } = spec;
    expect(jobTemplate).toBeDefined();

    const templateSpec = jobTemplate.spec;
    expect(templateSpec).toBeDefined();
    return templateSpec;
  };

  it('should have default active deadline seconds', async () => {
    const cronJob = await renderAndGetCronJob();

    const templateSpec = getJobTemplateSpec(cronJob);
    expect(templateSpec.activeDeadlineSeconds).toBe(1800);
  });

  it('should include active deadline seconds from values', async () => {
    const activeDeadlineSeconds = 1234;

    const cronJob = await renderAndGetCronJob(
      await renderHelmChart({
        chartPath: 'charts/snow-white',
        values: {
          snowWhite: {
            housekeeping: {
              activeDeadlineSeconds,
            },
          },
        },
      }),
    );

    const templateSpec = getJobTemplateSpec(cronJob);
    expect(templateSpec.activeDeadlineSeconds).toBe(activeDeadlineSeconds);
  });

  describe('template metadata', () => {
    const getTemplateMetadata = (cronJob: any) => {
      const { spec } = cronJob;
      expect(spec).toBeDefined();

      const { jobTemplate } = spec;
      expect(jobTemplate).toBeDefined();

      const jobSpec = jobTemplate.spec;
      expect(jobSpec).toBeDefined();

      const { template } = jobSpec;
      expect(template).toBeDefined();

      const { metadata } = template;
      expect(metadata).toBeDefined();

      return metadata;
    };

    describe('annotations', () => {
      it('should not have any annotations by default', async () => {
        const metadata = await getTemplateMetadata(await renderAndGetCronJob());

        expect(metadata.annotations).toBeNull();
      });

      it('should include optional annotations', async () => {
        const podAnnotations = {
          foo: 'bar',
        };

        const metadata = await getTemplateMetadata(
          await renderAndGetCronJob(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  podAnnotations,
                },
              },
            }),
          ),
        );

        expect(metadata.annotations).toEqual(podAnnotations);
      });
    });

    it('should have default labels', async () => {
      const metadata = await getTemplateMetadata(await renderAndGetCronJob());

      expectToHaveDefaultLabelsForMicroservice(
        metadata.labels,
        'housekeeping-job',
      );
    });
  });

  describe('template spec', () => {
    describe('imagePullSecrets', () => {
      it('should have none by default', async () => {
        const cronJob = await renderAndGetCronJob();

        const { spec } = cronJob;
        expect(spec).toBeDefined();

        expect(spec.imagePullSecrets).toBeUndefined();
      });

      it('renders with custom image pull secret based on values', async () => {
        const token = 'something';
        const cronJob = await renderAndGetCronJob(
          await renderHelmChart({
            chartPath: 'charts/snow-white',
            values: {
              global: {
                imagePullSecrets: {
                  token,
                },
              },
            },
          }),
        );

        const templateSpec = getTemplateSpec(cronJob);
        expect(templateSpec.imagePullSecrets).toEqual({ token });
      });
    });
  });

  describe('containers', () => {
    it('should include housekeeping for report-coordinator-api', async () => {
      const templateSpec = getTemplateSpec(await renderAndGetCronJob());

      const { containers } = templateSpec;
      expect(containers).toHaveLength(1);

      expect(containers[0].name).toBe('report-coordinator-api');
    });

    describe('report-coordinator-api', () => {
      const renderAndGetHousekeepingJobContainer = async (
        manifests?: any[],
      ) => {
        const templateSpec = getTemplateSpec(
          await renderAndGetCronJob(manifests),
        );

        const { containers } = templateSpec;
        expect(containers).toBeDefined();

        return containers.find(
          (container) => container.name === 'report-coordinator-api',
        );
      };

      describe('image', () => {
        it('should be pulled from docker.io by default', async () => {
          const housekeepingJob = await renderAndGetHousekeepingJobContainer();

          expect(housekeepingJob.image).toBe(
            'docker.io/curlimages/curl:8.18.0',
          );
        });

        it('should adjust the image registry from values', async () => {
          const customRegistry = 'custom.registry';

          const housekeepingJob = await renderAndGetHousekeepingJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  housekeeping: {
                    image: { registry: customRegistry },
                  },
                },
              },
            }),
          );

          expect(housekeepingJob.image).toBe(
            'custom.registry/curlimages/curl:8.18.0',
          );
        });

        it('should adjust the image tag from values', async () => {
          const customTag = 'custom.tag';

          const housekeepingJob = await renderAndGetHousekeepingJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                snowWhite: {
                  housekeeping: {
                    image: { tag: customTag },
                  },
                },
              },
            }),
          );

          expect(housekeepingJob.image).toBe(
            'docker.io/curlimages/curl:custom.tag',
          );
        });
      });

      describe('imagePullPolicy', () => {
        it('should pull images if they are not present by default', async () => {
          const housekeepingJob = await renderAndGetHousekeepingJobContainer();

          expect(housekeepingJob.imagePullPolicy).toBe('Always');
        });

        it('should adjust the image pull policy from values', async () => {
          const imagePullPolicy = 'my.policy';

          const housekeepingJob = await renderAndGetHousekeepingJobContainer(
            await renderHelmChart({
              chartPath: 'charts/snow-white',
              values: {
                image: { pullPolicy: imagePullPolicy },
              },
            }),
          );

          expect(housekeepingJob.imagePullPolicy).toBe(imagePullPolicy);
        });
      });

      describe('args', () => {
        it('should call housekeeping endpoint', async () => {
          const housekeepingJob = await renderAndGetHousekeepingJobContainer();

          expect(housekeepingJob.args).toHaveLength(1);
          expect(housekeepingJob.args[0]).toBe(
            "curl -X 'POST' 'http://snow-white-report-coordinator-api-test-release.default.svc.cluster.local.:80/api/v1/housekeeping'",
          );
        });
      });

      describe('resources', () => {
        it('should be deployed with 64 MB memory by default', async () => {
          const housekeepingJob = await renderAndGetHousekeepingJobContainer();

          expect(housekeepingJob.resources.limits.memory).toBe('64Mi');
          expect(housekeepingJob.resources.requests.memory).toBe('64Mi');
        });
      });
    });
  });
});
