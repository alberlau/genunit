package genunit;

public class FieldDeclarationMeta {
    private String fieldType;
    private String fieldName;
    private String fullFieldType;

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
}
