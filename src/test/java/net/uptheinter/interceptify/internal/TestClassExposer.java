package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.EnumerationState;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class TestClassExposer {
    final ClassByteCodeLocator dummyLocator = new ClassByteCodeLocator();
    final ClassFileLocator locator = new ClassFileLocator.Compound(
            dummyLocator,
            ClassFileLocator.ForClassLoader.of(getClass().getClassLoader())
    );
    final GranularTypePool typePool = GranularTypePool.of(locator);
    final ByteBuddy bb = new ByteBuddy();

    @SuppressWarnings({"SameReturnValue", "unused", "EmptyMethod"})
    private abstract static class toMakePublic {
        public static final String x = "";
        private transient volatile char y;
        protected final int z = 0;

        private static synchronized strictfp boolean a(int x, String y) {
            return true;
        }

        public native final int b(char x, int... y);

        protected abstract void c();

        protected final void d() {}
    }

    @Test
    void testClassIsMadePublic() throws Exception {
        String name = "bar.MakePublic";
        var cls = bb.redefine(toMakePublic.class)
                .name(name)
                .modifiers(Ownership.MEMBER, TypeManifestation.ABSTRACT, Visibility.PRIVATE)
                .noNestMate()
                .make()
                .getBytes();
        var arr = new ClassExposer(bb, () -> typePool, () -> dummyLocator, () -> locator)
                .defineMakePublicList(new HashSet<>() {{
                    add(name);
                }})
                .transform(null,
                        ClassLoader.getSystemClassLoader(),
                        name,
                        null,
                        null, cls);
        var madePublic = new ByteArrayClassLoader(ClassLoader.getSystemClassLoader(),
                new HashMap<>(){{put(name, arr);}})
                .loadClass(name);
        var modifiers = madePublic.getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.ABSTRACT, modifiers);
        modifiers = madePublic.getDeclaredMethod("a", int.class, String.class).getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.SYNCHRONIZED | Modifier.STRICT, modifiers);
        modifiers = madePublic.getDeclaredMethod("b", char.class, int[].class).getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.NATIVE | Modifier.TRANSIENT, modifiers);
        modifiers = madePublic.getDeclaredMethod("c").getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.ABSTRACT, modifiers);
        modifiers = madePublic.getDeclaredMethod("d").getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
        modifiers = madePublic.getDeclaredField("x").getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.STATIC, modifiers);
        modifiers = madePublic.getDeclaredField("y").getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.VOLATILE | Modifier.TRANSIENT, modifiers);
        modifiers = madePublic.getDeclaredField("z").getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
    }

    @SuppressWarnings({"unused", "SameReturnValue"})
    private interface toMakePublic2 {
        String s = "";
        default int x() {
            return 0;
        }
    }

    @Test
    void testInterfaceIsMadePublic() throws Exception {
        String name = "bar.MakePublic2";
        var cls = bb.redefine(toMakePublic2.class)
                .name(name)
                .modifiers(Ownership.MEMBER, TypeManifestation.INTERFACE, Visibility.PRIVATE)
                .noNestMate()
                .make()
                .getBytes();
        var arr = new ClassExposer(bb, () -> typePool, () -> dummyLocator, () -> locator)
                .defineMakePublicPredicate(name::equals)
                .transform(null,
                        ClassLoader.getSystemClassLoader(),
                        name,
                        null,
                        null, cls);
        var madePublic = new ByteArrayClassLoader(ClassLoader.getSystemClassLoader(),
                new HashMap<>() {{
                    put(name, arr);
                }})
                .loadClass(name);
        var modifiers = madePublic.getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.INTERFACE | Modifier.ABSTRACT, modifiers);
        modifiers = madePublic.getDeclaredMethod("x").getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
    }

    @SuppressWarnings({"unused", "EmptyMethod"})
    private static final class toMakePublic3 {
        void a() {}
    }

    @Test
    void testFinalClassIsMadePublic() throws Exception {
        String name = "bar.MakePublic3";
        var cls = bb.redefine(toMakePublic3.class)
                .name(name)
                .modifiers(Ownership.MEMBER, TypeManifestation.FINAL, Visibility.PRIVATE)
                .noNestMate()
                .make()
                .getBytes();
        var arr = new ClassExposer(bb, () -> typePool, () -> dummyLocator, () -> locator)
                .defineMakePublicList(new HashSet<>() {{
                    add(name);
                }})
                .transform(null,
                        getClass().getClassLoader(),
                        name,
                        null,
                        null, cls);
        var madePublic = new ByteArrayClassLoader(ClassLoader.getSystemClassLoader(),
                new HashMap<>() {{
                    put(name, arr);
                }})
                .loadClass(name);
        var modifiers = madePublic.getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
        modifiers = madePublic.getDeclaredMethod("a").getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
    }

    @SuppressWarnings({"unused", "SameReturnValue", "EmptyMethod"})
    private enum toMakePublic4 {
        A,
        B;

        public final String s = "";
        public final void x() {}
        private void y() {}
    }

    @Test
    void testEnumIsMadePublic() throws Exception {
        String name = "bar.MakePublic4";
        var type = bb.redefine(toMakePublic4.class)
                .name(name)
                .modifiers(Ownership.MEMBER, EnumerationState.ENUMERATION, Visibility.PRIVATE)
                .noNestMate()
                .make();
        var cls = type.getBytes();
        var arr = new ClassExposer(bb, () -> typePool, () -> dummyLocator, () -> locator)
                .defineMakePublicPredicate(s -> true)
                .transform(null,
                        getClass().getClassLoader(),
                        type.getTypeDescription().getTypeName().replace('.', '/'),
                        null,
                        null, cls);
        var madePublic = new ByteArrayClassLoader(ClassLoader.getSystemClassLoader(),
                new HashMap<>() {{
                    put(name, arr);
                }})
                .loadClass(name);
        var modifiers = madePublic.getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers & 0xFFF); // ignore unexposed bits
        modifiers = madePublic.getDeclaredMethod("x").getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
        modifiers = madePublic.getDeclaredMethod("y").getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
        modifiers = madePublic.getDeclaredField("s").getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
        assertTrue(madePublic.isEnum());
    }

    @SuppressWarnings({"unused", "SameReturnValue"})
    private @interface toMakePublic5 {
        String s = "";
        int x() default 5;
    }

    @Test
    void testAnnotationIsMadePublic() throws Exception {
        String name = "bar.MakePublic5";
        var cls = bb.redefine(toMakePublic5.class)
                .name(name)
                .modifiers(Ownership.MEMBER, TypeManifestation.ANNOTATION, Visibility.PRIVATE)
                .noNestMate()
                .make()
                .getBytes();
        var arr = new ClassExposer(bb, () -> typePool, () -> dummyLocator, () -> locator)
                .defineMakePublicPredicate(name::equals)
                .transform(null,
                        ClassLoader.getSystemClassLoader(),
                        name,
                        null,
                        null, cls);
        var madePublic = new ByteArrayClassLoader(ClassLoader.getSystemClassLoader(),
                new HashMap<>() {{
                    put(name, arr);
                }})
                .loadClass(name);
        var modifiers = madePublic.getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.ABSTRACT | Modifier.INTERFACE, modifiers & 0xFFF); // ignore unexposed bits
        modifiers = madePublic.getDeclaredMethod("x").getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.ABSTRACT, modifiers);
        modifiers = madePublic.getDeclaredField("s").getModifiers();
        assertEquals(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, modifiers);
        assertTrue(madePublic.isAnnotation());
    }

    @Test
    void unwantedClassIsIgnored() {
        var bytes = new byte[1];
        var arr = new ClassExposer(bb, () -> typePool, () -> dummyLocator, () -> locator)
                .transform(null, ClassLoader.getSystemClassLoader(), "unwanted", null, null, bytes);
        assertSame(bytes, arr);
    }
}