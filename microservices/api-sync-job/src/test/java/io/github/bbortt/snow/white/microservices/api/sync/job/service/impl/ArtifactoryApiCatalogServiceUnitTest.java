/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.DOWNLOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOADED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.PARSE_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.GRACEFUL;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.STRICT;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Locale.ROOT;
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

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesNf;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.openapi.InformationExtractor;
import io.github.bbortt.snow.white.commons.openapi.OpenApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.OpenApiValidationService;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.exception.ApiCatalogException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class ArtifactoryApiCatalogServiceUnitTest {

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
    // Order matters here: the constructor calls getArtifactory() eagerly to cache it,
    // so the stub must be in place before construction.
    // @InjectMocks would construct the fixture before this @BeforeEach body runs,
    // breaking that ordering - manual construction is kept intentionally.
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
    @VerifiesArch(
      ArchTraceables.ARCH_015_IDENTITY_DECLARED_IN_THE_SPECIFICATION
    )
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
      // language=yaml
      String openApiContent = """
      openapi: 3.1.2
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

      doReturn(parseResult)
        .when(openAPIV3ParserMock)
        .readContents(anyString(), any(), any());

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
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldSkipInvalidOpenApiSpecs_inGracefulParsingMode()
      throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      prepareInvalidOpenApiSpecificationWhenDownloading();

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results)
        .singleElement()
        .extracting(ApiInformation::getLoadStatus)
        .isEqualTo(PARSE_FAILED);
      verify(
        openApiValidationServiceMock,
        never()
      ).validateApiInformationFromIndex(any(), any());
    }

    /**
     * A document can parse into an {@code OpenAPI} without an {@code info}
     * object - the identity fields live there, so it is no more indexable than
     * one that did not parse at all, and reaching for them must not raise out of
     * the graceful path.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldSkipSpecsWithoutInformationObject_inGracefulParsingMode()
      throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      var parseResult = new SwaggerParseResult();
      parseResult.setOpenAPI(new OpenAPI());
      parseResult.setMessages(List.of("attribute info is missing"));

      prepareOpenApiSpecificationWhenDownloading(parseResult);

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results)
        .singleElement()
        .extracting(ApiInformation::getLoadStatus)
        .isEqualTo(PARSE_FAILED);
      verify(
        openApiValidationServiceMock,
        never()
      ).validateApiInformationFromIndex(any(), any());
    }

    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
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
        .hasMessage(
          "Failed to parse OpenAPI from 'apis/invalid.yml': [Invalid OpenAPI format]"
        );

      verify(
        openApiValidationServiceMock,
        never()
      ).validateApiInformationFromIndex(any(), any());
    }

    private void prepareInvalidOpenApiSpecificationWhenDownloading()
      throws IOException {
      SwaggerParseResult parseResult = new SwaggerParseResult();
      parseResult.setOpenAPI(null);
      parseResult.setMessages(List.of("Invalid OpenAPI format"));

      prepareOpenApiSpecificationWhenDownloading(parseResult);
    }

    private void prepareOpenApiSpecificationWhenDownloading(
      SwaggerParseResult parseResult
    ) throws IOException {
      prepareOpenApiSpecificationWhenDownloading();

      doReturn(parseResult)
        .when(openAPIV3ParserMock)
        .readContents(anyString(), any(), any());
    }

    /**
     * Everything up to the parse, so a test that wants the parser to fail
     * instead of answering can stub that itself rather than leaving the
     * answering stub behind unused.
     */
    private void prepareOpenApiSpecificationWhenDownloading()
      throws IOException {
      AqlItem aqlItem = createAqlItem("apis", "invalid.yml");
      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(List.of(aqlItem)).when(searches).artifactsByFileSpec(any());

      doReturn(repositoryHandle).when(artifactoryMock).repository("api-specs");

      var downloadableArtifact = mock(DownloadableArtifact.class);
      doReturn(downloadableArtifact)
        .when(repositoryHandle)
        .download("apis/invalid.yml");
      doReturn(new ByteArrayInputStream("invalid content".getBytes()))
        .when(downloadableArtifact)
        .doDownload();
    }

    /**
     * A file that cannot be read is counted apart from one that was read but
     * would not parse: the operator's summary distinguishes an unreachable
     * repository from a repository full of unparseable documents.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldHandleDownloadErrors_gracefully() throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      prepareDownloadErrorThrownWhenDownloadingOpenApiSpecification();

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results)
        .singleElement()
        .extracting(ApiInformation::getLoadStatus)
        .isEqualTo(DOWNLOAD_FAILED);
      verify(
        openApiValidationServiceMock,
        never()
      ).validateApiInformationFromIndex(any(), any());
    }

    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
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
        .hasMessageContaining("Failed to download 'apis/error.yml'")
        // An abort is read from a log line, so the reason belongs in the line
        // rather than a `getCause()` away.
        .hasMessageContaining(downloadException.getMessage())
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

    /**
     * One cycle over a source holding a valid specification next to a file that
     * fails at each stage: the valid one still loads, and each unreadable file
     * is counted apart rather than taking the cycle down with it.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldCountEachUnreadableFileUnderItsOwnReason_inGracefulParsingMode()
      throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(
        List.of(
          createAqlItem("apis", "petstore.yml"),
          createAqlItem("apis", "undownloadable.yml"),
          createAqlItem("apis", "unparseable.yml")
        )
      )
        .when(searches)
        .artifactsByFileSpec(any());

      doReturn(repositoryHandle).when(artifactoryMock).repository("api-specs");

      setupValidOpenApiMock("apis/petstore.yml", "Petstore API", "1.0.0");
      doAnswer(invocation ->
        invocation.<ApiInformation>getArgument(0).withLoadStatus(LOADED)
      )
        .when(openApiValidationServiceMock)
        .validateApiInformationFromIndex(
          any(ApiInformation.class),
          eq(GRACEFUL)
        );

      var undownloadable = mock(DownloadableArtifact.class);
      doReturn(undownloadable)
        .when(repositoryHandle)
        .download("apis/undownloadable.yml");
      doThrow(new RuntimeException("Download failed"))
        .when(undownloadable)
        .doDownload();

      var unparseable = mock(DownloadableArtifact.class);
      doReturn(unparseable)
        .when(repositoryHandle)
        .download("apis/unparseable.yml");
      doReturn(new ByteArrayInputStream("not a specification".getBytes()))
        .when(unparseable)
        .doDownload();

      var parseResult = new SwaggerParseResult();
      parseResult.setMessages(List.of("Invalid OpenAPI format"));
      doReturn(parseResult)
        .when(openAPIV3ParserMock)
        .readContents(eq("not a specification"), any(), any());

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results)
        .extracting(ApiInformation::getLoadStatus)
        .containsExactlyInAnyOrder(LOADED, DOWNLOAD_FAILED, PARSE_FAILED);
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

    /**
     * Anything that goes wrong between a parsed document and an indexable
     * identity is the third stage, and it fails the same way the first two do -
     * gracefully by default, so one unusable repository entry cannot take the
     * worker that found it down with it.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldCountFileInfoWhichIsNotAFile_asLoadFailed() throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      prepareFolderInfoWhenReadingTheDownloadUrl();

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results)
        .singleElement()
        .extracting(ApiInformation::getLoadStatus)
        .isEqualTo(LOAD_FAILED);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldThrowWhenFileInfoItNotAFile() throws IOException {
      doReturn(STRICT).when(artifactoryProperties).getParsingMode();

      prepareFolderInfoWhenReadingTheDownloadUrl();

      var apiInformationStream = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get);

      assertThatThrownBy(apiInformationStream::toList)
        .isInstanceOf(ApiCatalogException.class)
        .hasMessageContaining(
          "Failed to extract API information from 'apis/petstore.yml'"
        )
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Encountered OpenAPI specification which is not a file!");
    }

    private void prepareFolderInfoWhenReadingTheDownloadUrl()
      throws IOException {
      AqlItem aqlItem = createAqlItem("apis", "petstore.yml");
      doReturn(searches).when(artifactoryMock).searches();
      doReturn(searches).when(searches).repositories("api-specs");
      doReturn(List.of(aqlItem)).when(searches).artifactsByFileSpec(any());

      // language=yaml
      String openApiContent = """
      openapi: 3.1.2
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

      doReturn(parseResult)
        .when(openAPIV3ParserMock)
        .readContents(anyString(), any(), any());

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
    }

    @Test
    @VerifiesNf(NfTraceables.NF_010_REFERENCE_RESOLUTION_IS_OFF_BY_DEFAULT)
    void shouldNotResolveReferences_byDefault() throws IOException {
      assertThat(parseOptionsUsedWhenParsing())
        .extracting(ParseOptions::isResolve)
        .isEqualTo(false);
    }

    @Test
    @VerifiesNf(NfTraceables.NF_010_REFERENCE_RESOLUTION_IS_OFF_BY_DEFAULT)
    void shouldResolveReferences_whenTheOperatorAsksForIt() throws IOException {
      doReturn(TRUE).when(artifactoryProperties).getResolveReferences();

      // The property is read once, when the service is constructed.
      fixture = new ArtifactoryApiCatalogService(
        artifactoryMock,
        apiSyncJobProperties,
        openApiValidationServiceMock,
        informationExtractorMock,
        openAPIV3ParserMock
      );

      assertThat(parseOptionsUsedWhenParsing())
        .extracting(ParseOptions::isResolve)
        .isEqualTo(true);
    }

    /**
     * Resolving {@code $ref} pointers walks them, so a circular chain exhausts
     * the stack rather than raising. That is an {@link Error}, not an exception:
     * letting it escape would kill the worker and drop the file from the summary
     * it belongs in.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldCountASpecificationWhoseReferencesRecurse_asParseFailed()
      throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      prepareOpenApiSpecificationWhenDownloading();
      doThrow(new StackOverflowError())
        .when(openAPIV3ParserMock)
        .readContents(anyString(), any(), any());

      var results = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get)
        .toList();

      assertThat(results)
        .singleElement()
        .extracting(ApiInformation::getLoadStatus)
        .isEqualTo(PARSE_FAILED);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_034_UNREADABLE_SPEC_SKIPPED_UNLESS_STRICT)
    void shouldAbortOnASpecificationWhoseReferencesRecurse_inStrictParsingMode()
      throws IOException {
      doReturn(STRICT).when(artifactoryProperties).getParsingMode();

      prepareOpenApiSpecificationWhenDownloading();
      doThrow(new StackOverflowError())
        .when(openAPIV3ParserMock)
        .readContents(anyString(), any(), any());

      var openapiInformationStream = fixture
        .getApiSpecificationLoaders()
        .stream()
        .map(Supplier::get);

      assertThatThrownBy(openapiInformationStream::toList)
        .isInstanceOf(ApiCatalogException.class)
        .hasMessageContaining("Failed to parse OpenAPI from 'apis/invalid.yml'")
        .hasCauseInstanceOf(StackOverflowError.class);
    }

    /**
     * Drives one specification through the loader and hands back the options the
     * parser was actually called with - the only place the setting becomes
     * observable, because resolution happens inside the parser.
     */
    private ParseOptions parseOptionsUsedWhenParsing() throws IOException {
      doReturn(GRACEFUL).when(artifactoryProperties).getParsingMode();

      prepareInvalidOpenApiSpecificationWhenDownloading();

      fixture.getApiSpecificationLoaders().stream().map(Supplier::get).toList();

      var parseOptionsCaptor = ArgumentCaptor.forClass(ParseOptions.class);
      verify(openAPIV3ParserMock).readContents(
        anyString(),
        any(),
        parseOptionsCaptor.capture()
      );

      return parseOptionsCaptor.getValue();
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
        openapi: 3.1.2
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
        .readContents(eq(openApiContent), any(), any());

      OpenApiInformation openApiInformation = new OpenApiInformation(
        title.toLowerCase(ROOT).replace(" ", "-"),
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
