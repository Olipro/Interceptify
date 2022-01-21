package net.uptheinter.interceptify.internal;

import net.bytebuddy.asm.MemberAttributeExtension.ForMethod;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import net.uptheinter.interceptify.util.Boxed;

import java.lang.reflect.Method;

import static net.bytebuddy.description.annotation.AnnotationDescription.Builder.ofType;
import static net.bytebuddy.matcher.ElementMatchers.is;

class ParamsAnnotator {
    private static final String METHODTYPENAME = Method.class.getTypeName();
    private final Boxed<DynamicType.Builder<?>> builderBox;
    private final MethodMetadata method;
    private final MethodMetadata targetMethod;
    private int paramIdx;
    private int argIdx;

    public ParamsAnnotator(Boxed<DynamicType.Builder<?>> builderBox,
                           MethodMetadata method,
                           MethodMetadata targetMethod) {
        this.builderBox = builderBox;
        this.method = method;
        this.targetMethod = targetMethod;
    }

    public void annotate() {
        method.getParameters()
                .sequential()
                .dropWhile(this::isForThisInstance)
                .dropWhile(this::isForOriginalMethodRef)
                .forEach(param -> annotateArguments());
    }

    private boolean isForThisInstance(ParameterMetadata param) {
        if (paramIdx != 0 || !param.getTypeName().equals(targetMethod.getDeclaringType().getTypeName()))
            return false;
        builderBox.run(builder -> builder.visit(new ForMethod()
                .annotateParameter(paramIdx++,
                        ofType(RuntimeType.class).build(),
                        ofType(This.class).build())
                .on(is(method.getShape()))));
        return true;
    }

    private boolean isForOriginalMethodRef(ParameterMetadata param) {
        if (paramIdx > 1 || !param.getTypeName().equals(METHODTYPENAME))
            return false;
        builderBox.run(builder -> builder.visit(new ForMethod()
                .annotateParameter(paramIdx++,
                        ofType(SuperMethod.class).build())
                .on(is(method.getShape()))));
        return true;
    }

    private void annotateArguments() {
        builderBox.run(builder -> builder.visit(new ForMethod()
                .annotateParameter(paramIdx++,
                        ofType(Argument.class)
                                .define("value", argIdx++).build())
                .on(is(method.getShape()))));
    }
}
