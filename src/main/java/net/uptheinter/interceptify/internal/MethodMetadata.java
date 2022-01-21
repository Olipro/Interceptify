package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MethodMetadata extends MetadataBase {
    private final MethodDescription.InDefinedShape method;

    public MethodMetadata(MethodDescription.InDefinedShape method) {
        this.method = method;
    }

    public boolean isConstructor() { return method.isConstructor(); }

    public boolean isStatic() {
        return method.isStatic();
    }

    public boolean isInstanceMethod() {
        return !isStatic();
    }

    public MethodDescription.InDefinedShape getShape() {
        return method;
    }

    public Stream<ParameterMetadata> getParameters() {
        return method.getParameters()
                .stream()
                .map(ParameterMetadata::new);
    }

    public List<ParameterMetadata> getParameterList() {
        return getParameters().collect(Collectors.toList());
    }

    public String getName() {
        return method.getName();
    }

    public TypeDescription getDeclaringType() {
        return method.getDeclaringType();
    }

    @Override
    protected AnnotationList getInternal() {
        return method.getDeclaredAnnotations();
    }
}
