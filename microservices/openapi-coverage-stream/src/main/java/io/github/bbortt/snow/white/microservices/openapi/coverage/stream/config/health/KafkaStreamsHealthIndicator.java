/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.health;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.ThreadMetadata;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Reports whether the Kafka Streams topology is actually processing, rather than whether the
 * process happens to be alive.
 * <p>
 * A Kafka Streams process outlives its topology: the JVM keeps running while the stream sits in
 * {@code ERROR} or {@code NOT_RUNNING} and consumes nothing, so the container being up is no
 * answer about this service's health.
 * <p>
 * {@code REBALANCING} counts as healthy. It is the ordinary response to a broker blip, a scale
 * event, or a sibling instance restarting, and it resolves on its own - reporting it as down would
 * have a liveness probe restart the pod part-way through a coverage calculation, discarding
 * in-flight work.
 */
@Component
@NullMarked
@RequiredArgsConstructor
@RealizesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
public class KafkaStreamsHealthIndicator implements HealthIndicator {

  static final String STATE_DETAIL = "state";
  static final String THREADS_DETAIL = "threads";

  private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

  @Override
  public Health health() {
    @Nullable
    KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

    if (isNull(kafkaStreams)) {
      return Health.down().withDetail(STATE_DETAIL, "UNINITIALIZED").build();
    }

    KafkaStreams.State state = kafkaStreams.state();

    return healthFor(state)
      .withDetail(STATE_DETAIL, state.name())
      .withDetail(THREADS_DETAIL, threadStates(kafkaStreams))
      .build();
  }

  /**
   * Maps a stream state to a verdict, exhaustively over {@link KafkaStreams.State}.
   * <p>
   * The switch is deliberately exhaustive rather than defaulting: a state added by a future Kafka
   * version must fail the build here instead of being silently reported as healthy.
   */
  private static Health.Builder healthFor(KafkaStreams.State state) {
    return switch (state) {
      case RUNNING, REBALANCING -> Health.up();
      case
        CREATED,
        PENDING_SHUTDOWN,
        PENDING_ERROR,
        ERROR,
        NOT_RUNNING -> Health.down();
    };
  }

  /**
   * Exposes each local stream thread's state, so a topology running with a dead thread is
   * distinguishable from one that is wholly down.
   */
  private static Map<String, String> threadStates(KafkaStreams kafkaStreams) {
    Set<ThreadMetadata> threads = kafkaStreams.metadataForLocalThreads();

    return threads
      .stream()
      .collect(toMap(ThreadMetadata::threadName, ThreadMetadata::threadState));
  }
}
