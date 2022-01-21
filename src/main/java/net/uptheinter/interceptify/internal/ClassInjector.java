package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.uptheinter.interceptify.annotations.InterceptClass;
import net.uptheinter.interceptify.annotations.OverwriteConstructor;
import net.uptheinter.interceptify.annotations.OverwriteMethod;
import net.uptheinter.interceptify.util.Boxed;
import net.uptheinter.interceptify.util.JarEntryEx;
import net.uptheinter.interceptify.util.JarFileEx;
import net.uptheinter.interceptify.util.JarFiles;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesGenericArguments;

class ClassInjector {
    private static final ClassLoader classLoader = ClassInjector.class.getClassLoader();

    private final ByteBuddy byteBuddy;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final Instrumentation instr;
    private final ClassByteCodeLocator byteCodeLocator;
    private final ClassFileLocator classFileLocator;
    private final GranularTypePool typePool;
    private final List<ClassMetadata> classes = new ArrayList<>();

    public ClassInjector(ByteBuddy byteBuddy, Instrumentation instr, URL[] classPath) {
        this.byteBuddy = byteBuddy;
        this.instr = instr;
        this.byteCodeLocator = new ClassByteCodeLocator();
        this.classFileLocator = new ClassFileLocator.Compound(
                byteCodeLocator,
                new ClassFileLocator.ForUrl(classPath),
                ClassFileLocator.ForClassLoader.ofSystemLoader()
        );
        typePool = GranularTypePool.of(classFileLocator);
    }

    public ClassInjector collectMetadataFrom(JarFiles jarFiles) {
        jarFiles.stream()
                .map(JarFileEx::getClasses)
                .flatMap(Collection::stream)
                .map(JarEntryEx::getClassName)
                .map(this::getClassMetadata)
                .filter(a -> a.hasAnnotation(InterceptClass.class))
                .forEach(classes::add);
        return this;
    }

    private ClassMetadata getClassMetadata(String classFile) {
        return new ClassMetadata(classFile, typePool);
    }

    public void applyAnnotationsAndIntercept() {
        var interceptMap = classes.stream()
                .collect(HashMap<String, List<ClassMetadata>>::new,
                        (map, cls) -> map
                                .computeIfAbsent(cls.getAnnotation(InterceptClass.class).value(),
                                        k -> new ArrayList<>())
                                .add(cls), (a, b) -> {});
        interceptMap.entrySet()
                .stream()
                .map(this::applyInterceptTo)
                .flatMap(this::loadInterceptee) // All interceptees are now loaded.
                .forEach(this::loadInterceptor);
    }

    private InterceptPair applyInterceptTo(Map.Entry<String, List<ClassMetadata>> entry) {
        var target = entry.getKey();
        var interceptors = entry.getValue();
        var targetedCls = new ClassMetadata(target, typePool);
        var intercepteeBuildBox = new Boxed<Builder<?>>(byteBuddy.rebase(targetedCls.getTypeDesc(), classFileLocator));

        List<Unloaded<?>> unloadedInterceptors = interceptors.stream()
                .map(cls -> annotateMethodsFor(cls, targetedCls))
                .map(this::refreshDefinitions)
                .map(interceptor -> prepareMethodInterception(interceptor, intercepteeBuildBox))
                .map(interceptor -> prepareConstructorInterception(interceptor, intercepteeBuildBox))
                .collect(Collectors.toList());
        var unloadedInterceptee = intercepteeBuildBox
                .get()
                .make(new TypeResolutionStrategy.Active());
        return new InterceptPair(unloadedInterceptee, unloadedInterceptors);
    }

    private Unloaded<?> annotateMethodsFor(ClassMetadata cls, ClassMetadata targetedCls) {
        var buildBox = new Boxed<Builder<?>>(byteBuddy.redefine(cls.getTypeDesc(), classFileLocator));
        cls.getMethodsWithAnnotation(OverwriteMethod.class, OverwriteConstructor.class)
                .forEach(method -> tryAnnotateParamsFor(cls, method, buildBox, targetedCls));
        return buildBox
                .get()
                .make(new TypeResolutionStrategy.Active(), typePool);
    }

    private void tryAnnotateParamsFor(ClassMetadata cls,
                                      MethodMetadata method,
                                      Boxed<Builder<?>> builder,
                                      ClassMetadata targetedCls) {
        var paramsValidator = new ParamsValidator(method, targetedCls);
        var targetMethod = targetedCls.getMethods()
                .filter(candidate -> (isConstructorMatch(candidate, method) || isMethodMatch(candidate, method)) &&
                        paramsValidator.isCompatible(candidate))
                .findAny()
                .orElse(null);
        if (targetMethod == null) {
            System.err.println("ERROR: " + cls.getTypeName() + "::" + method.getName() +
                    " does not match the signature of any targeted functions - Skipped");
        } else {
            new ParamsAnnotator(builder, method, targetMethod)
                    .annotate();
        }
    }

    private static boolean isMethodMatch(MethodMetadata candidate, MethodMetadata interceptor) {
        var val = interceptor.getAnnotation(OverwriteMethod.class);
        return val != null && val.value().equals(candidate.getName());
    }

    private static boolean isConstructorMatch(MethodMetadata candidate, MethodMetadata interceptor) {
        return interceptor.hasAnnotation(OverwriteConstructor.class) &&
                candidate.isConstructor();
    }

    private Unloaded<?> refreshDefinitions(Unloaded<?> staleClass) {
        var typeName = staleClass.getTypeDescription().getTypeName();
        byteCodeLocator.put(typeName, staleClass.getBytes());
        typePool.removeFromCache(typeName);
        var type = typePool.describe(typeName).resolve();
        return byteBuddy.redefine(type,
                classFileLocator).make(new TypeResolutionStrategy.Active(), typePool);
    }

    private Unloaded<?> prepareMethodInterception(Unloaded<?> interceptor, Boxed<Builder<?>> buildBox) {
        var metadata = new ClassMetadata(interceptor.getTypeDescription());
        var methodIntercepts = metadata.getMethodsWithAnnotation(OverwriteMethod.class)
                .map(method -> method.getAnnotation(OverwriteMethod.class))
                .map(OverwriteMethod::value)
                .distinct()
                .toArray(String[]::new);
        buildBox.run(builder -> builder
                .method(namedOneOf(methodIntercepts))
                .intercept(to(interceptor.getTypeDescription())));
        return interceptor;
    }

    private Unloaded<?> prepareConstructorInterception(Unloaded<?> interceptor, Boxed<Builder<?>> buildBox) {
        new ClassMetadata(interceptor.getTypeDescription())
                .getMethodsWithAnnotation(OverwriteConstructor.class)
                .forEach(constructor -> doConstructorInterception(constructor, interceptor, buildBox));
        return interceptor;
    }

    private void doConstructorInterception(MethodMetadata constructor,
                                           Unloaded<?> interceptor,
                                           Boxed<Builder<?>> buildBox) {
        var params = constructor.getParameters()
                .filter(p -> p.hasAnnotation(Argument.class))
                .map(ParameterMetadata::getType)
                .collect(Collectors.toList());
        buildBox.run(builder -> builder.constructor(takesGenericArguments(params))
                .intercept(computeCallSemantics(constructor, interceptor)));
    }

    private Implementation computeCallSemantics(MethodMetadata constructor, Unloaded<?> interceptor) {
        var semantics = constructor.getAnnotation(OverwriteConstructor.class);
        var desc = interceptor.getTypeDescription();
        if (semantics.after() && !semantics.before())
            return SuperMethodCall.INSTANCE.andThen(to(desc));
        else if (semantics.before() && !semantics.after())
            return to(desc).andThen(SuperMethodCall.INSTANCE);
        else if (semantics.before() /* && semantics.after() */) // tautologically true, so it's just a comment.
            return to(desc).andThen(SuperMethodCall.INSTANCE).andThen(to(desc));
        throw new RuntimeException("ERROR: " + desc.getName() +
                " has OverwriteConstructor with neither before or after == true");
    }

    private Stream<Unloaded<?>> loadInterceptee(InterceptPair pair) {
        pair.getInterceptee().load(classLoader, ClassLoadingStrategy.Default.INJECTION);
        return pair.getInterceptors().stream();
    }

    private void loadInterceptor(Unloaded<?> interceptor) {
        interceptor.load(classLoader, ClassLoadingStrategy.Default.INJECTION);
    }
}