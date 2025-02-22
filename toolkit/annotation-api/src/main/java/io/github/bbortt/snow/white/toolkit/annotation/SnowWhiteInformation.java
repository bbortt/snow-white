package io.github.bbortt.snow.white.toolkit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SnowWhiteInformation {
  String serviceName();

  String apiName();

  String apiVersion();

  String operationId() default "";
}
