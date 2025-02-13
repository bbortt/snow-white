package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.openapitools.codegen.CodegenType.SERVER;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import io.swagger.v3.oas.models.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

@ExtendWith({ MockitoExtension.class })
class SnowWhiteSpringServerGeneratorTest {

  private static final String VALID_API_NAME = "Test API";
  private static final String VALID_API_VERSION = "1.0.0";
  private static final String VALID_SERVICE_NAME = "test-service";

  private static final String INPUT_SPEC =
    "SnowWhiteSpringServerGeneratorIntegrationTest/valid-specification.yml";
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
    void initializesCliOptions() {
      assertThat(fixture)
        .extracting("cliOptions")
        .asInstanceOf(LIST)
        .anySatisfy(option ->
          assertThat(option)
            .asInstanceOf(type(CliOption.class))
            .extracting(CliOption::getOpt)
            .isEqualTo(SnowWhiteSpringServerGenerator.API_NAME_PROPERTY)
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

    assertThat(fixture.apiName).isEqualTo(newApiName);
    assertThat(fixture.apiVersion).isEqualTo(newApiVersion);
    assertThat(fixture.serviceName).isEqualTo(newServiceName);
  }

  @Nested
  class PostProcessOperationsWithModels {

    @Test
    void enhancesOperationsWithCompleteInformation() {
      var operationsMap = new OperationsMap();
      var operations = new OperationMap();
      List<CodegenOperation> operationList = new ArrayList<>();
      var operation = new CodegenOperation();
      operation.tags = singletonList(new Tag());
      operationList.add(operation);
      operations.put("operation", operationList);
      operationsMap.setOperation(operations);
      operationsMap.setImports(emptyList());

      fixture.setInputSpec(INPUT_SPEC);
      doReturn(VALID_YAML).when(yamlParserMock).readSpecToJson(INPUT_SPEC);

      var apiInfo = new OpenApiInformation(
        VALID_API_NAME,
        VALID_API_VERSION,
        VALID_SERVICE_NAME
      );
      doReturn(apiInfo)
        .when(informationExtractorMock)
        .extractFromOpenApi(anyString());

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

      fixture.setInputSpec(INPUT_SPEC);
      doReturn(VALID_YAML).when(yamlParserMock).readSpecToJson(INPUT_SPEC);

      var incompleteInfo = new OpenApiInformation(
        null,
        VALID_API_VERSION,
        VALID_SERVICE_NAME
      );
      doReturn(incompleteInfo)
        .when(informationExtractorMock)
        .extractFromOpenApi(anyString());

      OperationsMap result = fixture.postProcessOperationsWithModels(
        operationsMap,
        models
      );

      assertThat(result).isSameAs(operationsMap);
    }
  }
}
