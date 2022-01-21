package net.uptheinter.interceptify.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Essentially the same as {@link JarFile} with a few extra convenience
 * functions.
 */
public class JarFileEx extends JarFile {
    private final File file;
    private final List<JarEntryEx> cache = new ArrayList<>();

    /**
     * @param file The file to construct from.
     * @see JarFile#JarFile(File)
     * @throws IOException if construction fails.
     */
    public JarFileEx(File file) throws IOException {
        super(file);
        this.file = file;
    }

    /**
     * @return The {@code File} this instance was originally constructed with.
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns a list of all entries in this Jar file that are considered to be
     * Java classes.<br>
     * Basically, every file ending in <code>.class</code>
     * @return A {@code List} of entries identified as class files.
     */
    public List<JarEntryEx> getClasses() {
        if (cache.isEmpty())
            stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().endsWith(".class"))
                .map(JarEntryEx::new)
                .forEach(cache::add);
        return cache;
    }

    /**
     * Compares this instance to another. Two {@code JarFileEx} instances are considered
     * equal if they were both constructed with the same {@code File}.
     * @param other The object to compare - you likely want this to also be a JarFileEx.
     * @return whether the two entries are the same
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof JarFileEx)
            return getFile().equals(((JarFileEx) other).getFile());
        return super.equals(other);
    }

    /**
     * Obtains the hashCode from the {@code File} this instance is referencing.
     * @return the hashCode.
     * @see File#hashCode()
     */
    @Override
    public int hashCode() {
        return getFile().hashCode();
    }
}
