package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.swagger.v3.oas.models.info.Info;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.languages.SpringCodegen;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationsMap;

@Setter
public class SnowWhiteSpringServerGenerator extends SpringCodegen {

  public static final String NAME = "snow-white-spring-server";

  public static final String API_NAME_PROPERTY = "apiNameProperty";
  public String apiName = "x-api-name";

  public static final String API_VERSION_PROPERTY = "apiVersionProperty";
  public String apiVersion = "version";

  public static final String OTEL_SERVICE_NAME_PROPERTY =
    "otelServiceNameProperty";
  public String otelServiceName = "x-otel-service-name";

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
        OTEL_SERVICE_NAME_PROPERTY,
        "Property from which to extract the OTEL service name, which this API belongs to",
        otelServiceName
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
    OperationsMap operations,
    List<ModelMap> allModels
  ) {
    OperationsMap processedOperations = super.postProcessOperationsWithModels(
      operations,
      allModels
    );

    Map<String, Object> ops = processedOperations.getOperations();
    if (nonNull(ops)) {
      @SuppressWarnings("unchecked")
      List<CodegenOperation> operationList = (List<CodegenOperation>) ops.get(
        "operation"
      );
      for (CodegenOperation operation : operationList) {
        var info = openAPI.getInfo();

        var apiName = extractApiName(info);
        var apiVersion = extractApiVersion(info);
        var otelServiceName = extractOtelServiceName(info);

        if (
          isNotBlank(apiName) &&
          isNotBlank(apiVersion) &&
          isNotBlank(otelServiceName)
        ) {
          var annotation = format(
            "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"%s\", apiName = \"%s\", apiVersion = \"%s\")",
            otelServiceName,
            apiName,
            apiVersion
          );

          operation.vendorExtensions.put(
            "x-operation-extra-annotation",
            annotation
          );
        }
      }
    }

    return processedOperations;
  }

  private @Nullable String extractApiName(Info info) {
    if ("title".equals(apiName)) {
      return info.getTitle();
    } else if (isBlank(apiName) || !apiName.startsWith("x-")) {
      throw new IllegalArgumentException(
        "The api name property must either be 'title' or start with 'x-'"
      );
    }

    return extractFromExtensionsOrNull(info, apiName);
  }

  private @Nullable String extractApiVersion(Info info) {
    if ("version".equals(apiVersion)) {
      return info.getVersion();
    } else if (isBlank(apiVersion) || !apiVersion.startsWith("x-")) {
      throw new IllegalArgumentException(
        "The api version property must either be 'version' or start with 'x-'"
      );
    }

    return extractFromExtensionsOrNull(info, apiVersion);
  }

  private @Nullable String extractOtelServiceName(Info info) {
    if (isBlank(otelServiceName) || !otelServiceName.startsWith("x-")) {
      throw new IllegalArgumentException(
        "The OTEL service name property must start with 'x-'"
      );
    }

    return extractFromExtensionsOrNull(info, otelServiceName);
  }

  private @Nullable String extractFromExtensionsOrNull(
    Info info,
    String otelServiceName
  ) {
    return nonNull(info.getExtensions())
      ? (String) info.getExtensions().get(otelServiceName)
      : null;
  }
}
