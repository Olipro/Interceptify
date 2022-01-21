package net.uptheinter.interceptify.interfaces;

import net.uptheinter.interceptify.util.JarFiles;
import net.uptheinter.interceptify.util.Util;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This interface must be implemented by your application hook.
 * It is used to identify JAR files that will (potentially)
 * be loaded into the process
 */
public interface StartupConfig {
    /**
     * This should return a {@link JarFiles} object that you have populated with
     * whatever Jar files you wish to be processed for injection and interception.
     * @return the {@link JarFiles} to be processed.
     */
    JarFiles getJarFilesToInject();


    /**
     * This should return a {@link List} of URLs to be included in a "fake"
     * classpath. By default, this is just whatever Jar files you've listed in
     * {@link StartupConfig#getJarFilesToInject()} but if you have other dependencies
     * (whether jar files, or just a directory of .class files) that, for whatever reason,
     * are <b>not</b> known to the JVM via the
     * <a href="https://docs.oracle.com/en/java/javase/13/docs/specs/man/java.html">{@code -cp} argument</a>,
     * list them here.
     * @return a list of URLs to be used for the "fake" classpath.
     */
    default List<URL> getClasspaths() {
            return getJarFilesToInject().stream()
                    .map(jar -> Util.toURL(jar.getFile().toPath()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
    }

    /**
     * This function should return the entrypoint to execute after all interception
     * and class modification has completed. In most cases, you can likely write your method
     * as simply as {@code return TheClass::main}
     * @return The true main() function of the intercepted application
     */
    Consumer<String[]> getRealMain();
}
