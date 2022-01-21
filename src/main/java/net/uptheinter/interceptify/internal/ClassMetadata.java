package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;

import java.util.Arrays;
import java.util.stream.Stream;

class ClassMetadata extends MetadataBase {
    private final TypeDescription cls;

    public ClassMetadata(String classFile, TypePool typePool) {
        cls = typePool.describe(classFile).resolve();
    }

    public ClassMetadata(TypeDescription cls) {
        this.cls = cls;
    }

    public Stream<MethodMetadata> getMethodsWithAnnotation(Class<?>... annotationType) {
        return cls.getDeclaredMethods()
                .stream()
                .filter(ModifierReviewable.OfByteCodeElement::isPublic)
                .map(MethodMetadata::new)
                .filter(m -> Arrays.stream(annotationType).anyMatch(m::hasAnnotation));
    }

    public Stream<MethodMetadata> getMethods() {
        return cls.getDeclaredMethods()
                .stream()
                .map(MethodMetadata::new);
    }

    public TypeDescription getTypeDesc() {
        return cls;
    }

    public String getTypeName() {
        return cls.getTypeName();
    }

    @Override
    protected AnnotationList getInternal() {
        return cls.getDeclaredAnnotations();
    }
}
