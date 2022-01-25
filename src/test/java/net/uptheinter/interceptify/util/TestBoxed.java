package net.uptheinter.interceptify.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestBoxed {

    @Test
    void runAndGet() {
        var boxed = new Boxed<>("astr");
        boxed.run(s -> s + "ing");
        assertEquals("astring", boxed.get());
    }

    @Test
    void set() {
        var boxed = new Boxed<>("astr");
        boxed.set("ing");
        assertEquals(boxed.get(), "ing");
    }
}