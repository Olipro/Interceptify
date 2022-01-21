package net.uptheinter.interceptify.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class annotated with this will be processed for interception.
 * You must provide a fully-qualified class name on this annotation.<br>
 * E.g: <code>"com.foo.bar.ClassIWantToIntercept"</code>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterceptClass {
    /**
     * @return The fully-qualified class name that should be intercepted.
     */
    String value();
}
