package net.uptheinter.interceptify.internal;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

class GranularTypePool extends TypePool.Default {
    private final GranularCacheProvider cacheProvider;

    public GranularTypePool(GranularCacheProvider cacheProvider,
                            ClassFileLocator classFileLocator,
                            ReaderMode readerMode) {
        super(cacheProvider, classFileLocator, readerMode);
        this.cacheProvider = cacheProvider;
    }

    public void removeFromCache(String name) {
        cacheProvider.unregister(name);
    }

    public static GranularTypePool of(ClassFileLocator classFileLocator) {
        return new GranularTypePool(new GranularCacheProvider(), classFileLocator, ReaderMode.FAST);
    }
}
