package net.uptheinter.interceptify.internal;

import net.uptheinter.interceptify.annotations.OverwriteConstructor;

import java.lang.reflect.Method;
import java.util.List;

class ParamsValidator {
    private static final String METHODTYPENAME = Method.class.getTypeName();
    private final MethodMetadata method;
    private final List<ParameterMetadata> params;
    private final ClassMetadata targetedCls;

    public ParamsValidator(MethodMetadata method, ClassMetadata targetedCls) {
        this.method = method;
        this.params = method.getParameterList();
        this.targetedCls = targetedCls;
    }

    private boolean isValidPreConstruct(List<?> otherParams) {
        return params.size() == otherParams.size() &&
                method.hasAnnotation(OverwriteConstructor.class) &&
                method.getAnnotation(OverwriteConstructor.class).before();
    }

    private boolean isValidPostConstruct(ClassMetadata targetedCls, List<?> otherParams) {
        return params.size() - 1 == otherParams.size() &&
                params.get(0).getTypeName().equals(targetedCls.getTypeName());
    }

    private int validateConstructorMethod(ClassMetadata targetedCls, List<?> otherParams) {
        if (isValidPreConstruct(otherParams))
            return 0;
        if (isValidPostConstruct(targetedCls, otherParams))
            return 1;
        return -1;
    }

    private int validateInstanceMethod(ClassMetadata targetedCls, List<?> otherParams) {
        return (params.size() - 2 != otherParams.size() ||
                !params.get(0).getTypeName().equals(targetedCls.getTypeName()) ||
                !params.get(1).getTypeName().equals(METHODTYPENAME)) ? -1 : 2;
    }

    private int validateWithSuper(List<?> otherParams) {
        return (params.size() - 1 != otherParams.size() ||
                !params.get(0).getTypeName().equals(METHODTYPENAME)) ? -1 : 1;
    }

    public boolean isCompatible(MethodMetadata targetedMethod) {
        if (!method.isStatic())
            return false;
        var argsStart = 0;
        var otherParams = targetedMethod.getParameterList();
        if (targetedMethod.isConstructor()) {
            argsStart = validateConstructorMethod(targetedCls, otherParams);
        } else if (targetedMethod.isInstanceMethod()) {
            argsStart = validateInstanceMethod(targetedCls, otherParams);
        } else if (params.size() - 1 == otherParams.size()) {
            argsStart = validateWithSuper(otherParams);
        } else if (params.size() != otherParams.size()) {
            argsStart = -1;
        }
        if (argsStart == -1)
            return false;
        for (var otherParam : otherParams)
            if (!otherParam.equals(params.get(argsStart++)))
                return false;
        return true;
    }
}
