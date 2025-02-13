package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static io.github.bbortt.snow.white.toolkit.openapi.generator.YamlParser.OBJECT_MAPPER;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

@Slf4j
@Setter
public class SnowWhiteSpringServerGenerator extends SpringCodegen {

  public static final String NAME = "snow-white-spring-server";

  public static final String API_NAME_PROPERTY = "apiNameProperty";
  public String apiName = "info.title";

  public static final String API_VERSION_PROPERTY = "apiVersionProperty";
  public String apiVersion = "info.version";

  public static final String SERVICE_NAME_PROPERTY = "serviceNameProperty";
  public String serviceName = "info.x-service-name";

  private final YamlParser yamlParser = new YamlParser();

  private InformationExtractor informationExtractor;

  public SnowWhiteSpringServerGenerator() {
    super();
    cliOptions.add(
      new CliOption(
        API_NAME_PROPERTY,
        "Property from which to extract the API name",
        apiName
      )
    );
    cliOptions.add(
      new CliOption(
        API_VERSION_PROPERTY,
        "Property from which to extract the API version",
        apiVersion
      )
    );
    cliOptions.add(
      new CliOption(
        SERVICE_NAME_PROPERTY,
        "Property from which to extract the service name, the name of the API provider",
        serviceName
      )
    );
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getHelp() {
    return "Enhances the OpenAPI Spring generator with snow-white specific information.";
  }

  @Override
  public OperationsMap postProcessOperationsWithModels(
    OperationsMap operationsMap,
    List<ModelMap> allModels
  ) {
    OperationsMap processedOperations = super.postProcessOperationsWithModels(
      operationsMap,
      allModels
    );

    var openApi = yamlParser.readSpecToJson(getInputSpec());

    var extractedApiInformation = getOrCreateInformationExtractor()
      .extractFromOpenApi(openApi);

    if (extractedApiInformation.isIncomplete()) {
      try {
        logger.warn(
          "Failed to extract snow-white information from OpenAPI specification: {}",
          OBJECT_MAPPER.writeValueAsString(extractedApiInformation)
        );
      } catch (JsonProcessingException e) {
        // Ignore exception
      }

      return processedOperations;
    }

    enhanceApiOperationsWithSnowWhiteInformation(
      processedOperations.getOperations(),
      extractedApiInformation
    );

    return processedOperations;
  }

  private InformationExtractor getOrCreateInformationExtractor() {
    if (isNull(informationExtractor)) {
      informationExtractor = new InformationExtractor(
        apiName,
        apiVersion,
        serviceName
      );
    }

    return informationExtractor;
  }

  private void enhanceApiOperationsWithSnowWhiteInformation(
    OperationMap operations,
    OpenApiInformation extractedApiInformation
  ) {
    if (nonNull(operations)) {
      @SuppressWarnings("unchecked")
      List<CodegenOperation> operationList = (List<
          CodegenOperation
        >) operations.get("operation");
      for (CodegenOperation operation : operationList) {
        var annotation = format(
          "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"%s\", apiName = \"%s\", apiVersion = \"%s\")",
          extractedApiInformation.serviceName(),
          extractedApiInformation.apiName(),
          extractedApiInformation.apiVersion()
        );

        operation.vendorExtensions.put(
          "x-operation-extra-annotation",
          annotation
        );
      }
    }
  }
}
