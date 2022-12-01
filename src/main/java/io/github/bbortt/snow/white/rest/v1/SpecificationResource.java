package io.github.bbortt.snow.white.rest.v1;

import io.github.bbortt.snow.white.rest.utils.MultipartParser;
import io.github.bbortt.snow.white.service.SpecificationService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SpecificationResource implements SpecificationResourceApi {

  private static final Logger logger = LoggerFactory.getLogger(SpecificationResource.class);

  @Inject
  SpecificationService specificationService;

  @Override
  public List<String> apiV1SpecificationsOpenapiForcePost(String contentType, InputStream specificationInputStream) {
    return specificationService.parseAndForcePersistOpenApis(parseMultipartRequest(contentType, specificationInputStream));
  }

  @Override
  public List<String> uploadOpenapiSpecifications(String contentType, InputStream specificationInputStream) {
    return specificationService.parseAndPersistOpenApis(parseMultipartRequest(contentType, specificationInputStream));
  }

  private Map<String, List<FileItem>> parseMultipartRequest(String contentType, InputStream specificationInputStream) {
    try {
      return MultipartParser.parseRequest(specificationInputStream.readAllBytes(), contentType);
    } catch (FileUploadException | IOException e) {
      logger.error("Failed to read OpenAPI specification from multipart request!", e);
      throw new IllegalArgumentException(e);
    }
  }
}
