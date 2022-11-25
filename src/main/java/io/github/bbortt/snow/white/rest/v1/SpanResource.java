package io.github.bbortt.snow.white.rest.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpanResource implements SpanResourceApi {

  public ResponseEntity<Long> countRecordedSpans() {
    return ResponseEntity.ok(1L);
  }
}
