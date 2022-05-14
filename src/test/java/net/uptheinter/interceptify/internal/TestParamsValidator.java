package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("SpellCheckingInspection")
class TestParamsValidator {

    @SuppressWarnings({"EmptyMethod", "unused", "SpellCheckingInspection"})
    static class TestMe {
        static void Good1(TestMe a, Method b, int c, String d) {}
        void Chek1(int c, String d) {}

        static void Good2(Method b, int c, String d) {}
        static void Chek2(int c, String d) {}

        static void Good3(int c, char d) {}
        static void Chek3(int c, char d) {}

        void Bad1(TestMe a, Method b, int c) {}
        void Chk1(int c) {}

        void Bad2(Method a, int c) {}
        void Chk2(int c) {}

        static void Bad3(int a, int b, int c) {}
        static void Chk3(int a, int b, int c, int d, int e, int f) {}

        static void Bad4(int a, String b) {}
        static void Chk4(int a, int b) {}

        public TestMe(String a) {}
        public static void Constr(TestMe a, String b) {}
        public static void BadConstr(String a) {}
    }

    private static MethodMetadata getMethod(TypeDescription cls, String name) {
        return cls.getDeclaredMethods()
                .stream()
                .filter(m -> m.getName().equals(name))
                .map(MethodMetadata::new)
                .findAny().orElseThrow();
    }

    private static boolean paramsMatch(MethodDescription m, Type[] params) {
        var p = m.getParameters();
        for (var i = 0; i < p.size(); ++i)
            if (!p.get(i).getType().represents(params[i]))
                return false;
        return true;
    }

    private static MethodMetadata getConstructor(TypeDescription cls, Type... params) {
        return cls.getDeclaredMethods()
                .stream()
                .filter(MethodDescription::isConstructor)
                .filter(m -> m.getParameters().size() == params.length)
                .filter(m -> paramsMatch(m, params))
                .map(MethodMetadata::new)
                .findAny().orElseThrow();
    }

    @Test
    void testEquals() {
        var cls = TypePool.Default.ofSystemLoader().describe(TestMe.class.getName()).resolve();
        var cm = new ClassMetadata(cls);
        assertTrue(new ParamsValidator(getMethod(cls, "Good1"), cm).isCompatible(getMethod(cls, "Chek1")));
        assertTrue(new ParamsValidator(getMethod(cls, "Good2"), cm).isCompatible(getMethod(cls, "Chek2")));
        assertTrue(new ParamsValidator(getMethod(cls, "Good3"), cm).isCompatible(getMethod(cls, "Chek3")));

        assertFalse(new ParamsValidator(getMethod(cls, "Bad1"), cm).isCompatible(getMethod(cls, "Chk1")));
        assertFalse(new ParamsValidator(getMethod(cls, "Bad2"), cm).isCompatible(getMethod(cls, "Chk2")));
        assertFalse(new ParamsValidator(getMethod(cls, "Bad3"), cm).isCompatible(getMethod(cls, "Chk3")));
        assertFalse(new ParamsValidator(getMethod(cls, "Bad4"), cm).isCompatible(getMethod(cls, "Chk4")));

        assertTrue(new ParamsValidator(getMethod(cls, "Constr"), cm).isCompatible(getConstructor(cls, String.class)));
        assertFalse(new ParamsValidator(getMethod(cls, "BadConstr"), cm).isCompatible(getConstructor(cls, String.class)));
    }
}
