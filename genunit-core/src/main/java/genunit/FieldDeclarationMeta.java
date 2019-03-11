package genunit;

import java.util.ArrayList;
import java.util.List;

public class FieldDeclarationMeta {
    private String fieldType;
    private String fieldName;
    private String fullFieldType;
    private List<MethodDeclarationMeta> calls = new ArrayList<>();

    public FieldDeclarationMeta(String fieldType, String fieldName) {
        this.fieldType = fieldType;
        this.fieldName = fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setFullFieldType(String nameAsString) {
        this.fullFieldType = nameAsString;
    }

    public String getFullFieldType() {
        return fullFieldType;
    }

    public List<MethodDeclarationMeta> getCalls() {
        return calls;
    }

    public void setCalls(List<MethodDeclarationMeta> calls) {
        this.calls = calls;
    }
}
