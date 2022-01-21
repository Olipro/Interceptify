package net.uptheinter.interceptify;

import net.uptheinter.interceptify.internal.RuntimeHook;
import net.uptheinter.interceptify.interfaces.StartupConfig;

import java.lang.instrument.Instrumentation;

/**
 * This is the entrypoint for setting up interception. In most cases, your application's
 * main function should create a StartupConfig implementation and then immediately call
 * {@link EntryPoint#entryPoint(StartupConfig, String[])}
 */
public final class EntryPoint {

    private EntryPoint() {}

    /**
     * This is the entrypoint to Interceptify. You generally want to call this
     * immediately from your {@code main()} function.
     * @param cfg an implementation of StartupConfig
     * @param args These arguments will be passed to the target application's main.
     */
    @SuppressWarnings("unused")
    public static void entryPoint(StartupConfig cfg, String[] args) {
        RuntimeHook.init(cfg);
        cfg.getRealMain().accept(args);
    }

    /**
     * This function should be called by the JVM by setting this class up as a Java Agent.
     * See
     * <a href="https://docs.oracle.com/javase/10/docs/api/java/lang/instrument/package-summary.html">{@code -javaagent}</a>
     * for information.
     * <p>This functionality is currently reserved for future use. Consequently, you are
     * required to initialise this library as a Java Agent. This is primarily to avoid any
     * complaints/problems when this functionality is leveraged in a future version and
     * someone wonders why things don't work because they didn't use {@code -javaagent} on
     * an older version.</p>
     * @param args args from the JVM via {@code -javaagent}
     * @param inst an instance of {@link Instrumentation}, incidentally, also passed by the JVM.
     */
    public static void premain(String args, Instrumentation inst) {
        RuntimeHook.premain(args, inst);
    }

    /**
     * Placeholder function for future runtime Java Agent registration.
     * @param args args from the JVM
     * @param inst an {@link Instrumentation} instance from the JVM
     */
    @SuppressWarnings("unused")
    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }
}
