/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static io.github.bbortt.snow.white.toolkit.openapi.generator.SnowWhiteSpringServerGenerator.API_NAME_PROPERTY;
import static io.github.bbortt.snow.white.toolkit.openapi.generator.SnowWhiteSpringServerGenerator.API_VERSION_PROPERTY;
import static io.github.bbortt.snow.white.toolkit.openapi.generator.SnowWhiteSpringServerGenerator.SERVICE_NAME_PROPERTY;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.openapitools.codegen.CodegenType.SERVER;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.commons.openapi.OpenApiInformation;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

@ExtendWith({ MockitoExtension.class })
class SnowWhiteSpringServerGeneratorTest {

  private static final String DEFAULT_API_NAME = "info.title";
  private static final String DEFAULT_API_VERSION = "info.version";
  private static final String DEFAULT_SERVICE_NAME = "info.x-service-name";

  private static final String VALID_API_NAME = "Test API";
  private static final String VALID_API_VERSION = "1.0.0";
  private static final String VALID_SERVICE_NAME = "test-service";

  private static final String INPUT_SPEC =
    "SnowWhiteSpringServerGeneratorIT/valid-specification.yml";
  private static final String VALID_YAML = // language=yaml
    """
    info:
      title: Test API
      version: 1.0.0
      x-service-name: test-service
    """;

  @Mock
  private InformationExtractor informationExtractorMock;

  @Mock
  private YamlParser yamlParserMock;

  private SnowWhiteSpringServerGenerator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new SnowWhiteSpringServerGenerator();
    setField(
      fixture,
      "informationExtractor",
      informationExtractorMock,
      InformationExtractor.class
    );
    setField(fixture, "yamlParser", yamlParserMock, YamlParser.class);
  }

  @Test
  void overridesSpringGeneratorName() {
    assertThat(fixture.getName()).isEqualTo(
      SnowWhiteSpringServerGenerator.NAME
    );
  }

  @Test
  void overridesSpringGeneratorHelp() {
    assertThat(fixture.getHelp()).isEqualTo(
      "Enhances the OpenAPI Spring generator with snow-white specific information."
    );
  }

  @Test
  void isServerGenerator() {
    assertThat(fixture.getTag()).isEqualTo(SERVER);
  }

  @Nested
  class Constructor {

    @Test
    void initializesDefaultProperties() {
      assertThat(fixture).satisfies(
        f -> assertThat(f.apiName).isEqualTo(DEFAULT_API_NAME),
        f -> assertThat(f.apiVersion).isEqualTo(DEFAULT_API_VERSION),
        f -> assertThat(f.serviceName).isEqualTo(DEFAULT_SERVICE_NAME)
      );
    }

    @Test
    void initializesCliOptions() {
      assertThat(fixture)
        .extracting("cliOptions")
        .asInstanceOf(LIST)
        .anySatisfy(option ->
          assertThat(option)
            .asInstanceOf(type(CliOption.class))
            .extracting(CliOption::getOpt)
            .isEqualTo(API_NAME_PROPERTY)
        )
        .anySatisfy(option ->
          assertThat(option)
            .asInstanceOf(type(CliOption.class))
            .extracting(CliOption::getOpt)
            .isEqualTo(SnowWhiteSpringServerGenerator.API_VERSION_PROPERTY)
        )
        .anySatisfy(option ->
          assertThat(option)
            .asInstanceOf(type(CliOption.class))
            .extracting(CliOption::getOpt)
            .isEqualTo(SnowWhiteSpringServerGenerator.SERVICE_NAME_PROPERTY)
        );
    }
  }

  @Test
  void settersUpdateJsonPaths() {
    String newApiName = "custom.api.name";
    String newApiVersion = "custom.api.version";
    String newServiceName = "custom.service.name";

    fixture.setApiName(newApiName);
    fixture.setApiVersion(newApiVersion);
    fixture.setServiceName(newServiceName);

    assertThat(fixture).satisfies(
      f -> assertThat(f.apiName).isEqualTo(newApiName),
      f -> assertThat(f.apiVersion).isEqualTo(newApiVersion),
      f -> assertThat(f.serviceName).isEqualTo(newServiceName)
    );
  }

  @Nested
  class ProcessOpts {

    @Test
    void withoutAdditionalArgumentsRetainsDefaults() {
      fixture.processOpts();

      assertThat(fixture).satisfies(
        f -> assertThat(f.apiName).isEqualTo(DEFAULT_API_NAME),
        f -> assertThat(f.apiVersion).isEqualTo(DEFAULT_API_VERSION),
        f -> assertThat(f.serviceName).isEqualTo(DEFAULT_SERVICE_NAME)
      );
    }

    @Test
    void processesApiNameProperty() {
      fixture.additionalProperties().put(API_NAME_PROPERTY, VALID_API_NAME);

      fixture.processOpts();

      assertThat(fixture).satisfies(
        f -> assertThat(f.apiName).isEqualTo(VALID_API_NAME),
        f -> assertThat(f.apiVersion).isEqualTo(DEFAULT_API_VERSION),
        f -> assertThat(f.serviceName).isEqualTo(DEFAULT_SERVICE_NAME)
      );
    }

    @Test
    void processesApiVersionProperty() {
      fixture
        .additionalProperties()
        .put(API_VERSION_PROPERTY, VALID_API_VERSION);

      fixture.processOpts();

      assertThat(fixture).satisfies(
        f -> assertThat(f.apiName).isEqualTo(DEFAULT_API_NAME),
        f -> assertThat(f.apiVersion).isEqualTo(VALID_API_VERSION),
        f -> assertThat(f.serviceName).isEqualTo(DEFAULT_SERVICE_NAME)
      );
    }

    @Test
    void processesServiceNameProperty() {
      fixture
        .additionalProperties()
        .put(SERVICE_NAME_PROPERTY, VALID_SERVICE_NAME);

      fixture.processOpts();

      assertThat(fixture).satisfies(
        f -> assertThat(f.apiName).isEqualTo(DEFAULT_API_NAME),
        f -> assertThat(f.apiVersion).isEqualTo(DEFAULT_API_VERSION),
        f -> assertThat(f.serviceName).isEqualTo(VALID_SERVICE_NAME)
      );
    }
  }

  @Nested
  class PostProcessOperationsWithModels {

    private static OperationsMap createOperationsMapWithSingleOperation(
      String operationId
    ) {
      var operationsMap = new OperationsMap();
      var operations = new OperationMap();
      List<CodegenOperation> operationList = new ArrayList<>();
      var operation = new CodegenOperation();
      operation.operationId = operationId;
      operation.tags = singletonList(new Tag());
      operationList.add(operation);
      operations.put("operation", operationList);
      operationsMap.setOperation(operations);
      operationsMap.setImports(emptyList());
      return operationsMap;
    }

    @Test
    void enhancesOperationsWithCompleteInformation() {
      var operationId = "testOperationId";

      var operationsMap = createOperationsMapWithSingleOperation(operationId);

      prepareOpenApiInformation(VALID_API_NAME);

      OperationsMap result = fixture.postProcessOperationsWithModels(
        operationsMap,
        List.of()
      );

      @SuppressWarnings("unchecked")
      List<CodegenOperation> resultOperations = (List<CodegenOperation>) result
        .getOperations()
        .get("operation");
      String expectedAnnotation = String.format(
        "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"%s\", apiName = \"%s\", apiVersion = \"%s\", operationId = \"%s\")",
        VALID_SERVICE_NAME,
        VALID_API_NAME,
        VALID_API_VERSION,
        operationId
      );
      assertThat(resultOperations)
        .hasSize(1)
        .first()
        .extracting(op ->
          op.vendorExtensions.get("x-operation-extra-annotation")
        )
        .isEqualTo(expectedAnnotation);
    }

    static Stream<String> enhancesOperationsWithoutOperationIDs() {
      return Stream.of(null, "", " ");
    }

    @MethodSource
    @ParameterizedTest
    void enhancesOperationsWithoutOperationIDs(String operationId) {
      var operationsMap = createOperationsMapWithSingleOperation(operationId);

      prepareOpenApiInformation(VALID_API_NAME);

      OperationsMap result = fixture.postProcessOperationsWithModels(
        operationsMap,
        List.of()
      );

      @SuppressWarnings("unchecked")
      List<CodegenOperation> resultOperations = (List<CodegenOperation>) result
        .getOperations()
        .get("operation");
      String expectedAnnotation = String.format(
        "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"%s\", apiName = \"%s\", apiVersion = \"%s\")",
        VALID_SERVICE_NAME,
        VALID_API_NAME,
        VALID_API_VERSION
      );
      assertThat(resultOperations)
        .hasSize(1)
        .first()
        .extracting(op ->
          op.vendorExtensions.get("x-operation-extra-annotation")
        )
        .isEqualTo(expectedAnnotation);
    }

    @Test
    void returnsUnmodifiedOperationsWithIncompleteInformation() {
      var operationsMap = new OperationsMap();
      operationsMap.setImports(emptyList());

      List<ModelMap> models = emptyList();

      prepareOpenApiInformation(null);

      OperationsMap result = fixture.postProcessOperationsWithModels(
        operationsMap,
        models
      );

      assertThat(result).isSameAs(operationsMap);
    }

    private void prepareOpenApiInformation(String validApiName) {
      fixture.setInputSpec(INPUT_SPEC);
      doReturn(VALID_YAML).when(yamlParserMock).readSpecToJson(INPUT_SPEC);

      var apiInfo = new OpenApiInformation(
        validApiName,
        VALID_API_VERSION,
        VALID_SERVICE_NAME
      );
      doReturn(apiInfo)
        .when(informationExtractorMock)
        .extractFromOpenApi(anyString());
    }
  }
}
