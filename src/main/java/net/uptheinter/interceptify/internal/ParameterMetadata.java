package net.uptheinter.interceptify.internal;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.ParameterDescription.InDefinedShape;
import net.bytebuddy.description.type.TypeDescription.Generic;

class ParameterMetadata extends MetadataBase {
    private final InDefinedShape param;

    public ParameterMetadata(InDefinedShape param) {
        this.param = param;
    }

    public Generic getType() {
        return param.getType();
    }

    public String getTypeName() {
        return getType().getTypeName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ParameterMetadata
                ? this.equals((ParameterMetadata) other)
                : super.equals(other);
    }

    public boolean equals(ParameterMetadata other) {
        return other.getTypeName().equals(getTypeName());
    }

    @Override
    public int hashCode() {
        return getTypeName().hashCode();
    }

    @Override
    protected AnnotationList getInternal() {
        return param.getDeclaredAnnotations();
    }
}
