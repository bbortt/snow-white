package io.github.bbortt.snow.white.rest.v1;

import io.github.bbortt.snow.white.rest.utils.MultipartParser;
import io.github.bbortt.snow.white.service.SpecificationService;
import io.smallrye.mutiny.Multi;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reactive implementation of {@link SpecificationResourceApi}, because the generator does not support it!
 */
@ApplicationScoped
@Path("/api/v1/specifications/openapi")
public class SpecificationResource {

  private static final Logger logger = LoggerFactory.getLogger(SpecificationResource.class);

  @Inject
  SpecificationService specificationService;

  @POST
  @Path("/force")
  @Consumes({ "multipart/form-data" })
  @Produces({ "application/json" })
  public Multi<String> forceUploadOpenApiSpecification(
    @HeaderParam("Content-Type") @NotNull String contentType,
    @FormParam(value = "specification") InputStream specificationInputStream
  ) {
    return specificationService.parseAndForcePersistOpenApis(parseMultipartRequest(contentType, specificationInputStream));
  }

  @POST
  @Consumes({ "multipart/form-data" })
  @Produces({ "application/json" })
  public Multi<String> uploadOpenapiSpecification(
    @HeaderParam("Content-Type") @NotNull String contentType,
    @FormParam(value = "specification") InputStream specificationInputStream
  ) {
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
