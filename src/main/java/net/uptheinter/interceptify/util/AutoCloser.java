package net.uptheinter.interceptify.util;

import java.util.function.Consumer;

/**
 * This is a generic type for facilitating a try-with-resources
 * on any operation where something should be done at the end of
 * the scope.<br>
 * What that happens to be can either just be a pair of function
 * calls or an actual object; it depends on the constructor you use.
 * @param <T> The type to be held by this AutoCloser instance
 */
@SuppressWarnings("unused")
public class AutoCloser<T> implements AutoCloseable {
    private final T obj;
    private final Object closer;

    /**
     * @param obj The object to pass to the closer when {@link AutoCloser#close()} is called
     * @param closer a function that will be called upon close.
     */
    public AutoCloser(T obj, Runnable closer) {
        this.obj = obj;
        this.closer = closer;
    }

    /**
     * @param init A function to be called immediately. Mainly for convenience
     *             so that you can pass method references to whatever does initialisation.
     * @param closer A function to be called at close, convenient in the same way as {@code init}
     */
    public AutoCloser(Runnable init, Runnable closer) {
        this.obj = null;
        this.closer = closer;
        init.run();
    }

    /**
     * @param obj An arbitrary object to be passed to the {@code closer} when {@link AutoCloser#close()} is called
     * @param closer A function that will receive the {@code obj} at close.
     */
    public AutoCloser(T obj, Consumer<T> closer) {
        this.obj = obj;
        this.closer = closer;
    }

    /**
     * @return If constructed with {@link AutoCloser#AutoCloser(Object, Runnable)}
     *         or {@link AutoCloser#AutoCloser(Object, Consumer)}, the object. otherwise, {@code null}.
     */
    public final T get() {
        return obj;
    }

    /**
     * Invokes the closer. This is intended to be called by the JVM through
     * try-with-resources. If you're calling it yourself, you probably shouldn't
     * be using this class at all.
     */
    @Override
    @SuppressWarnings("unchecked") // remove when on JRE16
    public final void close() {
        if (closer instanceof Runnable)
            ((Runnable)closer).run();
        else if (closer instanceof Consumer)
            ((Consumer<T>)closer).accept(obj);
    }
}
