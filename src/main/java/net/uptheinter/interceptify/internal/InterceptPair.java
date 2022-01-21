package net.uptheinter.interceptify.internal;

import net.bytebuddy.dynamic.DynamicType;

import java.util.List;

class InterceptPair {
    private final DynamicType.Unloaded<?> interceptee;
    private final List<DynamicType.Unloaded<?>> interceptors;

    public InterceptPair(DynamicType.Unloaded<?> interceptee, List<DynamicType.Unloaded<?>> interceptors) {
        this.interceptee = interceptee;
        this.interceptors = interceptors;
    }

    public DynamicType.Unloaded<?> getInterceptee() {
        return interceptee;
    }

    public List<DynamicType.Unloaded<?>> getInterceptors() {
        return interceptors;
    }
}
