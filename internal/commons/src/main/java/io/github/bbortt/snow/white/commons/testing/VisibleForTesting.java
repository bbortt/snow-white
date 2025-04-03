package io.github.bbortt.snow.white.commons.testing;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the visibility of a type, method, or field has been relaxed from {@code private} to {@code public}, {@code protected}, or package-private specifically to enable testing.
 * <p>
 * This annotation serves as documentation that the annotated element would have a more restrictive access modifier if not for testing requirements.
 * It signals to developers that despite its visibility, the annotated element is not meant to be part of the public API and should only be accessed by test code.
 * <p>
 * The annotation is retained only at the source level and is not included in the compiled class files or runtime environment.
 */
@Retention(SOURCE)
@Target({ FIELD, METHOD, TYPE })
public @interface VisibleForTesting {
}
