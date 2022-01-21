package net.uptheinter.interceptify.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotates a <b>static</b> method to indicate it will be intercepting a method in a target.
 * the {@code value()} of this annotation must be the name of the method you want to intercept.
 * You will be passed an instance of a {@code Method} object which you can optionally {@code .invoke()} if you
 * want to have the intercepted function's method execute, otherwise, it won't.<br>
 * If you have no need to call the original method, you can omit the {@code Method} argument from yours.
 * Whatever you call your own method is <b>irrelevant</b>.</p>
 * <p>Here are some examples - assume they all are for methods in a class called {@code TargetClass}</p>
 * <p><b>theirs:</b> {@code int someMethod(String, int)}<br>
 * <b>yours:</b> {@code public static int myInterceptor(TargetClass, Method, String, int)}</p>
 * <p><b>theirs:</b> {@code static String someStaticMethod(boolean, Integer)}<br>
 * <b>yours:</b> {@code public static String myInterceptor(Method, boolean, Integer)}</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OverwriteMethod {
    /**
     * @return The name of the method to intercept. E.g. {@code "SomeMethod"}
     */
    String value();
}
