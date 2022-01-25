package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.uptheinter.interceptify.internal.ParameterMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestParameterMetadata {
    @Mock(answer = RETURNS_DEEP_STUBS) ParameterDescription.InDefinedShape mockParam;
    ParameterMetadata paramMeta;

    @BeforeEach
    void Init() {
        paramMeta = new ParameterMetadata(mockParam);
    }

    @Test
    void getType() {
        var gen = mock(TypeDescription.Generic.class);
        when(mockParam.getType()).thenReturn(gen);
        assertEquals(gen, paramMeta.getType());
    }

    @Test
    void getTypeName() {
        var str = "fakeString";
        when(mockParam.getType().getTypeName()).thenReturn(str);
        assertEquals(str, paramMeta.getTypeName());
    }

    @SuppressWarnings("EqualsWithItself")
    @Test
    void testEquals() {
        when(mockParam.getType().getTypeName())
                .thenReturn("first", "first", "second", "secondWrong");
        //noinspection ConstantConditions
        assertTrue(paramMeta.equals((Object)paramMeta));
        assertFalse(paramMeta.equals(paramMeta));
        //noinspection SimplifiableAssertion
        assertFalse(paramMeta.equals(new Object()));
    }

    @Test
    void testHashCode() {
        when(mockParam.getType().getTypeName())
                .thenReturn("staticString");
        assertEquals(paramMeta.hashCode(), "staticString".hashCode());
    }

    @Test
    void getInternal() {
        var al = mock(AnnotationList.class);
        when(mockParam.getDeclaredAnnotations()).thenReturn(al);
        assertEquals(al, paramMeta.getInternal());
    }
}