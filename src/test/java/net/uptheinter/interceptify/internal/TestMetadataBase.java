package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.annotation.AnnotationList;
import net.uptheinter.interceptify.internal.AnnotationMetadata;
import net.uptheinter.interceptify.internal.MetadataBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestMetadataBase {
    @Mock(answer = CALLS_REAL_METHODS)
    MetadataBase metadataBase;
    @Mock(answer = RETURNS_DEEP_STUBS) AnnotationList mockAnnot;
    @Mock
    AnnotationMetadata mockAmd;

    @Test
    void hasAnnotation() {
        when(mockAnnot.stream().map(any()).anyMatch(argThat(arg -> arg.test(mockAmd))))
                .thenReturn(true);
        when(mockAmd.isAnnotation(any())).thenReturn(true);
        when(metadataBase.getInternal()).thenReturn(mockAnnot);
        assertTrue(metadataBase.hasAnnotation(Class.class));
        verify(mockAnnot.stream().map(any())).anyMatch(any());
        verify(mockAmd).isAnnotation(Class.class);
    }

    @Test
    void getAnnotation() {
        var mockA = mock(Annotation.class);
        when(mockAnnot.stream()
                .map(any())
                .filter(argThat(arg -> arg.test(mockAmd)))
                .map(argThat(arg -> {arg.apply(mockAmd); return true;}))
                .findAny()).thenReturn(Optional.of(mockA));
        when(mockAmd.isAnnotation(any())).thenReturn(true);
        when(metadataBase.getInternal()).thenReturn(mockAnnot);
        assertEquals(mockA, metadataBase.getAnnotation(Annotation.class));
        verify(mockAmd).isAnnotation(Annotation.class);
    }
}