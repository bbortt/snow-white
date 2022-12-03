package io.github.bbortt.snow.white.service;

import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.smallrye.mutiny.Uni;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SpecificationService {

  private static final Logger logger = LoggerFactory.getLogger(SpecificationService.class);

  private static final Map<String, List<String>> REGISTERED_SPECIFICATIONS_WITH_PATHS = new HashMap<>();

  public List<String> parseAndForcePersistOpenApis(Map<String, List<FileItem>> multipartFiles) {
    return parseAndPersistOpenApis(multipartFiles, true);
  }

  public List<String> parseAndPersistOpenApis(Map<String, List<FileItem>> multipartFiles) {
    return parseAndPersistOpenApis(multipartFiles, false);
  }

  public List<String> parseAndPersistOpenApis(Map<String, List<FileItem>> multipartFiles, boolean forcePersist) {
    SwaggerParseResult result = new OpenAPIParser().readLocation("https://petstore3.swagger.io/api/v3/openapi.json", null, null);

    // or from a file
    //   SwaggerParseResult result = new OpenAPIParser().readLocation("./path/to/openapi.yaml", null, null);

    // the parsed POJO
    OpenAPI openAPI = result.getOpenAPI();

    if (result.getMessages() != null) result.getMessages().forEach(System.err::println); // validation errors and warnings

    // String sha256hex = DigestUtils.sha256Hex(originalString);

    return List.of();
  }
}
