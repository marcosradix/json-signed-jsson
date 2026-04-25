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
    /**
     * Optional: Specify fields that MUST be included in the JSSON signature.
     * All other root fields will be ignored.
     */
    String[] includes() default {};

    /**
     * Optional: Specify fields that MUST be excluded from the JSSON signature.
     * Overrides includes if there are conflicts.
     */
    String[] excludes() default {};
}
