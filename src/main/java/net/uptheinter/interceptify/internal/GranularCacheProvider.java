package net.uptheinter.interceptify.internal;

import net.bytebuddy.pool.TypePool.CacheProvider;
import net.bytebuddy.pool.TypePool.Resolution;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class GranularCacheProvider implements CacheProvider {
    private final ConcurrentMap<String, Resolution> storage = new ConcurrentHashMap<>();

    @Override
    public Resolution find(String name) {
        return storage.get(name);
    }

    public Resolution register(String name, Resolution resolution) {
        var cached = storage.putIfAbsent(name, resolution);
        return cached == null ? resolution : cached;
    }

    public void unregister(String name) {
        storage.remove(name);
    }

    public void clear() {
        storage.clear();
    }
}
