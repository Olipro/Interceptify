package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.pool.TypePool;
import net.uptheinter.interceptify.util.Boxed;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestParamsAnnotator {

    @SuppressWarnings("unused")
    static class ToAnnotate {
        @SuppressWarnings("EmptyMethod")
        public static void interceptor(ToAnnotate a, Method b, int c, String d) {}
        @SuppressWarnings("EmptyMethod")
        public void interceptee(int a, String b) {}
    }

    @Test
    void annotate() {
        var bb = new ByteBuddy();
        var box = new Boxed<DynamicType.Builder<?>>(bb.redefine(ToAnnotate.class));
        var toAnnotate = TypePool.Default.ofSystemLoader().describe(ToAnnotate.class.getName()).resolve();
        var interceptor = toAnnotate.getDeclaredMethods()
                .stream()
                .filter(m -> "interceptor".equals(m.getName()))
                .map(MethodMetadata::new)
                .findAny().orElseThrow();
        var interceptee = toAnnotate.getDeclaredMethods()
                .stream()
                .filter(m -> "interceptee".equals(m.getName()))
                .map(MethodMetadata::new)
                .findAny().orElseThrow();
        new ParamsAnnotator(box, interceptor, interceptee).annotate();
        var cfl = new ClassFileLocator.Compound(
                ClassFileLocator.Simple.of(ToAnnotate.class.getName(), box.get().make().getBytes()),
                ClassFileLocator.ForClassLoader.ofSystemLoader());
        var cls = TypePool.Default.of(cfl).describe(ToAnnotate.class.getName()).resolve();
        var p = cls.getDeclaredMethods()
                .stream()
                .filter(i -> "interceptor".equals(i.getName()))
                .findAny().orElseThrow().getParameters();
        assertEquals(4, p.size());
        assertTrue(p.get(0).getDeclaredAnnotations().get(0).getAnnotationType().represents(RuntimeType.class));
        assertTrue(p.get(0).getDeclaredAnnotations().get(1).getAnnotationType().represents(This.class));
        assertTrue(p.get(1).getDeclaredAnnotations().getOnly().getAnnotationType().represents(SuperMethod.class));
        for (int i = 0; i < 2; ++i) {
            var arg1 = p.get(i + 2).getDeclaredAnnotations().getOnly();
            assertTrue(arg1.getAnnotationType().represents(Argument.class));
            assertEquals(i, (int) arg1.getValue("value").resolve());
        }

    }
}