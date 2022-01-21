package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.uptheinter.interceptify.internal.ClassMetadata;
import net.uptheinter.interceptify.internal.MethodMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestClassMetadata {
    @Mock Stream<Object> mockStrm;
    @Mock(answer = RETURNS_DEEP_STUBS) TypeDescription mockTd;
    ClassMetadata classMetadata;

    @BeforeEach
    void Init() {
        classMetadata = new ClassMetadata(mockTd);
    }

    @Test
    void getMethodsWithAnnotation() {

        var mockMd = mock(MethodMetadata.class);
        when(mockTd
                .getDeclaredMethods()
                .stream()
                .filter(any())
                .map(any())
                .filter(argThat(arg -> arg.test(mockMd)))).thenReturn(mockStrm);
        when(mockMd.hasAnnotation(any(Class.class))).thenReturn(true);
        assertEquals(mockStrm, classMetadata.getMethodsWithAnnotation(Annotation.class));
        verify(mockTd
                .getDeclaredMethods()
                .stream()
                .filter(any())
                .map(MethodMetadata::new)).filter(any());
        verify(mockMd).hasAnnotation(any(Class.class));
    }

    @Test
    void getMethods() {
        when(mockTd
                .getDeclaredMethods()
                .stream()
                .map(any())).thenReturn(mockStrm);
        assertEquals(mockStrm, classMetadata.getMethods());
        verify(mockTd
                .getDeclaredMethods()
                .stream())
                .map(any());
    }

    @Test
    void getTypeDesc() {
        assertEquals(mockTd, classMetadata.getTypeDesc());
    }

    @Test
    void getTypeName() {
        when(mockTd.getTypeName()).thenReturn("FakeTypeName");
        assertEquals("FakeTypeName", classMetadata.getTypeName());
    }

    @Test
    void getInternal() {
        var mockAnnot = mock(AnnotationList.class);
        when(mockTd.getDeclaredAnnotations()).thenReturn(mockAnnot);
        assertEquals(mockAnnot, classMetadata.getInternal());
    }
}