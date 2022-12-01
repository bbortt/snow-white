package io.github.bbortt.snow.white.rest.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SpanResourceTest {

  @Test
  void countRecordedSpans() {
    given().when().get("/api/rest/v1/spans/count").then().statusCode(200).body(is("1"));
  }
}
