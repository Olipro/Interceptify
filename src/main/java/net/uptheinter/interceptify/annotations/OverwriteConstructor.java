package net.uptheinter.interceptify.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotates a <b>static</b> method to indicate it will be intercepting a constructor
 * of the target class. The first parameter of the method MUST be the
 * type of the class you are intercepting. All subsequent parameters
 * should be whatever the constructor you are intercepting takes.</p>
 * <p>For example, if you wanted to intercept: </p><pre>SomeConstructor(String a, int b)</pre>
 * <p>Your method would be: </p><pre>public static void whateverName(SomeConstructor inst, String x, String y)</pre>
 * <p>Due to the way in which the JVM verifies constructors, you can have your method be called before or after
 * the intercepted constructor executes (or both), but <b>not neither.</b></p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OverwriteConstructor {
    /**
     * @return whether to call your method before the constructor runs.
     *         if false, <code>after()</code> must be true.
     */
    boolean before() default false;

    /**
     * @return whether to call your method after the constructor runs.
     *         if false, <code>before()</code> must be true.
     */
    boolean after() default true;
}
