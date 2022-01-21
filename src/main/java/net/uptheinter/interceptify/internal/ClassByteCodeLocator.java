package net.uptheinter.interceptify.internal;

import net.bytebuddy.dynamic.ClassFileLocator;

import java.util.HashMap;
import java.util.Map;

class ClassByteCodeLocator implements ClassFileLocator {
    private final Map<String, byte[]> byteCode = new HashMap<>();

    public void put(String name, byte[] byteCode) {
        this.byteCode.put(name, byteCode);
    }

    @Override
    public Resolution locate(String name) {
        return new ByteArrayResolution(byteCode.get(name));
    }

    @Override
    public void close() {} // Not relevant to this implementation.
}
