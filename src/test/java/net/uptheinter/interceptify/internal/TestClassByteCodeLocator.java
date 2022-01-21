package net.uptheinter.interceptify.internal;

import net.uptheinter.interceptify.internal.ClassByteCodeLocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpellCheckingInspection")
class TestClassByteCodeLocator {
    private final String testStr = "testStr";
    private final ClassByteCodeLocator cbcl = new ClassByteCodeLocator();
    private final byte[] arr = new byte[1];

    @Test
    void put() {
        cbcl.put(testStr, arr);
    }

    @Test
    void locate() {
        var loc = cbcl.locate(testStr);
        assertFalse(loc.isResolved());
        cbcl.put(testStr, arr);
        loc = cbcl.locate(testStr);
        assertTrue(loc.isResolved());
        assertEquals(loc.resolve(), arr);
    }

    @Test
    void close() {
        cbcl.close(); // shouldn't do anything.
    }
}