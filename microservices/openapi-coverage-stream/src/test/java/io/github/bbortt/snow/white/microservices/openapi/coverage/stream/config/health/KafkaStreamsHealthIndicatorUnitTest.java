/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.health;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.health.KafkaStreamsHealthIndicator.STATE_DETAIL;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.health.KafkaStreamsHealthIndicator.THREADS_DETAIL;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KafkaStreams.State;
import org.apache.kafka.streams.ThreadMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

@ExtendWith(MockitoExtension.class)
class KafkaStreamsHealthIndicatorUnitTest {

  private static final String STATE_UNINITIALIZED = "UNINITIALIZED";

  @Mock
  private StreamsBuilderFactoryBean streamsBuilderFactoryBeanMock;

  @Mock
  private KafkaStreams kafkaStreamsMock;

  @InjectMocks
  private KafkaStreamsHealthIndicator fixture;

  @Nested
  class HealthTest {

    @Test
    void shouldReportDownWithUninitializedStateOnly_withoutKafkaStreamsInstance() {
      doReturn(null).when(streamsBuilderFactoryBeanMock).getKafkaStreams();

      var health = fixture.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
      assertThat(health.getDetails()).containsExactly(
        Map.entry(STATE_DETAIL, STATE_UNINITIALIZED)
      );
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = { "RUNNING", "REBALANCING" })
    @VerifiesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
    void shouldReportUp_whenStreamIsProcessing(State state) {
      givenStreamIn(state, emptySet());

      var health = fixture.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry(STATE_DETAIL, state.name());
    }

    @ParameterizedTest
    @EnumSource(
      value = State.class,
      names = { "RUNNING", "REBALANCING" },
      mode = EXCLUDE
    )
    @VerifiesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
    void shouldReportDown_whenStreamIsNotProcessing(State state) {
      givenStreamIn(state, emptySet());

      var health = fixture.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
      assertThat(health.getDetails()).containsEntry(STATE_DETAIL, state.name());
    }

    @Test
    void shouldReportEveryThreadState_whenARunningStreamHasADeadThread() {
      givenStreamIn(
        State.RUNNING,
        Set.of(
          threadMetadata("stream-thread-1", "RUNNING"),
          threadMetadata("stream-thread-2", "DEAD")
        )
      );

      var health = fixture.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry(
        THREADS_DETAIL,
        Map.of("stream-thread-1", "RUNNING", "stream-thread-2", "DEAD")
      );
    }

    @Test
    void shouldReportNoThreads_whenTheStreamHasNone() {
      givenStreamIn(State.NOT_RUNNING, emptySet());

      var health = fixture.health();

      assertThat(health.getDetails()).containsEntry(THREADS_DETAIL, Map.of());
    }

    private void givenStreamIn(State state, Set<ThreadMetadata> threads) {
      doReturn(kafkaStreamsMock)
        .when(streamsBuilderFactoryBeanMock)
        .getKafkaStreams();
      doReturn(state).when(kafkaStreamsMock).state();
      doReturn(threads).when(kafkaStreamsMock).metadataForLocalThreads();
    }

    private ThreadMetadata threadMetadata(String threadName, String state) {
      var threadMetadataMock = mock(ThreadMetadata.class);

      doReturn(threadName).when(threadMetadataMock).threadName();
      doReturn(state).when(threadMetadataMock).threadState();

      return threadMetadataMock;
    }
  }
}
