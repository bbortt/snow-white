package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openapitools.codegen.CodegenType.SERVER;

import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.codegen.CodegenConfigLoader;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

class SnowWhiteSpringServerGeneratorTest {

  private static CodegenConfigurator getCodegenConfigurator(
    String inputSpec,
    String outputDir
  ) {
    return new CodegenConfigurator()
      .setGeneratorName(SnowWhiteSpringServerGenerator.NAME)
      .setInputSpec(inputSpec)
      .setOutputDir(outputDir)
      .addAdditionalProperty("delegatePattern", true)
      .addAdditionalProperty("interfaceOnly", true)
      .addAdditionalProperty("useSpringBoot3", true);
  }

  private static void invokeGenerator(CodegenConfigurator fixture) {
    var clientOptInput = fixture.toClientOptInput();
    var generator = new DefaultGenerator();
    generator.opts(clientOptInput).generate();
  }

  private SnowWhiteSpringServerGenerator fixture;

  @BeforeEach
  void setUp() {
    fixture = new SnowWhiteSpringServerGenerator();

    fixture.additionalProperties().put("delegatePattern", true);
    fixture.additionalProperties().put("interfaceOnly", true);
    fixture.additionalProperties().put("useSpringBoot3", true);
  }

  @Test
  void isRegisteredWithinSpi() {
    var codegenConfig = CodegenConfigLoader.forName(
      SnowWhiteSpringServerGenerator.NAME
    );
    assertThat(codegenConfig)
      .isNotNull()
      .isInstanceOf(SnowWhiteSpringServerGenerator.class);
  }

  @Test
  void overridesSpringGeneratorName() {
    assertThat(fixture.getName()).isEqualTo(
      SnowWhiteSpringServerGenerator.NAME
    );
  }

  @Test
  void overridesSpringGeneratorHelp() {
    assertThat(fixture.getHelp()).isEqualTo(
      "Enhances the OpenAPI Spring generator with snow-white specific information."
    );
  }

  @Test
  void isServerGenerator() {
    assertThat(fixture.getTag()).isEqualTo(SERVER);
  }

  @Test
  void doesAddAnnotationIfValuesDefined() throws Exception {
    var inputSpec =
      "src/test/resources/SnowWhiteSpringGeneratorUnitTest/valid-specification.yml";
    var outputDir =
      "target/test-generated/" +
      getClass().getSimpleName() +
      "/valid-specification";

    var codegenConfigurator = getCodegenConfigurator(inputSpec, outputDir);
    invokeGenerator(codegenConfigurator);

    var controllerFile = new File(
      outputDir + "/src/main/java/org/openapitools/api/ExampleApi.java"
    );
    assertThat(controllerFile).exists();

    var generatedCode = new String(readAllBytes(controllerFile.toPath()));
    assertThat(generatedCode).contains(
      "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"sample-service\", apiName = \"Valid Specification\", apiVersion = \"1.2.3\")"
    );
  }

  @Test
  void doesAddAnnotationFromCustomDefinitions() throws Exception {
    var inputSpec =
      "src/test/resources/SnowWhiteSpringGeneratorUnitTest/custom-specification.yml";
    var outputDir =
      "target/test-generated/" +
      getClass().getSimpleName() +
      "/custom-specification";

    fixture.apiName = "x-custom-api-name";
    fixture.apiVersion = "x-custom-api-version";
    fixture.otelServiceName = "x-custom-otel-service-name";

    invokeGeneratorBasedOnFixture(inputSpec, outputDir);

    var controllerFile = new File(
      outputDir + "/src/main/java/org/openapitools/api/ExampleApi.java"
    );
    assertThat(controllerFile).exists();

    var generatedCode = new String(readAllBytes(controllerFile.toPath()));
    assertThat(generatedCode).contains(
      "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"sample-service\", apiName = \"Custom Specification\", apiVersion = \"1.0.0\")"
    );
  }

  @Test
  void doesAddAnnotationFromOpenApiDefinitions() throws Exception {
    var inputSpec =
      "src/test/resources/SnowWhiteSpringGeneratorUnitTest/openapi-specification.yml";
    var outputDir =
      "target/test-generated/" +
      getClass().getSimpleName() +
      "/openapi-specification";

    fixture.apiName = "title";

    invokeGeneratorBasedOnFixture(inputSpec, outputDir);

    var controllerFile = new File(
      outputDir + "/src/main/java/org/openapitools/api/ExampleApi.java"
    );
    assertThat(controllerFile).exists();

    var generatedCode = new String(readAllBytes(controllerFile.toPath()));
    assertThat(generatedCode).contains(
      "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"sample-service\", apiName = \"Sample API\", apiVersion = \"1.2.3\")"
    );
  }

  @Test
  void doesNotAddAnnotationIfValuesNotDefined() throws Exception {
    var inputSpec =
      "src/test/resources/SnowWhiteSpringGeneratorUnitTest/empty-specification.yml";
    var outputDir =
      "target/test-generated/" +
      getClass().getSimpleName() +
      "/empty-specification";

    var codegenConfigurator = getCodegenConfigurator(inputSpec, outputDir);
    invokeGenerator(codegenConfigurator);

    var controllerFile = new File(
      outputDir + "/src/main/java/org/openapitools/api/ExampleApi.java"
    );
    assertThat(controllerFile).exists();

    var generatedCode = new String(readAllBytes(controllerFile.toPath()));
    assertThat(generatedCode).doesNotContain(
      "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation"
    );
  }

  public static Stream<String> blankProperties() {
    return Stream.of(null, "");
  }

  public static Stream<String> invalidProperties() {
    return Stream.concat(blankProperties(), Stream.of("foo"));
  }

  @ParameterizedTest
  @MethodSource("invalidProperties")
  void throwsGivenInvalidTitle(String title) {
    var inputSpec =
      "src/test/resources/SnowWhiteSpringGeneratorUnitTest/valid-specification.yml";
    var outputDir = "target/test-generated/noop";

    fixture.apiName = title;

    assertThatThrownBy(() -> invokeGeneratorBasedOnFixture(inputSpec, outputDir)
    )
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Could not generate api file for 'example'")
      .rootCause()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        "The api name property must either be 'title' or start with 'x-'"
      );
  }

  @ParameterizedTest
  @MethodSource("invalidProperties")
  void throwsGivenInvalidVersion(String version) {
    var inputSpec =
      "src/test/resources/SnowWhiteSpringGeneratorUnitTest/valid-specification.yml";
    var outputDir = "target/test-generated/noop";

    fixture.apiVersion = version;

    assertThatThrownBy(() -> invokeGeneratorBasedOnFixture(inputSpec, outputDir)
    )
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Could not generate api file for 'example'")
      .rootCause()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        "The api version property must either be 'version' or start with 'x-'"
      );
  }

  @ParameterizedTest
  @MethodSource("blankProperties")
  void throwsGivenInvalidOtelServiceName(String otelServiceName) {
    var inputSpec =
      "src/test/resources/SnowWhiteSpringGeneratorUnitTest/valid-specification.yml";
    var outputDir = "target/test-generated/noop";

    fixture.otelServiceName = otelServiceName;

    assertThatThrownBy(() -> invokeGeneratorBasedOnFixture(inputSpec, outputDir)
    )
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Could not generate api file for 'example'")
      .rootCause()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The OTEL service name property must start with 'x-'");
  }

  private void invokeGeneratorBasedOnFixture(
    String inputSpec,
    String outputDir
  ) {
    fixture.setInputSpec(inputSpec);
    fixture.setOutputDir(outputDir);

    var codegenConfigurator = getCodegenConfigurator(inputSpec, outputDir);
    var clientOptInput = codegenConfigurator.toClientOptInput().config(fixture);
    var defaultGenerator = new DefaultGenerator();
    defaultGenerator.opts(clientOptInput).generate();
  }
}
