package io.github.bbortt.snow.white.rest.v1;

import io.github.bbortt.snow.white.rest.multipart.SimpleUploadContext;
import io.github.bbortt.snow.white.service.SpecificationService;
import io.smallrye.mutiny.Multi;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reactive implementation of {@link SpecificationResourceApi}, because the generator does not
 * support it!
 */
@ApplicationScoped
@Path("/api/v1/specifications/openapi")
public class SpecificationResource {

  private static final Logger logger = LoggerFactory.getLogger(SpecificationResource.class);

  private final ServletFileUpload fileUpload = new ServletFileUpload();

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

  private Map<String, List<FileItem>> parseMultipartRequest(String contentType, InputStream inputStream) {
    logger.debug("Parsing multipart request...");

    try {
      SimpleUploadContext context = new SimpleUploadContext(contentType, inputStream);
      if (!FileUploadBase.isMultipartContent(context)) {
        throw new IllegalArgumentException("The request is not of type multipart/form-data!");
      }

      FileItemIterator fileItemIterator = fileUpload.getItemIterator(context);
      while (fileItemIterator.hasNext()) {
        FileItemStream item = fileItemIterator.next();
        String fieldName = item.getFieldName();

        if (item.isFormField()) {
          throw new IllegalArgumentException("Request contained a form field which is not a file item!");
        } else {
          System.out.println("File field " + fieldName + " with file name " + item.getName() + " detected.");
          //          // Process the input stream
          //        ...
        }
      }

      //      Map<String, List<FileItem>> multiparts = MultipartParser.parseRequest(specificationInputStream.readAllBytes(), contentType);
      //
      //      logger.trace("Parsed {} different multipart files", multiparts.size());
      //
      return new HashMap<>();
    } catch (FileUploadException | IOException e) {
      logger.error("Failed to read OpenAPI specification from multipart request!", e);
      throw new IllegalArgumentException(e);
    }
  }
}
