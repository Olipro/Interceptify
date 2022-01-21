package net.uptheinter.interceptify.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static net.uptheinter.interceptify.util.Util.DebugError;

/**
 * Represents a collection of Jar files, de-duplicated.
 */
public class JarFiles implements Iterable<JarFileEx> {
    private final Set<JarFileEx> jarFiles = new HashSet<>();

    /**
     * @see Set#iterator()
     * @return Same semantics as for {@link Set#stream()}
     */
    @Override
    public Iterator<JarFileEx> iterator() {
        return jarFiles.iterator();
    }

    /**
     * @see Set#stream()
     * @return Same semantics as for {@link Set#stream()}
     */
    public Stream<JarFileEx> stream() {
        return jarFiles.stream();
    }

    /**
     * Adds all jar files found <b>directly</b> in the {@code path} that you provide.<br>
     * In other words, it doesn't recurse.<br>
     * Does nothing if you don't pass a valid directory.
     * @param path A directory to search for jar files.
     */
    public void addFromDirectory(Path path) {
        if (path == null || !Files.isDirectory(path))
            return;
        Objects.requireNonNull(Util.walk(path, 1))
            .filter(Files::isRegularFile)
            .map(Path::toFile)
            .filter(f -> f.getName().endsWith(".jar"))
            .map(this::newJarFile)
            .filter(Objects::nonNull)
            .forEach(jarFiles::add);
    }

    /**
     * Tries to construct a {@link JarFileEx}, logs to stderr if it
     * fails and returns null.
     * @param file a valid Jar file to construct from
     * @return a new {@link JarFileEx} or null if construction fails.
     */
    private JarFileEx newJarFile(File file) {
        try {
            return new JarFileEx(file);
        } catch (IOException e) {
            DebugError(e);
        }
        return null;
    }
}
