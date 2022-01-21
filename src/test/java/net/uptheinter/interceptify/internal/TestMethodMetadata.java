package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.uptheinter.interceptify.internal.MethodMetadata;
import net.uptheinter.interceptify.internal.ParameterMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestMethodMetadata {
    @Mock(answer = RETURNS_DEEP_STUBS) MethodDescription.InDefinedShape mockMethod;
    @Mock Stream<Object> mockStrm;
    MethodMetadata methodMetadata;

    @BeforeEach
    void Init() {
        methodMetadata = new MethodMetadata(mockMethod);
    }

    @Test
    void isConstructor() {
        when(mockMethod.isConstructor()).thenReturn(true, false);
        assertTrue(methodMetadata.isConstructor());
        assertFalse(methodMetadata.isConstructor());
    }

    @Test
    void isStatic() {
        when(mockMethod.isStatic()).thenReturn(true, false);
        assertTrue(methodMetadata.isStatic());
        assertFalse(methodMetadata.isStatic());
    }

    @Test
    void isInstanceMethod() {
        when(mockMethod.isStatic()).thenReturn(true, false);
        assertFalse(methodMetadata.isInstanceMethod());
        assertTrue(methodMetadata.isInstanceMethod());
    }

    @Test
    void getShape() {
        assertEquals(mockMethod, methodMetadata.getShape());
    }

    @Test
    void getParameters() {
        when(mockMethod.getParameters().stream().map(any())).thenReturn(mockStrm);
        assertEquals(mockStrm, methodMetadata.getParameters());
    }

    @Test
    void getParameterList() {
        var obj = new ArrayList<ParameterMetadata>();
        when(mockMethod.getParameters().stream().map(any()).collect(any()))
                .thenReturn(obj);
        assertEquals(obj, methodMetadata.getParameterList());
    }

    @Test
    void getName() {
        var str = "testName";
        when(mockMethod.getName()).thenReturn(str);
        assertEquals(str, mockMethod.getName());
    }

    @Test
    void getDeclaringType() {
        var td = mock(TypeDescription.class);
        when(mockMethod.getDeclaringType()).thenReturn(td);
        assertEquals(td, methodMetadata.getDeclaringType());
    }

    @Test
    void getInternal() {
        var al = mock(AnnotationList.class);
        when(mockMethod.getDeclaredAnnotations()).thenReturn(al);
        assertEquals(al, methodMetadata.getInternal());
    }
}