package net.uptheinter.interceptify.util;

import java.util.function.UnaryOperator;

/**
 * This is a generic class for composing operations over a type.
 * Internal. If you need this class, you are suggested to make a copy of it,
 * with respect to the license.
 * @param <T> The boxed type.
 */
public class Boxed<T> {
    private T obj;

    /**
     * @param obj the object to box.
     */
    public Boxed(T obj) {
        this.obj = obj;
    }

    /**
     * @param func A callable type (such as a lambda) that will operate on
     *             the boxed object and return it after such changes.
     * @return {@code this}
     */
    @SuppressWarnings("UnusedReturnValue")
    public Boxed<T> run(UnaryOperator<T> func) {
        obj = func.apply(obj);
        return this;
    }

    /**
     * Intended for finalising operations when you no longer need to use {@code run()}
     * @return the boxed object
     */
    public T get() {
        return obj;
    }

    /**
     * If, for some reason you need to overwrite the type that is currently boxed,
     * use this.
     * @param obj the object to put inside this box.
     * @return {@code obj}
     */
    @SuppressWarnings("unused")
    public T set(T obj) {
        this.obj = obj;
        return this.obj;
    }
}
