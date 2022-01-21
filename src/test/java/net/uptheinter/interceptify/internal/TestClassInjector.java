package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
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
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
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
    ClassInjector classInjector;

    @BeforeEach
    void Init() {
        classInjector = new ClassInjector(mockBb, mockInstr, mockUrl);
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
        var tmp = Files.createTempDirectory("tmp");
        var jarFile = tmp.resolve("test.jar").toFile();
        var bb = new ByteBuddy();
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
        var ci = new ClassInjector(bb, null, new URL[]{jarFile.toURI().toURL()});
        var jf = new JarFiles();
        jf.addFromDirectory(tmp);
        ci.collectMetadataFrom(jf).applyAnnotationsAndIntercept();
        var cls = Class.forName("foo.Interceptee");
        var inst = cls.getDeclaredConstructor(int.class).newInstance(5);
        var result = (int)cls.getDeclaredMethod("func", int.class)
                .invoke(inst, 3);
        assertEquals(8, result);
        assertTrue(success);
    }
}