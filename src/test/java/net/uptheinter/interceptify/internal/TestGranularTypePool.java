package net.uptheinter.interceptify.internal;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import net.uptheinter.interceptify.internal.GranularCacheProvider;
import net.uptheinter.interceptify.internal.GranularTypePool;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("SpellCheckingInspection")
class TestGranularTypePool {

    @Test
    void removeFromCache() {
        var mockGcp = mock(GranularCacheProvider.class);
        var mockCfl = mock(ClassFileLocator.class);
        var gtp = new GranularTypePool(mockGcp, mockCfl, TypePool.Default.ReaderMode.FAST);
        gtp.removeFromCache("asdff");
        verify(mockGcp).unregister("asdff");
    }
}