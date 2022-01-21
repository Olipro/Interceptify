package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.uptheinter.interceptify.internal.AnnotationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@SuppressWarnings("SpellCheckingInspection")
class TestAnnotationMetadata {
    @Mock AnnotationDescription mockDesc;
    @Mock TypeDescription mockType;
    @Mock AnnotationDescription.Loadable<Annotation> mockLoadable;

    @BeforeEach
    void initMocks() {
        openMocks(this);
    }

    @Test
    void isAnnotation() {
        var fakeClass = getClass();
        when(mockDesc.getAnnotationType()).thenReturn(mockType);
        when(mockType.represents(fakeClass))
                .thenReturn(true)
                .thenReturn(false);
        var am = new AnnotationMetadata(mockDesc);
        assertTrue(am.isAnnotation(fakeClass));
        assertFalse(am.isAnnotation(fakeClass));
    }

    @Test
    void getField() {
        var fakeClass = Annotation.class;
        var fakeAntn = mock(Annotation.class);
        when(mockDesc.prepare(fakeClass)).thenReturn(mockLoadable);
        when(mockLoadable.load()).thenReturn(fakeAntn);
        assertEquals(fakeAntn, new AnnotationMetadata(mockDesc).getField(fakeClass));
    }
}