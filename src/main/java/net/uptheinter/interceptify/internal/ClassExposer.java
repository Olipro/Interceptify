package net.uptheinter.interceptify.internal;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.ModifierAdjustment;
import net.bytebuddy.asm.TypeReferenceAdjustment;
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
import net.bytebuddy.dynamic.DynamicType;
import net.uptheinter.interceptify.util.Boxed;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static net.bytebuddy.matcher.ElementMatchers.is;

class ClassExposer implements ClassFileTransformer {
    private final ByteBuddy byteBuddy;
    private final Supplier<GranularTypePool> typePoolSupplier;
    private final Supplier<ClassByteCodeLocator> locatorSupplier;
    private final Supplier<ClassFileLocator> compoundSupplier;
    private Predicate<String> shouldMakePublic = s -> false;
    private Set<String> toMakePublic = new HashSet<>();

    public ClassExposer(ByteBuddy byteBuddy,
                        Supplier<GranularTypePool> typePoolSupplier,
                        Supplier<ClassByteCodeLocator> locatorSupplier,
                        Supplier<ClassFileLocator> compoundLocator) {
        this.byteBuddy = byteBuddy;
        this.typePoolSupplier = typePoolSupplier;
        this.locatorSupplier = locatorSupplier;
        this.compoundSupplier = compoundLocator;
    }

    public ClassExposer defineMakePublicList(Set<String> toMakePublic) {
        this.toMakePublic = toMakePublic;
        return this;
    }

    public ClassExposer defineMakePublicPredicate(Predicate<String> shouldMakePublic) {
        this.shouldMakePublic = shouldMakePublic;
        return this;
    }

    private void updateClass(String name, byte[] bytes) {
        typePoolSupplier.get().removeFromCache(name);
        locatorSupplier.get().put(name, bytes);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        className = className.replace('/', '.');
        if (!toMakePublic.contains(className) && !shouldMakePublic.test(className))
            return classfileBuffer;
        updateClass(className, classfileBuffer);
        var cls = typePoolSupplier.get().describe(className).resolve();
        if (cls.isEnum()) // workaround for the enum visitor getting the raw name.
            updateClass("L" + className + ";", classfileBuffer);
        var transformed = makeAllPublic(cls);
        updateClass(className, transformed);
        return transformed;
    }

    @Override
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
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

    @SuppressWarnings("unchecked")
    private <R extends ModifierContributor> List<R> getManifestation(FieldDescription obj, TypeDescription cls) {
        var ret = new ArrayList<ModifierContributor>(6);
        ret.add(Visibility.PUBLIC);
        applyModifiersFor(obj, cls, ret);
        return (List<R>) ret;
    }

    @SuppressWarnings("unchecked")
    private <R extends ModifierContributor, T> List<R> getManifestation(T obj) {
        var ret = new ArrayList<ModifierContributor>(6);
        ret.add(Visibility.PUBLIC);
        if (obj instanceof TypeDescription) {
            applyModifiersFor((TypeDescription) obj, ret);
        } else if (obj instanceof MethodDescription) {
            applyModifiersFor((MethodDescription) obj, ret);
        }
        return (List<R>) ret;
    }

    private byte[] makeAllPublic(TypeDescription cls) {
        return makeTypeDescPublic(cls, new Boxed<>(byteBuddy.redefine(cls, compoundSupplier.get())))
                .get().make(typePoolSupplier.get()).getBytes().clone();
    }

    public Boxed<DynamicType.Builder<?>> makeTypeDescPublic(TypeDescription cls, Boxed<DynamicType.Builder<?>> typeBuilder) {
        var adjust = new Boxed<>(new ModifierAdjustment());
        adjust.run(builder -> builder.withTypeModifiers(getManifestation(cls)));
        cls.getDeclaredMethods().stream()
                .filter(method -> !method.isPublic() || method.isFinal())
                .forEach(method -> adjust.run(builder -> builder.withMethodModifiers(is(method), getManifestation(method))));
        cls.getDeclaredFields().stream()
                .filter(field -> !field.isPublic() || field.isFinal())
                .forEach(field -> adjust.run(builder -> builder.withFieldModifiers(is(field), getManifestation(field, cls))));
        return typeBuilder.run(builder -> builder.visit(adjust.get()).visit(TypeReferenceAdjustment.strict()));
    }
}
