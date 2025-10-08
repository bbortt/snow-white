/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static java.math.BigDecimal.ZERO;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.ORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.EntitiesQueryResponse;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.dto.Entity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class PagedBackstageEntitySpliteratorTest {

  @Mock
  private EntityApi backstageEntityApiMock;

  @Mock
  private EntitiesQueryResponse entityQueryResultMock;

  @Mock
  private EntitiesQueryResponse totalItemsQueryResultMock;

  @Mock
  private Entity entity1;

  @Mock
  private Entity entity2;

  @Mock
  private Entity entity3;

  @Mock
  private Entity entity4;

  private static final int PAGE_SIZE = 2;
  private static final int TOTAL_ITEMS = 4;

  private PagedBackstageEntitySpliterator fixture;

  @BeforeEach
  void beforeEachSetup() {
    doReturn(totalItemsQueryResultMock)
      .when(backstageEntityApiMock)
      .getEntitiesByQuery(null, 0, null, null, null, null, null, null);
    doReturn(BigDecimal.valueOf(TOTAL_ITEMS))
      .when(totalItemsQueryResultMock)
      .getTotalItems();

    fixture = new PagedBackstageEntitySpliterator(
      backstageEntityApiMock,
      PAGE_SIZE
    );
  }

  @Nested
  class ConstructorTests {

    @Test
    void shouldQueryTotalItemsOnConstruction() {
      verify(backstageEntityApiMock).getEntitiesByQuery(
        null,
        0,
        null,
        null,
        null,
        null,
        null,
        null
      );
      verify(totalItemsQueryResultMock).getTotalItems();
    }

    @Test
    void shouldInitializeWithCorrectPageSize() {
      assertThat(fixture)
        .asInstanceOf(type(Object.class))
        .hasNoNullFieldsOrPropertiesExcept("currentPage");
    }
  }

  @Nested
  class TryAdvanceTests {

    @Test
    void shouldFetchFirstPageOnFirstCall() {
      doReturn(entityQueryResultMock)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(PAGE_SIZE),
          eq(0),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(List.of(entity1, entity2))
        .when(entityQueryResultMock)
        .getItems();

      List<Entity> consumed = new ArrayList<>();

      boolean hasNext = fixture.tryAdvance(consumed::add);

      assertThat(hasNext).isTrue();
      assertThat(consumed).containsExactly(entity1);
      verify(backstageEntityApiMock).getEntitiesByQuery(
        List.of("metadata.annotations", "spec.definition"),
        PAGE_SIZE,
        0,
        null,
        null,
        null,
        null,
        null
      );
    }

    @Test
    void shouldConsumeMultipleItemsFromSamePage() {
      doReturn(entityQueryResultMock)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(PAGE_SIZE),
          anyInt(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(List.of(entity1, entity2))
        .when(entityQueryResultMock)
        .getItems();

      List<Entity> consumed = new ArrayList<>();

      fixture.tryAdvance(consumed::add);
      fixture.tryAdvance(consumed::add);

      assertThat(consumed).containsExactly(entity1, entity2);
      verify(backstageEntityApiMock, times(1)).getEntitiesByQuery(
        List.of("metadata.annotations", "spec.definition"),
        PAGE_SIZE,
        0,
        null,
        null,
        null,
        null,
        null
      );
    }

    @Test
    void shouldFetchNextPageWhenCurrentPageIsExhausted() {
      EntitiesQueryResponse secondPageResult = mock(
        EntitiesQueryResponse.class
      );

      doReturn(entityQueryResultMock)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(PAGE_SIZE),
          eq(0),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(secondPageResult)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(PAGE_SIZE),
          eq(PAGE_SIZE),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(List.of(entity1, entity2))
        .when(entityQueryResultMock)
        .getItems();
      doReturn(List.of(entity3, entity4)).when(secondPageResult).getItems();

      List<Entity> consumed = new ArrayList<>();

      fixture.tryAdvance(consumed::add);
      fixture.tryAdvance(consumed::add);
      fixture.tryAdvance(consumed::add);

      assertThat(consumed).containsExactly(entity1, entity2, entity3);
      verify(backstageEntityApiMock).getEntitiesByQuery(
        List.of("metadata.annotations", "spec.definition"),
        PAGE_SIZE,
        0,
        null,
        null,
        null,
        null,
        null
      );
      verify(backstageEntityApiMock).getEntitiesByQuery(
        List.of("metadata.annotations", "spec.definition"),
        PAGE_SIZE,
        PAGE_SIZE,
        null,
        null,
        null,
        null,
        null
      );
    }

    @Test
    void shouldReturnFalseWhenOffsetExceedsTotalItems() {
      doReturn(entityQueryResultMock)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(PAGE_SIZE),
          anyInt(),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(List.of(entity1, entity2))
        .when(entityQueryResultMock)
        .getItems();

      List<Entity> consumed = new ArrayList<>();

      fixture.tryAdvance(consumed::add);
      fixture.tryAdvance(consumed::add);
      fixture.tryAdvance(consumed::add);
      fixture.tryAdvance(consumed::add);
      boolean hasNext = fixture.tryAdvance(consumed::add);

      assertThat(hasNext).isFalse();
    }

    @Test
    void shouldHandleEmptyPage() {
      doReturn(entityQueryResultMock)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(PAGE_SIZE),
          eq(0),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(List.of()).when(entityQueryResultMock).getItems();

      List<Entity> consumed = new ArrayList<>();

      boolean hasNext = fixture.tryAdvance(consumed::add);

      assertThat(hasNext).isFalse();
    }
  }

  @Nested
  class TrySplitTests {

    @Test
    void shouldReturnNullForTrySplit() {
      Spliterator<Entity> split = fixture.trySplit();

      assertThat(split).isNull();
    }
  }

  @Nested
  class EstimateSizeTests {

    @Test
    void shouldReturnMaxLongForEstimateSize() {
      long size = fixture.estimateSize();

      assertThat(size).isEqualTo(Long.MAX_VALUE);
    }
  }

  @Nested
  class CharacteristicsTests {

    @Test
    void shouldReturnOrderedAndImmutableCharacteristics() {
      int characteristics = fixture.characteristics();

      assertThat(characteristics).isEqualTo(ORDERED | IMMUTABLE);
    }

    @Test
    void shouldHaveOrderedCharacteristic() {
      boolean hasOrdered = fixture.hasCharacteristics(ORDERED);

      assertThat(hasOrdered).isTrue();
    }

    @Test
    void shouldHaveImmutableCharacteristic() {
      boolean hasImmutable = fixture.hasCharacteristics(IMMUTABLE);

      assertThat(hasImmutable).isTrue();
    }
  }

  @Nested
  class StreamIntegrationTests {

    @Test
    void shouldWorkCorrectlyWithStreamSupport() {
      EntitiesQueryResponse firstPageResult = mock(EntitiesQueryResponse.class);
      EntitiesQueryResponse secondPageResult = mock(
        EntitiesQueryResponse.class
      );

      doReturn(firstPageResult)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(PAGE_SIZE),
          eq(0),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(secondPageResult)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(PAGE_SIZE),
          eq(PAGE_SIZE),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(List.of(entity1, entity2)).when(firstPageResult).getItems();
      doReturn(List.of(entity3, entity4)).when(secondPageResult).getItems();

      List<Entity> result = StreamSupport.stream(fixture, false).toList();

      assertThat(result).containsExactly(entity1, entity2, entity3, entity4);
    }

    @Test
    void shouldHandleLargePageSize() {
      int largePageSize = 100;
      doReturn(totalItemsQueryResultMock)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(null, 0, null, null, null, null, null, null);
      doReturn(BigDecimal.valueOf(10))
        .when(totalItemsQueryResultMock)
        .getTotalItems();

      List<Entity> entities = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        entities.add(mock(Entity.class));
      }

      doReturn(entityQueryResultMock)
        .when(backstageEntityApiMock)
        .getEntitiesByQuery(
          eq(List.of("metadata.annotations", "spec.definition")),
          eq(largePageSize),
          eq(0),
          isNull(),
          isNull(),
          isNull(),
          isNull(),
          isNull()
        );
      doReturn(entities).when(entityQueryResultMock).getItems();

      fixture = new PagedBackstageEntitySpliterator(
        backstageEntityApiMock,
        largePageSize
      );
      List<Entity> result = StreamSupport.stream(fixture, false).toList();

      assertThat(result).hasSize(10);
      verify(backstageEntityApiMock, times(1)).getEntitiesByQuery(
        List.of("metadata.annotations", "spec.definition"),
        largePageSize,
        0,
        null,
        null,
        null,
        null,
        null
      );
    }

    @Test
    void shouldHandleZeroTotalItems() {
      doReturn(ZERO).when(totalItemsQueryResultMock).getTotalItems();

      fixture = new PagedBackstageEntitySpliterator(
        backstageEntityApiMock,
        PAGE_SIZE
      );
      List<Entity> consumed = new ArrayList<>();

      boolean hasNext = fixture.tryAdvance(consumed::add);

      assertThat(hasNext).isFalse();
      assertThat(consumed).isEmpty();

      verify(backstageEntityApiMock, never()).getEntitiesByQuery(
        List.of("metadata.annotations", "spec.definition"),
        PAGE_SIZE,
        0,
        null,
        null,
        null,
        null,
        null
      );
    }
  }
}
