/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.FileCopyUtils.copyToByteArray;

import java.io.IOException;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ClassPathResource;

@NoArgsConstructor
public final class TestUtils {

  public static String getResourceContent(String resourcePath)
    throws IOException {
    return new String(
      copyToByteArray(new ClassPathResource(resourcePath).getInputStream()),
      UTF_8
    );
  }
}
