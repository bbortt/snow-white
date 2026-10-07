/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
package io.github.bbortt.snow.white.toolkit.junit.jupiter.extension;

import static org.slf4j.LoggerFactory.getLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;

/**
 * Captures what a class logged, so the claim about <em>which level</em> each degraded case is
 * reported at can be asserted instead of described.
 * <p>
 * The level matters to the decision, not just the message: "nothing configured" is the ordinary
 * state of most tests in this repository and is {@code DEBUG} so it stays out of the way, while
 * "configured and broken" is {@code WARN} because somebody needs to see it.
 */
final class CapturedLog implements AutoCloseable {

  private final Logger logger;
  private final Level originalLevel;
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  private CapturedLog(Class<?> type) {
    logger = (Logger) getLogger(type);
    originalLevel = logger.getLevel();

    // DEBUG is off by default, and one of the two levels under test is DEBUG.
    logger.setLevel(Level.DEBUG);
    appender.start();
    logger.addAppender(appender);
  }

  static CapturedLog of(Class<?> type) {
    return new CapturedLog(type);
  }

  List<ILoggingEvent> events() {
    return List.copyOf(appender.list);
  }

  List<ILoggingEvent> eventsAt(Level level) {
    return events()
      .stream()
      .filter(event -> event.getLevel() == level)
      .toList();
  }

  @Override
  public void close() {
    logger.detachAppender(appender);
    appender.stop();
    logger.setLevel(originalLevel);
  }
}
