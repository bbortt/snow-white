package io.github.bbortt.snow.white.service;

import io.smallrye.mutiny.Multi;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SpecificationService {

  private static final Logger logger = LoggerFactory.getLogger(SpecificationService.class);

  private static final Map<String, List<String>> REGISTERED_SPECIFICATIONS_WITH_PATHS = new HashMap<>();

  public Multi<String> parseAndForcePersistOpenApis(Map<String, List<FileItem>> multipartFiles) {
    return parseAndPersistOpenApis(multipartFiles, true);
  }

  public Multi<String> parseAndPersistOpenApis(Map<String, List<FileItem>> multipartFiles) {
    return parseAndPersistOpenApis(multipartFiles, false);
  }

  public Multi<String> parseAndPersistOpenApis(Map<String, List<FileItem>> multipartFiles, boolean forcePersist) {
    return Multi
      .createFrom()
      .items(
        multipartFiles
          .values()
          .stream()
          .flatMap(Collection::stream)
          .map(fileItem -> parseFileItemAndPersistOpenApi(fileItem, forcePersist))
          .flatMap(Collection::stream)
      );
  }

  private List<String> parseFileItemAndPersistOpenApi(FileItem fileItem, boolean forcePersist) {
    SwaggerParseResult result = new OpenAPIParser().readContents(fileItem.getString(), null, new ParseOptions());
    // OpenAPI openAPI = result.getOpenAPI();

    logger.info("Parsed OpenAPI: {}", result.getOpenAPI().getInfo());

    return result.getMessages() == null ? List.of() : result.getMessages();
  }

  public boolean openAPISpecificationForServiceExists(String serviceName) {
    return REGISTERED_SPECIFICATIONS_WITH_PATHS.containsKey(serviceName);
  }
}
