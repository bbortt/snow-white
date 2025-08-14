/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.jackson;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.apiType;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi.OpenApiProperties.OAS_INFO_TITLE;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi.OpenApiProperties.OAS_INFO_VERSION;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi.OpenApiProperties.OAS_TYPE;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static org.springframework.util.StringUtils.hasText;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ApiProperty;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.SimpleRequiredApiProperty;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.exception.ApiCatalogException;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

public class ApiInformationDeserializer
  extends StdDeserializer<ApiInformation> {

  private static final String KEY_PROPERTY = "key";
  private static final String VALUE_PROPERTY = "value";
  private static final String PROPERTIES_PROPERTY = "properties";
  public static final String SOURCE_PROPERTY = "source";

  private final transient ApiProperty serviceNameProperty;

  private transient @Nullable ApiProperty apiNameProperty;
  private transient @Nullable ApiProperty apiVersionProperty;

  public ApiInformationDeserializer(ApiSyncJobProperties apiSyncJobProperties) {
    super(ApiInformation.class);
    var serviceInterface = apiSyncJobProperties.getServiceInterface();

    this.serviceNameProperty = new SimpleRequiredApiProperty(
      serviceInterface.getServiceNameProperty()
    );

    if (hasText(serviceInterface.getApiNameProperty())) {
      this.apiNameProperty = new SimpleRequiredApiProperty(
        serviceInterface.getApiNameProperty()
      );
    }

    if (hasText(serviceInterface.getApiVersionProperty())) {
      this.apiVersionProperty = new SimpleRequiredApiProperty(
        serviceInterface.getApiVersionProperty()
      );
    }
  }

  @Override
  public ApiInformation deserialize(
    JsonParser parser,
    DeserializationContext deserializationContext
  ) throws IOException {
    var apiBuilder = ApiInformation.builder();

    JsonNode node = parser.getCodec().readTree(parser);
    JsonNode sourceNode = node.get(SOURCE_PROPERTY);
    if (sourceNode != null) {
      apiBuilder.sourceUrl(sourceNode.asText());
    }

    deserializeProperties(parser, apiBuilder, node);

    return apiBuilder.build();
  }

  private void deserializeProperties(
    JsonParser parser,
    ApiInformation.ApiInformationBuilder apiInformationBuilder,
    JsonNode node
  ) throws JsonParseException {
    if (node.has(PROPERTIES_PROPERTY)) {
      var propertyLookup = createPropertyLookup(
        (ArrayNode) node.get(PROPERTIES_PROPERTY)
      );

      try {
        assignProperty(
          apiInformationBuilder::title,
          OAS_INFO_TITLE,
          propertyLookup
        );
        assignProperty(
          apiInformationBuilder::version,
          OAS_INFO_VERSION,
          propertyLookup
        );

        if (nonNull(apiNameProperty)) {
          assignProperty(
            apiInformationBuilder::name,
            apiNameProperty,
            propertyLookup
          );
        } else {
          assignProperty(
            apiInformationBuilder::name,
            OAS_INFO_TITLE,
            propertyLookup
          );
        }

        if (nonNull(apiVersionProperty)) {
          assignProperty(
            apiInformationBuilder::version,
            apiVersionProperty,
            propertyLookup
          );
        }

        assignProperty(
          apiInformationBuilder::serviceName,
          serviceNameProperty,
          propertyLookup
        );

        assignProperty(
          prop -> apiInformationBuilder.apiType(apiType(prop)),
          OAS_TYPE,
          propertyLookup
        );
      } catch (ApiCatalogException e) {
        throw new JsonParseException(parser, e.getMessage(), e);
      }
    }
  }

  private static Map<String, String> createPropertyLookup(
    ArrayNode properties
  ) {
    Iterable<JsonNode> nodeIterable = properties::elements;
    return stream(nodeIterable.spliterator(), false)
      .filter(propertyNode -> propertyNode.get(VALUE_PROPERTY) != null)
      .collect(
        toMap(
          propertyNode -> propertyNode.get(KEY_PROPERTY).asText(),
          propertyNode -> propertyNode.get(VALUE_PROPERTY).asText()
        )
      );
  }

  private void assignProperty(
    Consumer<String> propertyConsumer,
    ApiProperty apiProperty,
    Map<String, String> propertyLookup
  ) throws ApiCatalogException {
    String propertyValue = propertyLookup.get(apiProperty.getPropertyName());
    if (apiProperty.isRequired() && !hasText(propertyValue)) {
      throw new ApiCatalogException(
        format(
          "Mandatory property '%s' is not provided by API!",
          apiProperty.getPropertyName()
        )
      );
    }
    propertyConsumer.accept(propertyValue);
  }
}
