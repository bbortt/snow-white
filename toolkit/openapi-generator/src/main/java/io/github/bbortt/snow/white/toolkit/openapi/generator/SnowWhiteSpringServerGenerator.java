/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PACKAGE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.commons.openapi.OpenApiInformation;
import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.Setter;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Setter
public class SnowWhiteSpringServerGenerator extends SpringCodegen {

  public static final String NAME = "snow-white-spring-server";

  public static final String API_NAME_PROPERTY = "apiNameProperty";

  @Setter
  protected String apiName;

  public static final String API_VERSION_PROPERTY = "apiVersionProperty";

  @Setter
  protected String apiVersion;

  public static final String SERVICE_NAME_PROPERTY = "serviceNameProperty";

  @Setter
  protected String serviceName;

  @With(PACKAGE)
  @VisibleForTesting
  private InformationExtractor informationExtractor;

  @With(PACKAGE)
  @VisibleForTesting
  private YamlToJsonConverter yamlToJsonConverter;

  public SnowWhiteSpringServerGenerator() {
    this(
      "info.title",
      "info.version",
      "info.x-service-name",
      null,
      new YamlToJsonConverter()
    );
  }

  SnowWhiteSpringServerGenerator(
    String apiName,
    String apiVersion,
    String serviceName,
    InformationExtractor informationExtractor,
    YamlToJsonConverter yamlToJsonConverter
  ) {
    super();
    this.apiName = apiName;
    this.apiVersion = apiVersion;
    this.serviceName = serviceName;
    this.informationExtractor = informationExtractor;
    this.yamlToJsonConverter = yamlToJsonConverter;

    cliOptions.add(
      CliOption.newString(
        API_NAME_PROPERTY,
        "Property from which to extract the API name"
      )
    );
    cliOptions.add(
      CliOption.newString(
        API_VERSION_PROPERTY,
        "Property from which to extract the API version"
      )
    );
    cliOptions.add(
      CliOption.newString(
        SERVICE_NAME_PROPERTY,
        "Property from which to extract the service name, the name of the API provider"
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
  public void processOpts() {
    super.processOpts();

    convertPropertyToStringAndWriteBack(API_NAME_PROPERTY, this::setApiName);
    convertPropertyToStringAndWriteBack(
      API_VERSION_PROPERTY,
      this::setApiVersion
    );
    convertPropertyToStringAndWriteBack(
      SERVICE_NAME_PROPERTY,
      this::setServiceName
    );
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

    var openApi = yamlToJsonConverter.readSpecToJson(getInputSpec());

    var extractedApiInformation =
      getOrCreateInformationExtractor().extractFromOpenApi(openApi);

    if (extractedApiInformation.isIncomplete()) {
      logger.warn(
        "Failed to extract snow-white information from OpenAPI specification: {}",
        JsonMapper.shared().writeValueAsString(extractedApiInformation)
      );

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
        operation.vendorExtensions.put(
          "x-operation-extra-annotation",
          buildAnnotationString(
            extractedApiInformation,
            operation.operationId,
            operation.path
          )
        );
      }
    }
  }

  private String buildAnnotationString(
    OpenApiInformation extractedApiInformation,
    @Nullable String operationId,
    String path
  ) {
    if (isNotBlank(operationId)) {
      return format(
        "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"%s\", apiName = \"%s\", apiVersion = \"%s\", operationId = \"%s\")",
        extractedApiInformation.serviceName(),
        extractedApiInformation.apiName(),
        extractedApiInformation.apiVersion(),
        operationId
      );
    } else {
      logger.warn(
        "No ID present for operation '{}'! Note that unique operation IDs will speed up data processing in snow-white drastically.",
        path
      );
      return format(
        "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"%s\", apiName = \"%s\", apiVersion = \"%s\")",
        extractedApiInformation.serviceName(),
        extractedApiInformation.apiName(),
        extractedApiInformation.apiVersion()
      );
    }
  }
}
