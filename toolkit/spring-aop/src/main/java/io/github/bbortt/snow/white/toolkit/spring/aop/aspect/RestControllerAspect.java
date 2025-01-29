package io.github.bbortt.snow.white.toolkit.spring.aop.aspect;

import static java.util.Objects.isNull;

import io.github.bbortt.snow.white.toolkit.annotation.SnowWhiteInformation;
import io.github.bbortt.snow.white.toolkit.spring.aop.config.SnowWhiteSpringAopProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class RestControllerAspect {

  private final SpanProvider spanProvider = new SpanProvider();

  private final SnowWhiteSpringAopProperties snowWhiteSpringAopProperties;

  public RestControllerAspect(
    SnowWhiteSpringAopProperties snowWhiteSpringAopProperties
  ) {
    this.snowWhiteSpringAopProperties = snowWhiteSpringAopProperties;
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
  public void restControllerPointcut() {}

  @Before("restControllerPointcut()")
  public void beforeRestMethod(JoinPoint joinPoint) {
    var currentSpan = spanProvider.getCurrentSpan();
    if (isNull(currentSpan)) {
      return; // No active span, nothing to enhance
    }

    if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
      var method = methodSignature.getMethod();
      if (method.isAnnotationPresent(SnowWhiteInformation.class)) {
        var snowWhiteInformation = method.getAnnotation(
          SnowWhiteInformation.class
        );

        log.trace("Enhancing span: [{}]", snowWhiteInformation);

        currentSpan.setAttribute(
          snowWhiteSpringAopProperties.getApiNameProperty(),
          snowWhiteInformation.apiName()
        );
        currentSpan.setAttribute(
          snowWhiteSpringAopProperties.getApiVersionProperty(),
          snowWhiteInformation.apiVersion()
        );
        currentSpan.setAttribute(
          snowWhiteSpringAopProperties.getOtelServiceNameProperty(),
          snowWhiteInformation.serviceName()
        );
      }
    }
  }
}
