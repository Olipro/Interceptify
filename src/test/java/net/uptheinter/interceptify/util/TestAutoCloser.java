package net.uptheinter.interceptify.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestAutoCloser {
    Object chk;
    boolean success1;
    boolean success2;
    boolean success3;

    @SuppressWarnings("EmptyTryBlock")
    @Test
    void testAutoCloser() {
        var dummy = new Object();
        try (var closer = new AutoCloser<>(dummy, obj -> chk = obj)) {
            assertEquals(closer.get(), dummy);
        }
        assertEquals(dummy, chk);
        try (var unused = new AutoCloser<>(() -> success1 = true, () -> success2 = true)) {}
        assertTrue(success1);
        assertTrue(success2);
        try (var unused = new AutoCloser<>(dummy, () -> success3 = true)) {}
        assertTrue(success3);
    }
}