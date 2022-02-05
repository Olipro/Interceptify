package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.ModifierAdjustment;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.EnumerationState;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.FieldPersistence;
import net.bytebuddy.description.modifier.MethodArguments;
import net.bytebuddy.description.modifier.MethodManifestation;
import net.bytebuddy.description.modifier.MethodStrictness;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.SynchronizationState;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesGenericArguments;

class ClassInjector implements ClassFileTransformer {
    private static final ClassLoader classLoader = ClassInjector.class.getClassLoader();

    private final ByteBuddy byteBuddy;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final Instrumentation instr;
    private final ClassByteCodeLocator byteCodeLocator = new ClassByteCodeLocator();
    private final TemporaryByteCodeLocator tempCodeLocator = new TemporaryByteCodeLocator();
    private final List<ClassMetadata> classes = new ArrayList<>();
    private Set<String> toMakePublic;
    private ClassFileLocator classFileLocator;
    private GranularTypePool typePool;

    public ClassInjector(ByteBuddy byteBuddy, Instrumentation instr) {
        this.byteBuddy = byteBuddy;
        this.instr = instr;
        instr.addTransformer(this);
    }

    public ClassInjector setClassPath(URL[] classPath) {
        this.classFileLocator = new ClassFileLocator.Compound(
                tempCodeLocator,
                byteCodeLocator,
                new ClassFileLocator.ForUrl(classPath),
                ClassFileLocator.ForClassLoader.ofSystemLoader()
        );
        this.typePool = GranularTypePool.of(classFileLocator);
        return this;
    }

    public ClassInjector defineMakePublicList(Set<String> list) {
        this.toMakePublic = list;
        return this;
    }

    public ClassInjector collectMetadataFrom(JarFiles jarFiles) {
        assert(classFileLocator != null);
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

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!toMakePublic.contains(className))
            return classfileBuffer;
        return tempCodeLocator.with(className, classfileBuffer, () -> makeAllPublic(className));
    }

    private void applyModifiersFor(TypeDescription type, List<ModifierContributor> list) {
        list.add(TypeManifestation.PLAIN);
        if (type.isAnnotation())
            list.add(TypeManifestation.ANNOTATION);
        else if (type.isInterface())
            list.add(TypeManifestation.INTERFACE);
        else if (type.isAbstract())
            list.add(TypeManifestation.ABSTRACT);
        if (type.isEnum())
            list.add(EnumerationState.ENUMERATION);
        if (type.isStatic())
            list.add(Ownership.STATIC);
    }

    private void applyModifiersFor(MethodDescription method, List<ModifierContributor> list) {
        list.add(MethodManifestation.PLAIN);
        if (method.isVarArgs())
            list.add(MethodArguments.VARARGS);
        if (method.isStrict())
            list.add(MethodStrictness.STRICT);
        if (method.isAbstract())
            list.add(MethodManifestation.ABSTRACT);
        if (method.isNative())
            list.add(MethodManifestation.NATIVE);
        if (method.isBridge())
            list.add(MethodManifestation.BRIDGE);
        if (method.isSynchronized())
            list.add(SynchronizationState.SYNCHRONIZED);
        if (method.isStatic())
            list.add(Ownership.STATIC);
    }

    private void applyModifiersFor(FieldDescription field, TypeDescription cls, List<ModifierContributor> list) {
        list.add(FieldManifestation.PLAIN);
        if (field.isVolatile())
            list.add(FieldManifestation.VOLATILE);
        if (field.isSynthetic())
            list.add(SyntheticState.SYNTHETIC);
        if (field.isTransient())
            list.add(FieldPersistence.TRANSIENT);
        if (field.isStatic())
            list.add(Ownership.STATIC);
        if (cls.isInterface())
            list.add(FieldManifestation.FINAL);
    }

    private <R extends ModifierContributor> List<R> getManifestation(FieldDescription obj, TypeDescription cls) {
        var ret = new ArrayList<ModifierContributor>(6);
        ret.add(Visibility.PUBLIC);
        applyModifiersFor(obj, cls, ret);
        //noinspection unchecked
        return (List<R>) ret;
    }

    private <R extends ModifierContributor, T> List<R> getManifestation(T obj) {
        var ret = new ArrayList<ModifierContributor>(6);
        ret.add(Visibility.PUBLIC);
        if (obj instanceof TypeDescription) {
            applyModifiersFor((TypeDescription) obj, ret);
        } else if (obj instanceof MethodDescription) {
            applyModifiersFor((MethodDescription) obj, ret);
        }
        //noinspection unchecked
        return (List<R>) ret;
    }

    private byte[] makeAllPublic(String className) {
        var cls = typePool.describe(className).resolve();
        var adjust = new Boxed<>(new ModifierAdjustment());
        adjust.run(builder -> builder.withTypeModifiers(getManifestation(cls)));
        cls.getDeclaredMethods().stream()
                .filter(method -> !method.isPublic() || method.isFinal())
                .forEach(method -> adjust.run(builder -> builder.withMethodModifiers(is(method), getManifestation(method))));
        cls.getDeclaredFields().stream()
                .filter(field -> !field.isPublic() || field.isFinal())
                .forEach(field -> adjust.run(builder -> builder.withFieldModifiers(is(field), getManifestation(field, cls))));
        return byteBuddy.redefine(cls, classFileLocator)
                .visit(adjust.get())
                .make().getBytes();
    }

    @Override
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }
}