package net.uptheinter.interceptify.util;

import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

/**
 * Essentially the same as {@code JarFile} with a few extra functions
 * for convenience.
 */
public class JarEntryEx extends JarEntry {
    /**
     * Constructs an entry with a string for the name
     * @param name The name to assign.
     * @see JarEntry#JarEntry(String)
     */
    @SuppressWarnings("unused")
    public JarEntryEx(String name) {
        super(name);
    }

    /**
     * Constructs an entry from an existing ZipEntry
     * @param ze the ZipEntry to construct from.
     * @see JarEntry#JarEntry(ZipEntry)
     */
    @SuppressWarnings("unused")
    public JarEntryEx(ZipEntry ze) {
        super(ze);
    }

    /**
     * Constructs an instance from an existing JarEntry.
     * @param je the JarEntry to construct from.
     * @see JarEntry#JarEntry(JarEntry)
     */
    public JarEntryEx(JarEntry je) {
        super(je);
    }

    /**
     * Obtains a fully-qualified class name for this entry.<br>
     * E.g. if this entry has the path {@code com/foo/bar/SomeClass.class}
     * it will return {@code com.foo.bar.SomeClass}
     * <p>If this entry does not represent a class file, it will either
     * return a garbage string or throw.</p>
     * @return the fully-qualified class name.
     * @throws IndexOutOfBoundsException You passed something that isn't a class and the function failed.
     */
    public String getClassName() {
        return getName().substring(0, getName().length() - 6).replace('/', '.');
    }
}
