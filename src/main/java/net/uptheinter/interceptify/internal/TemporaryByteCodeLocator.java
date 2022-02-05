package net.uptheinter.interceptify.internal;

import net.bytebuddy.dynamic.ClassFileLocator;

import java.util.function.Supplier;

class TemporaryByteCodeLocator implements ClassFileLocator {
    private String name;
    private byte[] byteCode;

    public <T> T with(String name, byte[] byteCode, Supplier<T> func) {
        this.name = name;
        this.byteCode = byteCode;
        var ret = func.get();
        this.name = null;
        this.byteCode = null;
        return ret;
    }

    @Override
    public Resolution locate(String name) {
        return new ByteArrayResolution(name.equals(this.name) ? byteCode : null);
    }

    @Override
    public void close() {} // Not relevant to this implementation
}
