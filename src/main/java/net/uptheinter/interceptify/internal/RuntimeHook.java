package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
import net.uptheinter.interceptify.EntryPoint;
import net.uptheinter.interceptify.interfaces.StartupConfig;
import net.uptheinter.interceptify.util.JarFiles;

import java.lang.instrument.Instrumentation;
import java.net.URL;

/**
 * This is the primary point at which intercepting logic kicks off.
 * The class doesn't do all that much now but exists for the purposes
 * of facilitating Java Agent functionality in the future.
 */
public final class RuntimeHook {
    private static Instrumentation instr;

    private RuntimeHook() {}

    /**
     * This function kicks everything off, but you should use
     * {@link EntryPoint} - this class may be changed arbitrarily
     * in future releases.
     * @param cfg an implementation of a {@code StartupConfig}
     */
    public static void init(StartupConfig cfg) {
        if (instr == null) {
            System.err.println("Error, JVM was not invoked with Interceptify " +
                    "as a Java Agent (see -javaagent). Terminating");
            return;
        }
        JarFiles jarFiles = cfg.getJarFilesToInject();
        URL[] classpaths = cfg.getClasspaths().toArray(URL[]::new);
        new ClassInjector(new ByteBuddy(), instr, classpaths)
                .collectMetadataFrom(jarFiles)
                .applyAnnotationsAndIntercept();
    }

    /**
     * This function should be called by the JVM, however, you could also
     * ostensibly call it from your own Java Agent - though since multiple
     * Java Agents can be registered, such a scenario is unsupported.
     * @param args arguments from the JVM
     * @param instr an Instrumentation instance.
     */
    @SuppressWarnings("unused")
    public static void premain(String args, Instrumentation instr) {
        RuntimeHook.instr = instr;
    }
}
