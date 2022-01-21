package net.uptheinter.interceptify.internal;

import net.bytebuddy.dynamic.DynamicType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpellCheckingInspection")
@ExtendWith(MockitoExtension.class)
class TestInterceptPair {
    @Mock DynamicType.Unloaded<?> dtu;
    @Mock List<DynamicType.Unloaded<?>> ldtu;

    @Test
    void getInterceptee() {
        assertEquals(dtu, new InterceptPair(dtu, ldtu).getInterceptee());
    }

    @Test
    void getInterceptors() {
        assertEquals(ldtu, new InterceptPair(dtu, ldtu).getInterceptors());
    }
}