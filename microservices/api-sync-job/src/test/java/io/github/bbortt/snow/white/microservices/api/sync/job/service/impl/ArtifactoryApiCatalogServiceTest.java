/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.GRACEFUL;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.STRICT;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.commons.openapi.OpenApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.exception.ApiCatalogException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.DownloadableArtifact;
import org.jfrog.artifactory.client.ItemHandle;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.Searches;
import org.jfrog.artifactory.client.model.AqlItem;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.artifactory.client.model.impl.FolderImpl;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class ArtifactoryApiCatalogServiceTest {

  @Mock
  private Artifactory artifactoryMock;

  @Mock
  private ApiSyncJobProperties apiSyncJobProperties;

  @Mock
  private ApiSyncJobProperties.ArtifactoryProperties artifactoryProperties;

  @Mock
  private OpenApiValidationService openApiValidationServiceMock;

  @Mock
  private InformationExtractor informationExtractorMock;

  @Mock
  private OpenAPIV3Parser openAPIV3ParserMock;

  @Mock
  private Searches searches;

  @Mock
  private RepositoryHandle repositoryHandle;

  private ArtifactoryApiCatalogService fixture;

  @BeforeEach
  void setUp() {
    doReturn(artifactoryProperties).when(apiSyncJobProperties).getArtifactory();
    doReturn("api-specs").when(artifactoryProperties).getRepository();

    fixture = new ArtifactoryApiCatalogService(
      artifactoryMock,
      apiSyncJobProperties,
      openApiValidationServiceMock,
      informationExtractorMock,
      openAPIV3ParserMock
    );
  }

  @Nested
  class GetApiSpecificationLoadersTest {

    @Test
    void shouldReturnApiInformationForValidOpenApiSpecs() throws IOException {
      AqlItem aqlItem = createAqlItem("apis", "petstore.yml");
      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(List.of(aqlItem)).when(searches).artifactsByFileSpec(any());

      // Fetching index is the only "synchronous" operation
      var results = fixture.getApiSpecificationLoaders();

      // Verify supplier does load asynchronous
      verifyNoInteractions(openApiValidationServiceMock);

      // Now we're instantiating the rest of the mocks, called when supplier is being invoked
      String openApiContent = """
        openapi: 3.0.0
        info:
          title: Petstore API
          version: 1.0.0
        """;

      OpenAPI openAPI = new OpenAPI();
      Info info = new Info();
      info.setTitle("Petstore API");
      info.setVersion("1.0.0");
      openAPI.setInfo(info);

      SwaggerParseResult parseResult = new SwaggerParseResult();
      parseResult.setOpenAPI(openAPI);

      doReturn(parseResult).when(openAPIV3ParserMock).readContents(anyString());

      OpenApiInformation openApiInformation = new OpenApiInformation(
        "petstore-api",
        "1.0.0",
        "petstore-service"
      );
      doReturn(openApiInformation)
        .when(informationExtractorMock)
        .extractFromOpenApi(anyString());

      doReturn(repositoryHandle).when(artifactoryMock).repository("api-specs");

      var downloadableArtifact = mock(DownloadableArtifact.class);
      doReturn(downloadableArtifact)
        .when(repositoryHandle)
        .download("apis/petstore.yml");
      doReturn(new ByteArrayInputStream(openApiContent.getBytes()))
        .when(downloadableArtifact)
        .doDownload();

      var itemHandle = mock(ItemHandle.class);
      doReturn(itemHandle).when(repositoryHandle).file("apis/petstore.yml");

      var fileInfo = mock(File.class);
      doReturn(fileInfo).when(itemHandle).info();
      doReturn("https://artifactory.example.com/api-specs/apis/petstore.yml")
        .when(fileInfo)
        .getDownloadUri();

      doReturn(STRICT).when(artifactoryProperties).getParsingMode();
      doAnswer(returnsFirstArg())
        .when(openApiValidationServiceMock)
        .validateApiInformationFromIndex(any(ApiInformation.class), eq(STRICT));

      var apiInformation = results.stream().map(Supplier::get).toList();
      assertThat(apiInformation).hasSize(1);

      var petstoreApi = apiInformation.getFirst();
      assertThat(petstoreApi)
        .isNotNull()
        .satisfies(
          a -> assertThat(a.getTitle()).isEqualTo("Petstore API"),
          a -> assertThat(a.getVersion()).isEqualTo("1.0.0"),
          a -> assertThat(a.getName()).isEqualTo("petstore-api"),
          a -> assertThat(a.getServiceName()).isEqualTo("petstore-service"),
          a -> assertThat(a.getApiType()).isEqualTo(OPENAPI),
          a ->
            assertThat(a.getSourceUrl()).isEqualTo(
              "https://artifactory.example.com/api-specs/apis/petstore.yml"
            )
        );

      verify(openApiValidationServiceMock).validateApiInformationFromIndex(
        petstoreApi,
        STRICT
      );
    }

    @Test
    void shouldSkipInvalidOpenApiSpecs_inGracefulParsingMode()
      throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      prepareInvalidOpenApiSpecificationWhenDownloading();

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results).containsExactly(new ApiInformation[] { null });
      verify(
        openApiValidationServiceMock,
        never()
      ).validateApiInformationFromIndex(any(), any());
    }

    @Test
    void shouldThrowOnInvalidOpenApiSpecs_inStrictParsingMode()
      throws IOException {
      doReturn(STRICT).when(artifactoryProperties).getParsingMode();

      prepareInvalidOpenApiSpecificationWhenDownloading();

      var openapiInformationStream = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get);

      assertThatThrownBy(openapiInformationStream::toList)
        .isInstanceOf(ApiCatalogException.class)
        .hasMessageContaining("Failed to parse OpenAPI from 'apis/invalid.yml'")
        .hasMessageContaining("at [No location information]");

      verify(
        openApiValidationServiceMock,
        never()
      ).validateApiInformationFromIndex(any(), any());
    }

    private void prepareInvalidOpenApiSpecificationWhenDownloading()
      throws IOException {
      AqlItem aqlItem = createAqlItem("apis", "invalid.yml");
      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(List.of(aqlItem)).when(searches).artifactsByFileSpec(any());

      SwaggerParseResult parseResult = new SwaggerParseResult();
      parseResult.setOpenAPI(null);
      parseResult.setMessages(List.of("Invalid OpenAPI format"));

      doReturn(parseResult).when(openAPIV3ParserMock).readContents(anyString());

      doReturn(repositoryHandle).when(artifactoryMock).repository("api-specs");

      var downloadableArtifact = mock(DownloadableArtifact.class);
      doReturn(downloadableArtifact)
        .when(repositoryHandle)
        .download("apis/invalid.yml");
      doReturn(new ByteArrayInputStream("invalid content".getBytes()))
        .when(downloadableArtifact)
        .doDownload();
    }

    @Test
    void shouldHandleDownloadErrors_gracefully() throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      prepareDownloadErrorThrownWhenDownloadingOpenApiSpecification();

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results).containsExactly(new ApiInformation[] { null });
      verify(
        openApiValidationServiceMock,
        never()
      ).validateApiInformationFromIndex(any(), any());
    }

    @Test
    void shouldHandleDownloadErrors_strict() throws IOException {
      doReturn(STRICT).when(artifactoryProperties).getParsingMode();

      var downloadException =
        prepareDownloadErrorThrownWhenDownloadingOpenApiSpecification();

      var openapiInformationStream = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get);

      assertThatThrownBy(openapiInformationStream::toList)
        .isInstanceOf(ApiCatalogException.class)
        .hasMessageContaining("Failed to parse OpenAPI from 'apis/error.yml'")
        .hasMessageContaining("at [No location information]")
        .rootCause()
        .isEqualTo(downloadException);

      verify(
        openApiValidationServiceMock,
        never()
      ).validateApiInformationFromIndex(any(), any());
    }

    private @NonNull RuntimeException prepareDownloadErrorThrownWhenDownloadingOpenApiSpecification()
      throws IOException {
      AqlItem aqlItem = createAqlItem("apis", "error.yml");
      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(List.of(aqlItem)).when(searches).artifactsByFileSpec(any());

      doReturn(repositoryHandle).when(artifactoryMock).repository("api-specs");

      var downloadableArtifact = mock(DownloadableArtifact.class);
      doReturn(downloadableArtifact)
        .when(repositoryHandle)
        .download("apis/error.yml");
      var downloadException = new RuntimeException("Download failed");
      doThrow(downloadException).when(downloadableArtifact).doDownload();
      return downloadException;
    }

    @Test
    void shouldHandleMultipleApiSpecs() throws IOException {
      AqlItem aqlItem1 = createAqlItem("apis", "petstore.yml");
      AqlItem aqlItem2 = createAqlItem("apis", "users.yml");

      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(List.of(aqlItem1, aqlItem2))
        .when(searches)
        .artifactsByFileSpec(any());

      doReturn(repositoryHandle).when(artifactoryMock).repository("api-specs");

      setupValidOpenApiMock("apis/petstore.yml", "Petstore API", "1.0.0");
      setupValidOpenApiMock("apis/users.yml", "Users API", "2.0.0");

      doReturn(STRICT).when(artifactoryProperties).getParsingMode();
      doAnswer(returnsFirstArg())
        .when(openApiValidationServiceMock)
        .validateApiInformationFromIndex(any(ApiInformation.class), eq(STRICT));

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results)
        .hasSize(2)
        .extracting(ApiInformation::getTitle)
        .containsExactlyInAnyOrder("Petstore API", "Users API");
    }

    @Test
    void shouldHandleEmptyRepository() {
      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(List.of()).when(searches).artifactsByFileSpec(any());

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results).isEmpty();
    }

    @Test
    void shouldHandleFileInfoNotBeingFile() throws IOException {
      AqlItem aqlItem = createAqlItem("apis", "petstore.yml");
      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(List.of(aqlItem)).when(searches).artifactsByFileSpec(any());

      String openApiContent = """
        openapi: 3.0.0
        info:
          title: Petstore API
          version: 1.0.0
        """;

      OpenAPI openAPI = new OpenAPI();
      Info info = new Info();
      info.setTitle("Petstore API");
      openAPI.setInfo(info);

      SwaggerParseResult parseResult = new SwaggerParseResult();
      parseResult.setOpenAPI(openAPI);

      doReturn(parseResult).when(openAPIV3ParserMock).readContents(anyString());

      OpenApiInformation openApiInformation = new OpenApiInformation(
        "petstore-api",
        "1.0.0",
        "petstore-service"
      );
      doReturn(openApiInformation)
        .when(informationExtractorMock)
        .extractFromOpenApi(anyString());

      doReturn(repositoryHandle).when(artifactoryMock).repository("api-specs");

      var downloadableArtifact = mock(DownloadableArtifact.class);
      doReturn(downloadableArtifact)
        .when(repositoryHandle)
        .download("apis/petstore.yml");
      doReturn(new ByteArrayInputStream(openApiContent.getBytes()))
        .when(downloadableArtifact)
        .doDownload();

      var itemHandle = mock(ItemHandle.class);
      doReturn(itemHandle).when(repositoryHandle).file("apis/petstore.yml");

      // Return a Folder instead of File
      var folderImpl = mock(FolderImpl.class);
      doReturn(folderImpl).when(itemHandle).info();

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results).containsExactly(new ApiInformation[] { null });
    }

    private AqlItem createAqlItem(String path, String name) {
      var aqlItem = mock(AqlItem.class);
      doReturn(path).when(aqlItem).getPath();
      doReturn(name).when(aqlItem).getName();
      return aqlItem;
    }

    private void setupValidOpenApiMock(
      String filePath,
      String title,
      String version
    ) throws IOException {
      var openApiContent = format(
        """
        openapi: 3.0.0
        info:
          title: %s
          version: %s
        """,
        title,
        version
      );

      var downloadableArtifact = mock(DownloadableArtifact.class);
      doReturn(downloadableArtifact).when(repositoryHandle).download(filePath);
      doReturn(new ByteArrayInputStream(openApiContent.getBytes()))
        .when(downloadableArtifact)
        .doDownload();

      var info = new Info().title(title).version(version);
      var openAPI = new OpenAPI().info(info);

      var parseResult = new SwaggerParseResult();
      parseResult.setOpenAPI(openAPI);

      doReturn(parseResult)
        .when(openAPIV3ParserMock)
        .readContents(openApiContent);

      OpenApiInformation openApiInformation = new OpenApiInformation(
        title.toLowerCase().replace(" ", "-"),
        version,
        "test-service"
      );

      doReturn(openApiInformation)
        .when(informationExtractorMock)
        .extractFromOpenApi(JsonMapper.shared().writeValueAsString(openAPI));

      var itemHandle = mock(ItemHandle.class);
      doReturn(itemHandle).when(repositoryHandle).file(filePath);

      var fileInfo = mock(File.class);
      doReturn(fileInfo).when(itemHandle).info();
      doReturn("https://artifactory.example.com/api-specs/" + filePath)
        .when(fileInfo)
        .getDownloadUri();
    }
  }
}
