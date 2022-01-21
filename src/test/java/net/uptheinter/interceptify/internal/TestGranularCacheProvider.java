package net.uptheinter.interceptify.internal;

import net.bytebuddy.pool.TypePool;
import net.uptheinter.interceptify.internal.GranularCacheProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings("SpellCheckingInspection")
class TestGranularCacheProvider {

    @Test
    void testAll() {
        var gcp = new GranularCacheProvider();
        var res = mock(TypePool.Resolution.class);
        assertNull(gcp.find("asdf"));
        assertEquals(res, gcp.register("asdf", res));
        assertEquals(res, gcp.find("asdf"));
        assertNull(gcp.find("asdf\0"));
        gcp.register("dede", res);
        gcp.unregister("asdf");
        assertNull(gcp.find("asdf"));
        assertEquals(res, gcp.find("dede"));
        gcp.clear();
        assertNull(gcp.find("dede"));
    }
}