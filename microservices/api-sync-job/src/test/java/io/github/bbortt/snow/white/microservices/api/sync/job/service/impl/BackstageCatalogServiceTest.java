/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.UNLOADED;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.commons.openapi.OpenApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.EntitiesQueryResponse;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.Entity;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.EntityMeta;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowingConsumer;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class BackstageCatalogServiceTest {

  @Mock
  private ApiSyncJobProperties.BackstageProperties backstagePropertiesMock;

  @Mock
  private EntityApi backstageEntityApiMock;

  @Mock
  private InformationExtractor informationExtractorMock;

  @Mock
  private JsonMapper jsonMapperMock;

  @Mock
  private OpenApiValidationService openApiValidationServiceMock;

  @Mock
  private OpenAPIV3Parser openAPIV3ParserMock;

  @Mock
  private MinioService minioServiceMock;

  @Nested
  class FetchApiIndex {

    private static @NonNull ThrowingConsumer<
      ApiInformation
    > assertOpenApiInformation() {
      return apiInformation ->
        assertThat(apiInformation).satisfies(
          i -> assertThat(i.getTitle()).isEqualTo("title"),
          i -> assertThat(i.getVersion()).isEqualTo("apiVersion"),
          i -> assertThat(i.getName()).isEqualTo("apiName"),
          i -> assertThat(i.getServiceName()).isEqualTo("serviceName"),
          i -> assertThat(i.getApiType()).isEqualTo(OPENAPI),
          i -> assertThat(i.getLoadStatus()).isEqualTo(UNLOADED)
        );
    }

    private BackstageCatalogService fixture;

    @BeforeEach
    void beforeEachSetup() {
      fixture = new BackstageCatalogService(
        backstagePropertiesMock,
        backstageEntityApiMock,
        jsonMapperMock,
        openApiValidationServiceMock,
        minioServiceMock,
        informationExtractorMock,
        openAPIV3ParserMock
      );
    }

    @Test
    void shouldReturnEmptySet_whenNoEntities() {
      getEntitiesByQuery(
        doReturn(new EntitiesQueryResponse().totalItems(ZERO)).when(
          backstageEntityApiMock
        )
      );

      Set<ApiInformation> result = fixture.fetchApiIndex();

      assertThat(result).isEmpty();

      getEntitiesByQuery(verify(backstageEntityApiMock));
      verifyNoMoreInteractions(backstageEntityApiMock);
    }

    @Test
    void shouldReturnMappedApiInformation_withCustomVersionAnnotation_andSuccessfulParsing() {
      var customAnnotation = "customAnnotation";
      doReturn(customAnnotation)
        .when(backstagePropertiesMock)
        .getCustomVersionAnnotation();

      getEntitiesByQuery(
        doReturn(new EntitiesQueryResponse().totalItems(ONE)).when(
          backstageEntityApiMock
        )
      );

      doReturn(
        new EntitiesQueryResponse().items(
          singletonList(
            new Entity().metadata(
              new EntityMeta().annotations(
                Map.of("customAnnotation", "customLocation1\ncustomLocation2")
              )
            )
          )
        )
      )
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          List.of("metadata.annotations", "spec.definition"),
          10,
          0,
          null,
          null,
          null,
          null,
          null
        );

      ArgumentCaptor<String> urlArgumentCaptor = captor();
      var swaggerParseResult = mock(SwaggerParseResult.class);
      doReturn(swaggerParseResult)
        .when(openAPIV3ParserMock)
        .readLocation(
          urlArgumentCaptor.capture(),
          anyList(),
          any(ParseOptions.class)
        );

      var openApiStringCaptor = addOpenApiInformationToSwaggerParseResult(
        swaggerParseResult
      );

      Set<ApiInformation> result = fixture.fetchApiIndex();

      assertThat(result)
        .hasSize(2)
        .allSatisfy(assertOpenApiInformation())
        .satisfiesOnlyOnce(apiInformation ->
          assertThat(apiInformation.getSourceUrl()).isEqualTo("customLocation1")
        )
        .satisfiesOnlyOnce(apiInformation ->
          assertThat(apiInformation.getSourceUrl()).isEqualTo("customLocation2")
        );

      assertThat(urlArgumentCaptor.getAllValues()).containsExactly(
        "customLocation1",
        "customLocation2"
      );

      assertThat(openApiStringCaptor.getValue())
        .isNotEmpty()
        .isEqualToIgnoringNewLines(
          // language=json
          """
          {"components":null,"extensions":{},"externalDocs":null,"info":{"contact":null,"description":null,"extensions":{},"license":null,"summary":null,"termsOfService":null,"title":"title","version":null},"jsonSchemaDialect":null,"openapi":null,"paths":null,"security":[],"servers":[],"tags":[],"webhooks":{}}"""
        );
    }

    @Test
    void shouldSkipSwaggerParseResult_withCustomVersionAnnotation() {
      var customAnnotation = "customAnnotation";
      doReturn(customAnnotation)
        .when(backstagePropertiesMock)
        .getCustomVersionAnnotation();

      getEntitiesByQuery(
        doReturn(new EntitiesQueryResponse().totalItems(ONE)).when(
          backstageEntityApiMock
        )
      );

      doReturn(
        new EntitiesQueryResponse().items(
          singletonList(
            new Entity().metadata(
              new EntityMeta().annotations(
                Map.of("customAnnotation", "customLocation1\ncustomLocation2")
              )
            )
          )
        )
      )
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          List.of("metadata.annotations", "spec.definition"),
          10,
          0,
          null,
          null,
          null,
          null,
          null
        );

      ArgumentCaptor<String> urlArgumentCaptor = captor();
      var swaggerParseResult = mock(SwaggerParseResult.class);
      doReturn(swaggerParseResult)
        .when(openAPIV3ParserMock)
        .readLocation(
          urlArgumentCaptor.capture(),
          anyList(),
          any(ParseOptions.class)
        );

      doReturn(singletonList("some message"))
        .when(swaggerParseResult)
        .getMessages();

      Set<ApiInformation> result = fixture.fetchApiIndex();

      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnMappedApiInformation_fromBackstage() {
      getEntitiesByQuery(
        doReturn(new EntitiesQueryResponse().totalItems(ONE)).when(
          backstageEntityApiMock
        )
      );

      var openApiDefinition = "openapi-definition";
      Map<String, Object> spec = Map.of(
        "type",
        "openapi",
        "definition",
        openApiDefinition
      );
      doReturn(
        new EntitiesQueryResponse().items(
          singletonList(new Entity().spec(spec))
        )
      )
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          List.of("metadata.annotations", "spec.definition"),
          10,
          0,
          null,
          null,
          null,
          null,
          null
        );

      var jsonNodeMock = mock(JsonNode.class);
      doReturn(jsonNodeMock).when(jsonNodeMock).get("definition");
      doReturn(openApiDefinition).when(jsonNodeMock).asString();

      doReturn(jsonNodeMock).when(jsonMapperMock).valueToTree(spec);

      var swaggerParseResultMock = mock(SwaggerParseResult.class);
      doReturn(swaggerParseResultMock)
        .when(openAPIV3ParserMock)
        .readContents(openApiDefinition);

      var openApiStringCaptor = addOpenApiInformationToSwaggerParseResult(
        swaggerParseResultMock
      );

      assertThat(openApiStringCaptor.getAllValues()).isEmpty();

      ArgumentCaptor<
        BackstageCatalogService.OpenAPIParameters
      > openAPIParametersArgumentCaptor = captor();
      var apiInformation = mock(ApiInformation.class);
      doReturn(apiInformation)
        .when(minioServiceMock)
        .storeBackstageApiEntity(openAPIParametersArgumentCaptor.capture());

      Set<ApiInformation> result = fixture.fetchApiIndex();
      assertThat(result).containsExactly(apiInformation);

      assertThat(openAPIParametersArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          openAPIParameters ->
            assertThat(openAPIParameters.swaggerParseResult()).isEqualTo(
              swaggerParseResultMock
            ),
          openAPIParameters ->
            assertThat(openAPIParameters.apiInformation()).satisfies(
              assertOpenApiInformation()
            )
        );
    }

    @Test
    void shouldSkipSwaggerParseErrors_fromBackstage() {
      getEntitiesByQuery(
        doReturn(new EntitiesQueryResponse().totalItems(ONE)).when(
          backstageEntityApiMock
        )
      );

      var openApiDefinition = "openapi-definition";
      Map<String, Object> spec = Map.of(
        "type",
        "openapi",
        "definition",
        openApiDefinition
      );
      doReturn(
        new EntitiesQueryResponse().items(
          singletonList(new Entity().spec(spec))
        )
      )
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          List.of("metadata.annotations", "spec.definition"),
          10,
          0,
          null,
          null,
          null,
          null,
          null
        );

      var jsonNodeMock = mock(JsonNode.class);
      doReturn(jsonNodeMock).when(jsonNodeMock).get("definition");
      doReturn(openApiDefinition).when(jsonNodeMock).asString();

      doReturn(jsonNodeMock).when(jsonMapperMock).valueToTree(spec);

      var swaggerParseResultMock = mock(SwaggerParseResult.class);
      doReturn(swaggerParseResultMock)
        .when(openAPIV3ParserMock)
        .readContents(openApiDefinition);

      doReturn(singletonList("some message"))
        .when(swaggerParseResultMock)
        .getMessages();

      Set<ApiInformation> result = fixture.fetchApiIndex();

      assertThat(result).isEmpty();
    }

    @Test
    void shouldSkipNonOpenAPISpecifications_fromBackstage() {
      getEntitiesByQuery(
        doReturn(new EntitiesQueryResponse().totalItems(ONE)).when(
          backstageEntityApiMock
        )
      );

      doReturn(
        new EntitiesQueryResponse().items(
          singletonList(new Entity().spec(Map.of("type", "not-openapi")))
        )
      )
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          List.of("metadata.annotations", "spec.definition"),
          10,
          0,
          null,
          null,
          null,
          null,
          null
        );

      Set<ApiInformation> result = fixture.fetchApiIndex();

      assertThat(result).isEmpty();
    }

    public static Stream<
      String
    > shouldThrowException_ifBackstageApiIndex_withoutMinIO_isBeingUsed() {
      return Stream.of(null, "", " ");
    }

    @MethodSource
    @ParameterizedTest
    void shouldThrowException_ifBackstageApiIndex_withoutMinIO_isBeingUsed(
      String customVersionAnnotation
    ) {
      setField(fixture, "minioService", null, MinioService.class);

      doReturn(customVersionAnnotation)
        .when(backstagePropertiesMock)
        .getCustomVersionAnnotation();

      assertThatThrownBy(() -> fixture.fetchApiIndex())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "MinIO connection is required when resolving Entities from Backstage!"
        );

      verifyNoMoreInteractions(backstageEntityApiMock);
    }

    @Test
    void shouldSkipNullSpecs() {
      // That's two pages that should be fetched
      ArgumentCaptor<List<String>> fieldsArgumentCaptor = captor();
      ArgumentCaptor<Integer> limitArgumentCaptor = captor();
      ArgumentCaptor<Integer> offsetArgumentCaptor = captor();

      doReturn(
        new EntitiesQueryResponse()
          .items(emptyList())
          .totalItems(BigDecimal.valueOf(20))
      )
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          fieldsArgumentCaptor.capture(),
          limitArgumentCaptor.capture(),
          offsetArgumentCaptor.capture(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );

      Set<ApiInformation> result = fixture.fetchApiIndex();

      assertThat(result).isEmpty();

      assertThat(fieldsArgumentCaptor.getAllValues())
        .hasSize(3)
        .flatExtracting(values -> Objects.isNull(values) ? List.of() : values)
        .containsExactly(
          "metadata.annotations",
          "spec.definition",
          "metadata.annotations",
          "spec.definition"
        );
      assertThat(limitArgumentCaptor.getAllValues()).containsExactly(0, 10, 10);
      assertThat(offsetArgumentCaptor.getAllValues()).containsExactly(
        null,
        0,
        10
      );
    }

    private void getEntitiesByQuery(EntityApi entityApi) {
      entityApi.getEntitiesByQuery(null, 0, null, null, null, null, null, null);
    }

    private ArgumentCaptor<String> addOpenApiInformationToSwaggerParseResult(
      SwaggerParseResult swaggerParseResult
    ) {
      var openAPIMock = mock(OpenAPI.class);
      doReturn(openAPIMock).when(swaggerParseResult).getOpenAPI();

      var info = mock(Info.class);
      doReturn("title").when(info).getTitle();
      doReturn(info).when(openAPIMock).getInfo();

      ArgumentCaptor<String> openApiStringCaptor = captor();
      doReturn(new OpenApiInformation("apiName", "apiVersion", "serviceName"))
        .when(informationExtractorMock)
        .extractFromOpenApi(openApiStringCaptor.capture());

      return openApiStringCaptor;
    }
  }

  @Nested
  class ValidateApiInformation {

    @ParameterizedTest
    @EnumSource(ParsingMode.class)
    void validatesApiUsingService(ParsingMode parsingMode) {
      doReturn(parsingMode).when(backstagePropertiesMock).getParsingMode();

      var fixture = new BackstageCatalogService(
        backstagePropertiesMock,
        backstageEntityApiMock,
        jsonMapperMock,
        openApiValidationServiceMock,
        minioServiceMock
      );

      var apiInformation = mock(ApiInformation.class);
      fixture.validateApiInformation(apiInformation);

      verify(openApiValidationServiceMock).validateApiInformationFromIndex(
        apiInformation,
        parsingMode
      );
    }
  }

  @Nested
  class OpenAPIParameters {

    @Test
    void constructorWith_SwaggerParseResult() {
      assertThat(
        new BackstageCatalogService.OpenAPIParameters(
          mock(SwaggerParseResult.class)
        )
      ).hasAllNullFieldsOrPropertiesExcept("swaggerParseResult");
    }

    @Test
    void constructorWith_locationAndSwaggerParseResult() {
      assertThat(
        new BackstageCatalogService.OpenAPIParameters(
          "sourceUrl",
          mock(SwaggerParseResult.class)
        )
      ).hasNoNullFieldsOrPropertiesExcept("apiInformation");
    }

    @Test
    void allArgsConstructor() {
      assertThat(
        new BackstageCatalogService.OpenAPIParameters(
          "sourceUrl",
          mock(SwaggerParseResult.class),
          mock(ApiInformation.class)
        )
      ).hasNoNullFieldsOrProperties();
    }
  }
}
