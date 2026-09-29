/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config.validation;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static java.lang.String.format;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiSyncJobPropertiesValidator {

  public ApiSyncJobPropertiesValidator(
    ApiSyncJobProperties apiSyncJobProperties
  ) {
    Map<String, String> properties = new HashMap<>();
    properties.put(
      ApiSyncJobProperties.ApiIndexProperties.BASE_URL_PROPERTY_NAME,
      apiSyncJobProperties.getApiIndex().getBaseUrl()
    );

    var artifactory = apiSyncJobProperties.getArtifactory();
    properties.put(
      ApiSyncJobProperties.ArtifactoryProperties.BASE_URL_PROPERTY_NAME,
      artifactory.getBaseUrl()
    );
    properties.put(
      ApiSyncJobProperties.ArtifactoryProperties.REPOSITORY_PROPERTY_NAME,
      artifactory.getRepository()
    );

    assertRequiredProperties(properties);

    assertPositive(
      ApiSyncJobProperties.MAX_PARALLEL_SYNC_TASKS_PROPERTY_NAME,
      apiSyncJobProperties.getMaxParallelSyncTasks()
    );
    assertPositive(
      ApiSyncJobProperties.WORK_QUEUE_CAPACITY_PROPERTY_NAME,
      apiSyncJobProperties.getWorkQueueCapacity()
    );

    logger.info(
      "Configuration: {}",
      toMaskedJsonRepresentation(apiSyncJobProperties)
    );
  }

  /**
   * Both knobs sit behind an executor and a queue that reject anything below
   * one with an exception naming neither the value nor the property - one on
   * every cycle, the other at startup. Refusing them here names the property
   * instead.
   */
  private static void assertPositive(String propertyName, Integer value) {
    if (value < 1) {
      throw new IllegalArgumentException(
        format(
          "Property '%s' must be greater than 0, but was: %s!",
          propertyName,
          value
        )
      );
    }
  }
}
