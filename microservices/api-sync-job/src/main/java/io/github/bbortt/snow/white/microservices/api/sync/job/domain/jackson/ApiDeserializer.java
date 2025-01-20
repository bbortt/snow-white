package io.github.bbortt.snow.white.microservices.api.sync.job.domain.jackson;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.ApiType.apiType;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi.OpenApiProperties.OAS_INFO_TITLE;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi.OpenApiProperties.OAS_INFO_VERSION;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi.OpenApiProperties.OAS_TYPE;
import static java.lang.String.format;
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
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.Api;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ApiProperty;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.SimpleRequiredApiProperty;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogException;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

public class ApiDeserializer extends StdDeserializer<Api> {

  private static final String KEY_PROPERTY = "key";
  private static final String VALUE_PROPERTY = "value";
  private static final String PROPERTIES_PROPERTY = "properties";
  public static final String SOURCE_PROPERTY = "source";

  private final ApiProperty apiNameProperty;
  private final ApiProperty otelServiceNameProperty;

  public ApiDeserializer(ApiSyncJobProperties apiSyncJobProperties) {
    super(Api.class);
    this.apiNameProperty = new SimpleRequiredApiProperty(
      apiSyncJobProperties.getServiceInterface().getApiNameProperty()
    );
    this.otelServiceNameProperty = new SimpleRequiredApiProperty(
      apiSyncJobProperties.getServiceInterface().getOtelServiceNameProperty()
    );
  }

  @Override
  public Api deserialize(
    JsonParser parser,
    DeserializationContext deserializationContext
  ) throws IOException {
    var apiBuilder = Api.builder();

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
    Api.ApiBuilder apiBuilder,
    JsonNode node
  ) throws JsonParseException {
    ArrayNode properties = (ArrayNode) node.get(PROPERTIES_PROPERTY);
    if (properties != null) {
      Map<String, String> propertyLookup = createPropertyLookup(properties);

      try {
        assignProperty(apiBuilder::title, OAS_INFO_TITLE, propertyLookup);
        assignProperty(apiBuilder::version, OAS_INFO_VERSION, propertyLookup);

        assignProperty(apiBuilder::name, apiNameProperty, propertyLookup);
        assignProperty(
          apiBuilder::otelServiceName,
          otelServiceNameProperty,
          propertyLookup
        );

        assignProperty(
          prop -> apiBuilder.apiType(apiType(prop)),
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
