package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.annotation.AnnotationDescription;

import java.lang.annotation.Annotation;

class AnnotationMetadata {
    private final AnnotationDescription annotation;

    public AnnotationMetadata(AnnotationDescription annotation) {
        this.annotation = annotation;
    }

    public boolean isAnnotation(Class<?> annotationType) {
        return annotation.getAnnotationType().represents(annotationType);
    }

    public <T extends Annotation> T getField(Class<T> annotationType) {
        return annotation.prepare(annotationType).load();
    }
}
