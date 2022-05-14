package net.uptheinter.interceptify.util;

import net.bytebuddy.ByteBuddy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TestJarFileEx {

    @Test
    void testEquals() throws IOException {
        var tmp = Files.createTempDirectory("tmp");
        tmp.toFile().deleteOnExit();
        var t = tmp.resolve("t.jar").toFile();
        var t2 = tmp.resolve("t2.jar").toFile();
        var bb = new ByteBuddy();
        bb.subclass(Object.class).name("test").make().toJar(t);
        bb.subclass(Object.class).name("test2").make().toJar(t2);
        try (var a = new JarFileEx(t);
             var a2 = new JarFileEx(t);
             var b = new JarFileEx(t2)) {
            assertEquals(a, a2);
            assertNotEquals(a, b);
        }
    }
}
