package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.implementation.MethodCall;
import net.uptheinter.interceptify.annotations.InterceptClass;
import net.uptheinter.interceptify.annotations.OverwriteConstructor;
import net.uptheinter.interceptify.annotations.OverwriteMethod;
import net.uptheinter.interceptify.util.JarFileEx;
import net.uptheinter.interceptify.util.JarFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static net.bytebuddy.description.annotation.AnnotationDescription.Builder.ofType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestClassInjector {
    @Mock ByteBuddy mockBb;
    @Mock Instrumentation mockInstr;
    @Mock JarFiles mockJf;
    @Mock(answer = RETURNS_DEEP_STUBS) Stream<JarFileEx> mockStrm;
    @Mock Stream<Object> mockStrm2;
    final URL[] mockUrl = new URL[0];
    final ByteBuddy bb = new ByteBuddy();
    ClassInjector classInjector;
    Path tmp;

    @BeforeEach
    void Init() throws IOException {
        classInjector = new ClassInjector(mockBb, mockInstr).setClassPath(mockUrl);
        tmp = Files.createTempDirectory("tmp");
        tmp.toFile().deleteOnExit();
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectMetadataFrom() throws Throwable {
        var mockCm = mock(ClassMetadata.class);
        when(mockJf.stream()).thenReturn(mockStrm);
        when(mockStrm.map(any())
                .flatMap(any())
                .map(any())
                .map(any())
                .filter(any())).thenReturn(mockStrm2);
        doNothing().when(mockStrm2).forEach(argThat(arg -> {arg.accept(mockCm); return true;}));
        assertEquals(classInjector, classInjector.collectMetadataFrom(mockJf));
        verify(mockStrm2).forEach(any());
        var field = ClassInjector.class.getDeclaredField("classes");
        field.setAccessible(true);
        var list = (List<ClassMetadata>)field.get(classInjector);
        assertEquals(1, list.size());
        assertEquals(mockCm, list.get(0));
    }

    @SuppressWarnings("unused")
    public static int interceptee(int a) {
        return a;
    }

    @SuppressWarnings("unused")
    public static int interceptor(Object inst, Method m, int a) throws Throwable {
        return (int)m.invoke(inst, 5) + a;
    }

    private static boolean success;
    @SuppressWarnings("unused")
    public static void constructIntercept(Object inst, int a) {
        success = true;
    }

    @SuppressWarnings("unused")
    public static void construct(int a) {
        success = false;
    }

    private Method to(String name, Class<?>... args) throws NoSuchMethodException {
        return getClass().getDeclaredMethod(name, args);
    }

    @Test
    void applyAnnotationsAndIntercept() throws Throwable {
        var jarFile = tmp.resolve("test.jar").toFile();
        var target = bb.subclass(Object.class)
                .name("foo.Interceptee")
                .modifiers(Modifier.PUBLIC)
                .defineMethod("func", int.class, Modifier.PUBLIC)
                .withParameter(int.class)
                .intercept(MethodCall.invoke(
                        to("interceptee", int.class)).withAllArguments())
                .defineConstructor(Modifier.PUBLIC)
                .withParameter(int.class)
                .intercept(MethodCall.invoke(to("construct", int.class)).withAllArguments().andThen(MethodCall.invoke(Object.class.getConstructor())))
                .make();
        var interceptee = target.getBytes();
        var interceptor = bb.subclass(Object.class)
                .name("foo.Interceptor")
                .modifiers(Modifier.PUBLIC)

                .defineMethod("intercept", int.class, Modifier.PUBLIC | Modifier.STATIC)
                .withParameter(target.getTypeDescription()).withParameter(Method.class).withParameter(int.class)
                .intercept(MethodCall.invoke(
                        to("interceptor", Object.class, Method.class, int.class)).withAllArguments()
                ).annotateMethod(ofType(OverwriteMethod.class).define("value", "func").build())

                .defineMethod("construct", void.class, Modifier.PUBLIC | Modifier.STATIC)
                .withParameter(target.getTypeDescription()).withParameter(int.class)
                .intercept(MethodCall.invoke(
                        to("constructIntercept", Object.class, int.class)).withAllArguments()
                ).annotateMethod(ofType(OverwriteConstructor.class).build())

                .annotateType(ofType(InterceptClass.class).define("value", "foo.Interceptee").build())
                .make().getBytes();

        try (var f = new FileOutputStream(jarFile);
            var jar = new ZipOutputStream(f)) {
            jar.putNextEntry(new ZipEntry("foo/Interceptor.class"));
            jar.write(interceptor);
            jar.putNextEntry(new ZipEntry("foo/Interceptee.class"));
            jar.write(interceptee);
        }

        classInjector = new ClassInjector(bb, mockInstr)
                .setClassPath(new URL[]{jarFile.toURI().toURL()});
        var jf = new JarFiles();
        jf.addFromDirectory(tmp);
        classInjector.collectMetadataFrom(jf).applyAnnotationsAndIntercept();
        var cls = Class.forName("foo.Interceptee");
        var inst = cls.getDeclaredConstructor(int.class).newInstance(5);
        var result = (int)cls.getDeclaredMethod("func", int.class)
                .invoke(inst, 3);
        assertEquals(8, result);
        assertTrue(success);
    }

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
        var arr = new ClassInjector(bb, mockInstr)
                .setClassPath(mockUrl)
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
        var arr = new ClassInjector(bb, mockInstr)
                .setClassPath(mockUrl)
                .defineMakePublicList(new HashSet<>() {{
                    add(name);
                }})
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
        var arr = new ClassInjector(bb, mockInstr)
                .setClassPath(mockUrl)
                .defineMakePublicList(new HashSet<>() {{
                    add(name);
                }})
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
        assertEquals(Modifier.PUBLIC, modifiers);
        modifiers = madePublic.getDeclaredMethod("a").getModifiers();
        assertEquals(Modifier.PUBLIC, modifiers);
    }
}