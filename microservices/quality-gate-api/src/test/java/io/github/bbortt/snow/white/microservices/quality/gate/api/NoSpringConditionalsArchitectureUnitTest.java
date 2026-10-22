/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import com.tngtech.archunit.junit.AnalyzeClasses;
import io.github.bbortt.snow.white.archunit.rules.AbstractNoSpringConditionalsArchitectureTest;

@AnalyzeClasses(packagesOf = Main.class)
class NoSpringConditionalsArchitectureUnitTest
  extends AbstractNoSpringConditionalsArchitectureTest {}
