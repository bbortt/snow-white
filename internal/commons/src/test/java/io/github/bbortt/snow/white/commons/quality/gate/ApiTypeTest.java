package io.github.bbortt.snow.white.commons.quality.gate;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.UNSPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApiTypeTest {

  @ParameterizedTest
  @EnumSource(ApiType.class)
  void fromString(ApiType apiType) {
    assertThat(ApiType.apiType(apiType.name())).isEqualTo(apiType);

    assertThat(ApiType.apiType(apiType.name().toLowerCase())).isEqualTo(
      apiType
    );
  }

  @Test
  void fromStringNullValue() {
    assertThat(ApiType.apiType((String) null)).isEqualTo(UNSPECIFIED);
  }

  @ParameterizedTest
  @EnumSource(ApiType.class)
  void fromIntegerValue(ApiType apiType) {
    assertThat(ApiType.apiType(apiType.getVal())).isEqualTo(apiType);
  }

  @Test
  void fromIntegerNullValue() {
    assertThat(ApiType.apiType((Integer) null)).isEqualTo(UNSPECIFIED);
  }
}
