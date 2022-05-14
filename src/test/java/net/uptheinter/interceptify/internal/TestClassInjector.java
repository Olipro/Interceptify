package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
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

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
    @Mock ClassExposer mockExposer;
    final URL[] mockUrl = new URL[0];
    final ByteBuddy bb = new ByteBuddy();
    ClassInjector classInjector;
    Field exposer;
    Path tmp;

    @BeforeEach
    void Init() throws Throwable {
        classInjector = new ClassInjector(mockBb, mockInstr).setClassPath(mockUrl);
        tmp = Files.createTempDirectory("tmp");
        tmp.toFile().deleteOnExit();
        exposer = ClassInjector.class.getDeclaredField("classExposer");
        exposer.setAccessible(true);
        exposer.set(classInjector, mockExposer);
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
    private static boolean success2 = true;
    @SuppressWarnings("unused")
    public static void constructIntercept(Object inst, int a) {
        success = true;
    }

    @SuppressWarnings("unused")
    public static void constructIntercept(String a) {
        success2 = false;
    }

    @SuppressWarnings("unused")
    public static void construct(int a) {
        success = false;
    }

    @SuppressWarnings("unused")
    public static void construct(String a) {
        success2 = !success2;
    }

    private Method to(String name, Class<?>... args) throws NoSuchMethodException {
        return getClass().getDeclaredMethod(name, args);
    }

    @Test
    void applyAnnotationsAndIntercept() throws Throwable {
        var jarFile = tmp.resolve("test.jar").toFile();
        var jarFile2 = tmp.resolve("test2.jar").toFile();
        var target = bb.subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name("foo.Interceptee")
                .modifiers(Modifier.PUBLIC)
                .defineMethod("func", int.class, Modifier.PUBLIC)
                .withParameter(int.class)
                .intercept(MethodCall.invoke(
                        to("interceptee", int.class)).withAllArguments())
                .defineConstructor(Modifier.PUBLIC)
                .withParameter(int.class)
                .intercept(MethodCall.invoke(to("construct", int.class)).withAllArguments().andThen(MethodCall.invoke(Object.class.getConstructor())))

                .defineConstructor(Modifier.PUBLIC)
                .withParameter(String.class)
                .intercept(MethodCall.invoke(to("construct", String.class)).withAllArguments().andThen(MethodCall.invoke(Object.class.getConstructor())))
                .make();
        target.toJar(jarFile);
        bb.subclass(Object.class)
                .name("foo.Interceptor")
                .modifiers(Modifier.PUBLIC)

                .defineMethod("intercept", int.class, Modifier.PUBLIC | Modifier.STATIC)
                .withParameter(target.getTypeDescription()).withParameter(Method.class).withParameter(int.class)
                .intercept(MethodCall.invoke(
                        to("interceptor", Object.class, Method.class, int.class)).withAllArguments())
                .annotateMethod(ofType(OverwriteMethod.class).define("value", "func").build())

                .defineMethod("construct", void.class, Modifier.PUBLIC | Modifier.STATIC)
                .withParameter(target.getTypeDescription()).withParameter(int.class)
                .intercept(MethodCall.invoke(
                        to("constructIntercept", Object.class, int.class)).withAllArguments())
                .annotateMethod(ofType(OverwriteConstructor.class).build())

                .defineMethod("construct", void.class, Modifier.PUBLIC | Modifier.STATIC)
                .withParameter(String.class)
                .intercept(MethodCall.invoke(to("constructIntercept", String.class)).withAllArguments())
                .annotateMethod(ofType(OverwriteConstructor.class).define("before", true).define("after", false).build())

                .annotateType(ofType(InterceptClass.class).define("value", "foo.Interceptee").build())
                .make().toJar(jarFile2);

        classInjector = new ClassInjector(bb, mockInstr)
                .setClassPath(new URL[]{jarFile.toURI().toURL(), jarFile2.toURI().toURL()});
        var jf = new JarFiles();
        jf.addFromDirectory(tmp);
        classInjector
                .collectMetadataFrom(jf)
                .applyAnnotationsAndIntercept();
        var cls = Class.forName("foo.Interceptee", true, ClassLoader.getSystemClassLoader());
        var inst = cls.getDeclaredConstructor(int.class).newInstance(5);
        var result = (int)cls.getDeclaredMethod("func", int.class)
                .invoke(inst, 3);
        cls.getDeclaredConstructor(String.class).newInstance("");
        assertEquals(8, result);
        assertTrue(success);
        assertTrue(success2);
    }

    @Test
    void defineMakePublicList() {
        var set = new HashSet<String>();
        classInjector.defineMakePublicList(set);
        verify(mockExposer).defineMakePublicList(set);
    }

    @Test
    void defineMakePublicPredicate() {
        Predicate<String> pred = s -> false;
        classInjector.defineMakePublicPredicate(pred);
        verify(mockExposer).defineMakePublicPredicate(pred);
    }
}