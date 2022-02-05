package net.uptheinter.interceptify.util;

import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * General utility class with a few modestly useful functions.
 */
public final class Util {
    private Util() {}

    /**
     * Prints an exception's message and stack trace to stderr.
     * @param exception The exception to print.
     */
    public static void DebugError(Throwable exception) {
        System.err.println(exception.getMessage());
        exception.printStackTrace();
    }

    /**
     * This is a convenience function for <i>trying to</i> convert a Path to a URI.<br>
     * Prints to stderr if an exception is thrown.
     * @param path A path that you wish to convert into a URL
     * @return a URL generated from the path, or null if it fails.
     */
    public static URL toURL(Path path) {
        try {
            return path.toUri().toURL();
        } catch (Throwable t) {
            DebugError(t);
        }
        return null;
    }

    /**
     * It's {@link Files#walk}, but it catches exceptions and logs them.
     * @param path same as for {@link Files#walk(Path, int, FileVisitOption...)}
     * @param depth ditto.
     * @return the return value also has the same semantics.
     */
    public static Stream<Path> walk(Path path, int depth) {
        try {
            return Files.walk(path, depth);
        } catch (Exception e) {
            DebugError(e);
        }
        return null;
    }
}
