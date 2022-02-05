package net.uptheinter.interceptify.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestUtil {
    final PrintStream oldErr = System.err;
    final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    final PrintStream rdr = new PrintStream(buf);

    TestUtil() {
        System.setErr(rdr);
    }

    @AfterEach
    void tearDown() {
        System.setErr(oldErr);
    }

    @Test
    void debugError() {
        Util.DebugError(new RuntimeException("check for this string"));
        assertTrue(buf.toString(StandardCharsets.UTF_8).contains("check for this string"));
    }

    @Test
    void toURL() throws URISyntaxException {
        var mockPath = mock(Path.class);
        when(mockPath.toUri()).thenThrow(new RuntimeException(""))
                .thenReturn(new URI("http://google.com"));
        assertDoesNotThrow(() -> Util.toURL(mockPath));
        assertNotNull(Util.toURL(mockPath));
    }

    @Test
    void walk() {
        assertNotNull(Util.walk(Path.of(""), 1));
        assertDoesNotThrow(() -> assertNull(Util.walk(null, 0)));
    }
}