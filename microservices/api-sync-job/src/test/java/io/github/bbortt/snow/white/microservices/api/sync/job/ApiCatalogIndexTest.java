package io.github.bbortt.snow.white.microservices.api.sync.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.service.ApiCatalogService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@ExtendWith({ MockitoExtension.class })
class ApiCatalogIndexTest {

  @Mock
  private ApiCatalogService apiCatalogServiceMock;

  @Mock
  private ApiInformation apiInformation1;

  @Mock
  private ApiInformation apiInformation2;

  @Mock
  private ApiInformation apiInformation3;

  @Mock
  private ApiInformation validatedApiInformation1;

  @Mock
  private ApiInformation validatedApiInformation2;

  @Mock
  private ApiInformation validatedApiInformation3;

  private AsyncTaskExecutor taskExecutor;

  private ApiCatalogIndex catalogIndex;

  @BeforeEach
  void beforeEachSetup() {
    taskExecutor = new SimpleAsyncTaskExecutor();
  }

  @Nested
  class ValidateApiInformationTests {

    @Test
    void shouldReturnEmptyListWhenNoApiInformationProvided() {
      catalogIndex = new ApiCatalogIndex(
        apiCatalogServiceMock,
        Set.of(),
        taskExecutor
      );

      List<CompletableFuture<ApiInformation>> result =
        catalogIndex.validateApiInformation();

      assertThat(result).isEmpty();
      verifyNoInteractions(apiCatalogServiceMock);
    }

    @Test
    void shouldValidateSingleApiInformation()
      throws ExecutionException, InterruptedException {
      catalogIndex = new ApiCatalogIndex(
        apiCatalogServiceMock,
        Set.of(apiInformation1),
        taskExecutor
      );
      doReturn(validatedApiInformation1)
        .when(apiCatalogServiceMock)
        .validateApiInformation(apiInformation1);

      List<CompletableFuture<ApiInformation>> result =
        catalogIndex.validateApiInformation();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).get()).isSameAs(validatedApiInformation1);
      verify(apiCatalogServiceMock).validateApiInformation(apiInformation1);
    }

    @Test
    void shouldValidateMultipleApiInformation()
      throws ExecutionException, InterruptedException {
      catalogIndex = new ApiCatalogIndex(
        apiCatalogServiceMock,
        Set.of(apiInformation1, apiInformation2, apiInformation3),
        taskExecutor
      );
      doReturn(validatedApiInformation1)
        .when(apiCatalogServiceMock)
        .validateApiInformation(apiInformation1);
      doReturn(validatedApiInformation2)
        .when(apiCatalogServiceMock)
        .validateApiInformation(apiInformation2);
      doReturn(validatedApiInformation3)
        .when(apiCatalogServiceMock)
        .validateApiInformation(apiInformation3);

      List<CompletableFuture<ApiInformation>> result =
        catalogIndex.validateApiInformation();

      assertThat(result).hasSize(3);
      assertThat(result.get(0).get()).isIn(
        validatedApiInformation1,
        validatedApiInformation2,
        validatedApiInformation3
      );
      assertThat(result.get(1).get()).isIn(
        validatedApiInformation1,
        validatedApiInformation2,
        validatedApiInformation3
      );
      assertThat(result.get(2).get()).isIn(
        validatedApiInformation1,
        validatedApiInformation2,
        validatedApiInformation3
      );
      verify(apiCatalogServiceMock).validateApiInformation(apiInformation1);
      verify(apiCatalogServiceMock).validateApiInformation(apiInformation2);
      verify(apiCatalogServiceMock).validateApiInformation(apiInformation3);
    }

    @Test
    void shouldHandleExceptionInValidation() {
      catalogIndex = new ApiCatalogIndex(
        apiCatalogServiceMock,
        Set.of(apiInformation1),
        taskExecutor
      );
      RuntimeException expectedException = new RuntimeException(
        "Validation failed"
      );
      doThrow(expectedException)
        .when(apiCatalogServiceMock)
        .validateApiInformation(apiInformation1);

      List<CompletableFuture<ApiInformation>> result =
        catalogIndex.validateApiInformation();

      assertThat(result).hasSize(1);
      assertThatThrownBy(() -> result.get(0).get())
        .isInstanceOf(ExecutionException.class)
        .hasCause(expectedException);
    }

    @Test
    void shouldAllowMultipleCallsToValidateApiInformation()
      throws ExecutionException, InterruptedException {
      catalogIndex = new ApiCatalogIndex(
        apiCatalogServiceMock,
        Set.of(apiInformation1),
        taskExecutor
      );
      doReturn(validatedApiInformation1)
        .when(apiCatalogServiceMock)
        .validateApiInformation(apiInformation1);

      List<CompletableFuture<ApiInformation>> result1 =
        catalogIndex.validateApiInformation();
      List<CompletableFuture<ApiInformation>> result2 =
        catalogIndex.validateApiInformation();

      assertThat(result1.get(0).get()).isSameAs(validatedApiInformation1);
      assertThat(result2.get(0).get()).isSameAs(validatedApiInformation1);
      verify(apiCatalogServiceMock, times(2)).validateApiInformation(
        apiInformation1
      );
    }
  }
}
