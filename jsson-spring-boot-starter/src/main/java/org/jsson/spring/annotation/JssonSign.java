package org.jsson.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ensures that the Controller's output (ResponseBody) is canonicalized, signed,
 * and attached to a transient $jsson node before concluding the HTTP Request.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JssonSign {
}
