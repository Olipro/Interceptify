package net.uptheinter.interceptify.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestUtil {
    PrintStream oldErr = System.err;
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    PrintStream rdr = new PrintStream(buf);

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