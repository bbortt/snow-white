package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static io.github.bbortt.snow.white.toolkit.openapi.generator.SnowWhiteSpringServerGenerator.API_NAME_PROPERTY;
import static io.github.bbortt.snow.white.toolkit.openapi.generator.SnowWhiteSpringServerGenerator.API_VERSION_PROPERTY;
import static io.github.bbortt.snow.white.toolkit.openapi.generator.SnowWhiteSpringServerGenerator.SERVICE_NAME_PROPERTY;
import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConfigLoader;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

class SnowWhiteSpringServerGeneratorIT {

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
    ClientOptInput clientOptInput = fixture.toClientOptInput();
    var generator = new DefaultGenerator();
    generator.opts(clientOptInput).generate();
  }

  private SnowWhiteSpringServerGenerator fixture;

  @BeforeEach
  void beforeEachSetup() {
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
  void doesAddAnnotationIfValuesDefined() throws Exception {
    var inputSpec =
      "src/test/resources/" +
      getClass().getSimpleName() +
      "/valid-specification.yml";
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
      "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"sample-service\", apiName = \"Valid Specification\", apiVersion = \"1.2.3\", operationId = \"getExample\")"
    );
  }

  @Test
  void doesAddAnnotationFromCustomDefinitions() throws Exception {
    var inputSpec =
      "src/test/resources/" +
      getClass().getSimpleName() +
      "/custom-specification.yml";
    var outputDir =
      "target/test-generated/" +
      getClass().getSimpleName() +
      "/custom-specification";

    var codegenConfigurator = getCodegenConfigurator(inputSpec, outputDir);
    codegenConfigurator.addAdditionalProperty(
      API_NAME_PROPERTY,
      "info.x-custom-api-name"
    );
    codegenConfigurator.addAdditionalProperty(
      API_VERSION_PROPERTY,
      "info.x-custom-api-version"
    );
    codegenConfigurator.addAdditionalProperty(
      SERVICE_NAME_PROPERTY,
      "info.x-custom-service-name"
    );

    invokeGenerator(codegenConfigurator);

    var controllerFile = new File(
      outputDir + "/src/main/java/org/openapitools/api/ExampleApi.java"
    );
    assertThat(controllerFile).exists();

    var generatedCode = new String(readAllBytes(controllerFile.toPath()));
    assertThat(generatedCode).contains(
      "@io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation(serviceName = \"sample-service\", apiName = \"Custom Specification\", apiVersion = \"1.0.0\", operationId = \"getExample\")"
    );
  }

  @Test
  void doesNotAddAnnotationIfValuesNotDefined() throws Exception {
    var inputSpec =
      "src/test/resources/" +
      getClass().getSimpleName() +
      "/empty-specification.yml";
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
}
