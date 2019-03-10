package genunit;

import java.util.List;

public class MetodDeclarationMeta {
    private String fieldReference;
    private String methodName;
    private List<FieldDeclarationMeta> params;
    private String methodArgs;

    public MetodDeclarationMeta(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<FieldDeclarationMeta> getParams() {
        return params;
    }

    public void setParams(List<FieldDeclarationMeta> params) {
        this.params = params;
    }

    public String getMethodArgs() {
        return methodArgs;
    }

    public void setMethodArgs(String methodArgs) {
        this.methodArgs = methodArgs;
    }

    public String getFieldReference() {
        return fieldReference;
    }

    public void setFieldReference(String fieldReference) {
        this.fieldReference = fieldReference;
    }
}
