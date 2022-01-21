package net.uptheinter.interceptify.internal;

import net.uptheinter.interceptify.internal.ByteArrayResolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestByteArrayResolution {

    @Test
    void isResolved() {
        var arr = new byte[0];
        assertTrue(new ByteArrayResolution(arr).isResolved());
        assertFalse(new ByteArrayResolution(null).isResolved());
    }

    @Test
    void resolve() {
        var arr = new byte[1];
        assertEquals(new ByteArrayResolution(arr).resolve(), arr);
    }
}