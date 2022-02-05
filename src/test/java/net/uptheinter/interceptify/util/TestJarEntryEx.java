package net.uptheinter.interceptify.util;

import org.junit.jupiter.api.Test;

import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

class TestJarEntryEx {

    @Test
    void testEverything() {
        var je = new JarEntryEx("first.class");
        assertEquals("first", je.getClassName());
        je = new JarEntryEx((ZipEntry)new JarEntryEx("second.class"));
        assertEquals("second", je.getClassName());
        je = new JarEntryEx(new JarEntryEx("third.class"));
        assertEquals("third", je.getClassName());
    }
}